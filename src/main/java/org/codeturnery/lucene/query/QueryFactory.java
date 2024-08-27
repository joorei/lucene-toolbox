package org.codeturnery.lucene.query;

import java.util.Map;
import java.util.Map.Entry;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.facet.DrillDownQuery;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.FacetsConfig.DimConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.function.FunctionScoreQuery;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.eclipse.jdt.annotation.Checks;
import org.apache.lucene.search.MatchAllDocsQuery;

// TODO: check if the queries gets simplified by using query.rewrite(reader)
@SuppressWarnings({ "javadoc", "null", "static-method" })
public class QueryFactory {
	/**
	 * Does demote (or boost) the given query based on the multiplier given for each
	 * term.
	 * <p>
	 * <strong>Warning:</strong> the built query may be heavy on performance, the
	 * more {@link DemotedTerm}s are used.
	 * <p>
	 * The given query will be boosted solely by the (multiplied) values given with
	 * the terms and <strong>not</strong> based on other factors like the term
	 * frequency of a given term. If a boosting already exists in the given
	 * input-query or is added later (eg. based on the term frequency) will be
	 * applied additionally to the boosts/demotes of this method.
	 * 
	 * @param inputQuery
	 * @param terms
	 * @return
	 */
	// TODO: what about FunctionScoreQuery.boostByValue(userQuery,
	// DoubleValuesSource.fromQuery(demoteQueryMultiplier));
	public Query createDemotedQuery(final Query inputQuery, Iterable<DemotedTerm> terms) {
		Query demoteQueryMultiplier = inputQuery;
		for (final DemotedTerm term : terms) {
			final var demoteQuery = new TermQuery(term.getTerm());
			/*
			 * the same as wrapping the TermQuery first in a ConstantScoreQuery and then in
			 * a BoostQuery before using
			 * FunctionScoreQuery.boostByValue(demoteQueryMultiplier,
			 * DoubleValuesSource.fromQuery(demoteQuery)) on the result
			 */
			demoteQueryMultiplier = FunctionScoreQuery.boostByQuery(demoteQueryMultiplier, demoteQuery,
					term.getMultiplier());
		}

		return demoteQueryMultiplier;
	}

	/**
	 * Define terms that must not be returned by the created query.
	 *
	 * @param inputQuery
	 * @param mustNotEntries The terms to add as {@link Occur#MUST_NOT} to the given
	 *                       {@link Query}.
	 * @return
	 */
	public Query createFilteredQuery(final Query inputQuery, final Iterable<Term> mustNotEntries) {
		final Builder b = new BooleanQuery.Builder();
		b.add(inputQuery, Occur.MUST);
		for (final Term term : mustNotEntries) {
			b.add(new TermQuery(term), Occur.MUST_NOT);
		}
		return b.build();
	}

	/**
	 * Extends the given query to limit the search to results that match all the
	 * given dimension terms.
	 * <p>
	 * The dimension terms are given in a {@link Map} structure with the keys being
	 * the dimension (i.e. the name) of the facet and the values being a
	 * {@link TermConjunction} containing terms to be present in the document field
	 * corresponding to the dimension.
	 * <p>
	 * If {@link TermConjunction#isOrConjunction()} is set to <code>false</code>
	 * <strong>all</strong> terms given for that dimension must be present in the
	 * corresponding field of the document for the document to be returned by the
	 * query. If set to <code>true</code> <strong>at least one</strong> term in that
	 * dimension must be present in the corresponding field of the document.
	 * <p>
	 * This must not be confused with the conjunction of the dimensions which is
	 * always <code>AND</code>, meaning for each of the dimensions given a match
	 * must exist for a document to be returned.
	 *
	 * @param baseQuery    The query to apply the facet search to.
	 * @param facetTerms   The dimensions and terms to use to limit the query
	 *                     result. If empty the given base query will be returned.
	 * @param facetsConfig
	 * @return The base query with the defined facet limitations applied.
	 */
	public Query createDrillDownQuery(final Query baseQuery, final Map<String, TermConjunction> facetTerms,
			final FacetsConfig facetsConfig) {
		if (facetTerms.isEmpty()) {
			return baseQuery;
		}

		final DrillDownQuery query = new DrillDownQuery(facetsConfig, baseQuery);
		for (final Entry<String, TermConjunction> entry : facetTerms.entrySet()) {
			final String facetDimensionName = entry.getKey();
			final TermConjunction termConjunction = entry.getValue();
			final String[] values = termConjunction.getTerms();

			if (shouldUseOrConjunction(values.length, termConjunction.isOrConjunction())) {
				// OR behavior in same fields, final result example:
				// +*:* #($facets:LANGUAGE/Japanese $facets:LANGUAGE/English)
				for (final String value : values) {
					if (value == null) {
						final String nullTrackingField = termConjunction.getNullTrackingField();
						final var missingQuery = createMissingQueryBuilder(nullTrackingField, facetDimensionName).build();
						// TODO: untested, using it like this may not work
						query.add(facetDimensionName, missingQuery);
					} else {
						query.add(facetDimensionName, value);
					}
				}
			} else {
				// AND behavior in same fields, final result example:
				// +*:* #((#$facets:LANGUAGE/English #$facets:LANGUAGE/Japanese))
				final BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
				boolean filterAdded = false;
				for (int i = 0; i < values.length; i++) {
					final String value = values[i];
					if (value == null) {
						final String nullTrackingField = termConjunction.getNullTrackingField();
						final var hasFieldQuery = createHasFieldQuery(nullTrackingField, facetDimensionName);
						booleanQueryBuilder.add(hasFieldQuery, Occur.MUST_NOT);
					} else {
						final DimConfig dimConfig = facetsConfig.getDimConfig(facetDimensionName);
						final Term term = DrillDownQuery.term(dimConfig.indexFieldName, facetDimensionName, value);
						booleanQueryBuilder.add(new TermQuery(term), Occur.FILTER);
						filterAdded = true;
					}
				}
				if (!filterAdded) {
					// Probably needed because boolean queries won't work when created
					// as open ended only; a limit needs to be set. This may affect
					// performance.
					booleanQueryBuilder.add(createMatchAllQuery(), Occur.FILTER);
				}
				/*
				 * The implementation of DrillDownQuery uses Occur.SHOULD for the values of the
				 * same field and Occur.FILTER when concatenating all fields. By passing a
				 * single query per field whose sub queries were already concatenated with
				 * Occur.FILTER the result will be an AND query.
				 */
				query.add(facetDimensionName, booleanQueryBuilder.build());
			}
		}

		return query;
	}

	public Query createAndConjunction(final Query query, final Query... queries) {
		final int queriesCount = queries.length;
		if (queriesCount == 0) {
			return query;
		}

		final var builder = new BooleanQuery.Builder();
		builder.add(query, Occur.MUST);
		for (int i = 0; i < queriesCount; i++) {
			builder.add(queries[i], Occur.MUST);
		}

		return builder.build();
	}

	/**
	 * The returned query will match documents that match the given base query and in which all the given terms are present.
	 */
	public Query createMandatoryQuery(final Query baseQuery, final Iterable<Term> mandatoryTerms) {
		final BooleanQuery.Builder b = new BooleanQuery.Builder();
		b.add(baseQuery, Occur.MUST);
		for (final Term term : mandatoryTerms) {
			b.add(new TermQuery(term), Occur.MUST);
		}
		return b.build();
	}

	/**
	 * The returned query will match documents that match the given base query and all queries generated from the given {@link TermConjunction}s.
	 * For each {@link TermConjunction} a query will generated that will match a document if either all terms are present
	 * (AND) or if at least one term is present (OR), depending on the settings in the {@link TermConjunction}.
	 */
	public Query createMandatoryQuery(final Query baseQuery, final Map<String, TermConjunction> mandatoryTerms) {
		final var b = new BooleanQuery.Builder();
		b.add(baseQuery, Occur.MUST);
		for (final Entry<String, TermConjunction> entry : mandatoryTerms.entrySet()) {
			final var innerBuilder = new BooleanQuery.Builder();
			innerBuilder.setMinimumNumberShouldMatch(1);
			final String field = entry.getKey();
			final Occur occur = entry.getValue().isOrConjunction() ? Occur.SHOULD : Occur.MUST;
			for (final String term : entry.getValue().getTerms()) {
				innerBuilder.add(new TermQuery(new Term(field, term)), occur);
			}
			b.add(innerBuilder.build(), Occur.MUST);
		}
		return b.build();
	}

	/**
	 * Creates the means to parse {@link String}s into {@link Query} objects using
	 * {@link QueryParser#parse(String)}.
	 * <p>
	 * The returned parser is not very flexible, you may consider using
	 * {@link #createStandardQueryParser(String[], Analyzer)} if you need more
	 * configuration options.
	 *
	 * @param defaultFields The fields to search in, in case a query {@link String}
	 *                      contains terms no field was specified for.
	 * @return
	 */
	public QueryParser createQueryParser(final String[] defaultFields, final Analyzer analyzer) {
		if (defaultFields.length == 1) {
			/*
			 * MultiFieldQueryParser uses internally a different QueryParser for each field.
			 * Using a QueryParser instance directly is simply a shortcut.
			 */
			return new QueryParser(defaultFields[0], analyzer);
		}
		return new MultiFieldQueryParser(defaultFields, analyzer);
	}

	/**
	 * <strong>TODO: WIP: DO NOT USE YET</strong> except for testing purposes.
	 * <p>
	 * Creates the means to parse {@link String}s into {@link Query} objects using
	 * {@link StandardQueryParser#parse(String, String)}.
	 * <p>
	 * TODO: test how the second parameter in
	 * {@link StandardQueryParser#parse(String, String)} interacts with
	 * {@link StandardQueryParser#setMultiFields(CharSequence[])} and document it
	 * here. Searching in the Lucene code gave no indications.
	 *
	 * @param fields
	 * @param analyzer
	 * @return
	 */
	public StandardQueryParser createStandardQueryParser(final String[] fields, final Analyzer analyzer) {
		final var queryParser = new StandardQueryParser(analyzer);
		/**
		 * TODO: it is not yet entirely clear how this method works. Initially it was
		 * assumed it is the same as using a {@link MultiFieldQueryParser}. However it
		 * may also be possible that this parameter is used in case a <em>document</em>
		 * having <code>null</code> set as value for the field that was to be used by a
		 * term in a query. This would explain why there is the mandatory second
		 * parameter when using {@link StandardQueryParser#parse(String, String)}, which
		 * seems to have the same purpose as {@link MultiFieldQueryParser}, just being
		 * less flexible.
		 */
		queryParser.setMultiFields(fields);
		return queryParser;
	}

	/**
	 * Create a query that matches all documents in the index.
	 * <p>
	 * You may want to use this method in case of an empty {@link String} given as
	 * query to parse.
	 * 
	 * @return The created query instance.
	 */
	public MatchAllDocsQuery createMatchAllQuery() {
		return new MatchAllDocsQuery();
	}

	/**
	 * See the comments in https://issues.apache.org/jira/browse/LUCENE-7899 why
	 * <code>new ConstantScoreQuery(new DocValuesFieldExistsQuery(dimension));</code>
	 * does not work.
	 */
	// TODO: check if this works as intended
	public Query createMissingQuery(final Query baseQuery, final String field, final String... dimensions) {
		final Builder b = createMissingQueryBuilder(field, dimensions);
		b.add(baseQuery, Occur.MUST);
		final var resultQuery = b.build();
		
		return Checks.requireNonNull(resultQuery);
	}

	/**
	 * See the comments in https://issues.apache.org/jira/browse/LUCENE-7899 why
	 * <code>new ConstantScoreQuery(new DocValuesFieldExistsQuery(dimension));</code>
	 * does not work.
	 */
	private BooleanQuery.Builder createMissingQueryBuilder(final String field, final String... dimensions) {
		final Builder b = new BooleanQuery.Builder();
		Checks.requireNonEmpty(field);
		for (final String dimension : dimensions) {
			final var dimensionExistQuery = createHasFieldQuery(field, Checks.requireNonEmpty(dimension));
			b.add(dimensionExistQuery, Occur.MUST_NOT);
		}
		return b;
	}
	
	private Query createHasFieldQuery(final String field, final String dimension) {
		return new TermQuery(new Term(field, dimension));
	}

	/**
	 * In case of a single term the conjunction doesn't matter and we use OR
	 * resulting in a more simple query building than AND.
	 * 
	 * @param valueCount
	 * @param termConjunctionIsOrConjunction
	 * @return
	 */
	private static boolean shouldUseOrConjunction(final int valueCount, final boolean termConjunctionIsOrConjunction) {
		return valueCount == 1 ? false : termConjunctionIsOrConjunction;
	}
}
