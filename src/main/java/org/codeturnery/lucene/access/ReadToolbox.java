package org.codeturnery.lucene.access;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsCollectorManager;
import org.apache.lucene.facet.FacetsCollectorManager.FacetsResult;
import org.apache.lucene.facet.taxonomy.FacetLabel;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.facet.taxonomy.ParallelTaxonomyArrays.IntArray;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.tests.search.CheckHits;
import org.apache.lucene.util.BytesRef;
import org.codeturnery.lucene.query.QueryFactory;
import org.codeturnery.typesystem.Optionals;
import org.eclipse.jdt.annotation.Checks;
import org.eclipse.jdt.annotation.Nullable;

public class ReadToolbox {
	private final FacetsCollectorManager facetsCollectorManager;
	private final ReadExecuter readExecuter;

	public ReadToolbox(final ReadExecuter readExecuter, final FacetsCollectorManager facetsCollectorManager) {
		this.facetsCollectorManager = facetsCollectorManager;
		this.readExecuter = readExecuter;
	}

	public void loadDocuments(final ReadRequest request, final ReadResponse receiver) throws IOException {
		Checks.requireNonNull(request);
		final @Nullable ScoreDoc afterDocument = request.getAfterDocument().orElse(null);
		final boolean scoreInclusion = request.getScoreInclusion();
		final Query query = Checks.requireNonNull(request.getQuery());
		final int maxHitCount = request.getMaxHitCount();
		final Sort sort = request.getSort();

		this.readExecuter.read((searcher, taxonomyReader, config) -> {
			Checks.requireNonNull(searcher);

			final TopFieldDocs topDocs = Checks
					.requireNonNull(searcher.searchAfter(afterDocument, query, maxHitCount, sort, scoreInclusion));
			fillReceiverWithHits(searcher, topDocs, request, receiver);
			return receiver;
		});
	}

	public void loadDocumentsAndFacets(final ReadRequest request, final ReadResponse receiver) throws IOException {
		this.readExecuter.read((searcher, taxonomyReader, config) -> {
			final int maxHitCount = request.getMaxHitCount();
			final Query query = request.getQuery();
			final boolean scoreInclusion = request.getScoreInclusion();
			final Sort sort = request.getSort();
			final @Nullable ScoreDoc afterDocument = request.getAfterDocument().orElse(null);
			// We can't just use searcher.searchAfter, as this alone does not support
			// facets.
			// If sort is non-null, afterDocument must be null or a FieldDoc.
			// Also, if afterDocument is non-null, afterDocument.doc must be smaller than
			// searcher.getIndexReader().maxDoc().
			final FacetsResult result = FacetsCollectorManager.searchAfter(searcher, afterDocument, query, maxHitCount,
					sort, scoreInclusion, this.facetsCollectorManager);
			final TopDocs topDocs = Checks.requireNonNull(result.topDocs());
			final var facets = new FastTaxonomyFacetCounts(taxonomyReader, config, result.facetsCollector());

			receiver.setFacets(facets);
			fillReceiverWithHits(searcher, topDocs, request, receiver);

			return receiver;
		});
	}

	public Facets loadFacets(final Query query) throws IOException {
		return this.readExecuter.read((searcher, taxonomyReader, config) -> {
			final FacetsCollector facetsCollector = searcher.search(query, this.facetsCollectorManager);
			return new FastTaxonomyFacetCounts(taxonomyReader, config, facetsCollector);
		});
	}

	public int loadCount(final Query query) throws IOException {
		return this.readExecuter.readInt((searcher, taxonomyReader, config) -> searcher.count(query));
	}

	/**
	 * Collects the number documents, that do not have a specific field set, for an
	 * array of fields.
	 * <p>
	 * Will first execute the given query to collect the document IDs matching it.
	 * Afterwards for each given field another search will be performed to collect
	 * the document IDs that <strong>do</strong> have the current field set. The
	 * documents present in the first search result but <strong>not</strong> present
	 * in the second one will be counted and added to the to-be-returned result
	 * array.
	 * <p>
	 * 
	 * TODO: is this method needed? or does have
	 * {@link QueryFactory#createMissingQuery(Query, String, String...)} better
	 * performance or can this method be improved with the approach in that method?
	 * <p>
	 * TODO: expand on this principle to implement methods (performantly) loading
	 * actual documents and facets TODO: is there an advantage in making the
	 * {@link TermRangeQuery} adjustable (i.e. a parameter)?
	 * 
	 * @param query
	 * @param fields the fields that must not be present in the documents to count
	 * @return
	 * @throws IOException
	 */
	public int[] loadMissingCount(final Query query, final List<String> fields) throws IOException {
		final int fieldsSize = fields.size();
		final var results = new int[fieldsSize];
		if (fieldsSize == 0) {
			return results;
		}

		return this.readExecuter.read((searcher, taxonomyReader, config) -> {
			// first search limits result to given query
			final var queryResBase = new HashSet<Integer>();
			final var setCollector = new CheckHits.SetCollector(queryResBase);
			searcher.search(query, setCollector);

			// shortcut in case if there are no results anyway
			if (queryResBase.isEmpty()) {
				Arrays.fill(results, 0);
				return results;
			}

			// if there are results we calculate which of them are missing the given fields
			for (int i = 0; i < fieldsSize; i++) {
				// second search gets all documents missing the given field, independent from
				// the original query
				final Set<Integer> missingRes = new HashSet<>();
				final var setCollectorMissing = new CheckHits.SetCollector(missingRes);
				final var termRangeQuery = new TermRangeQuery(fields.get(i), null, null, false, false);
				searcher.search(termRangeQuery, setCollectorMissing);

				// compute the intersection of both results and get the resulting count
				final var queryRes = new HashSet<>(queryResBase);
				queryRes.removeAll(missingRes);
				results[i] = queryRes.size();
			}

			return results;
		});
	}

	public Set<String> getExistingValues(final String field) throws IOException {
		return this.readExecuter.read((searcher, taxonomyReader, config) -> {
			final Set<String> termStrings = new LinkedHashSet<>();
			for (final LeafReaderContext leafReaderContext : searcher.getIndexReader().leaves()) {
				final Terms terms = leafReaderContext.reader().terms(field);
				final TermsEnum termsEnum = terms.iterator();
				for (BytesRef term = termsEnum.term(); term != null; term = termsEnum.next()) {
					final String termString = Checks.requireNonNull(term.utf8ToString());
					termStrings.add(termString);
				}
			}
			return termStrings;
		});
	}

	/**
	 * @return
	 * @throws IOException
	 */
	public Collection<String> getIndexedFields() throws IOException {
		return this.readExecuter
				.read((searcher, taxonomyReader, config) -> FieldInfos.getIndexedFields(searcher.getIndexReader()));
	}

//	private Set<String> getFieldsA(final IndexReader indexReader) {
//	final FieldInfos fieldInfos = indexReader.leafReaderContext.reader().getFieldInfos();
//	final Set<String> fieldStrings = new LinkedHashSet<>(Math.toIntExact(fieldInfos.size()));
//	for (final FieldInfo fieldInfo : fieldInfos) {
//		final boolean added = fieldStrings.add(fieldInfo.name);
//		if (!added) {
//			throw new IllegalArgumentException("got same term multiple times: " + fieldInfo.name);
//		}
//	}
//	return fieldStrings;
//}

	/**
	 * Generates a {@link FacetResult} on a specific dimension.
	 * <p>
	 * It will contain the count ({@link FacetResult#value}) of results that
	 * <strong>would</strong> result from the given query, or <code>-1</code>.
	 * <p>
	 * It will contain the the count ({@link FacetResult#childCount}) of possible
	 * values for the given dimension.
	 * <p>
	 * It will contain the <code>topN</code> possible values
	 * ({@link FacetResult#labelValues}) for the given dimension with the number of
	 * documents that value set in the given dimension. (TODO: is that true? The
	 * documentation says it's the sum, but that would mean incorrect values in case
	 * of documents with multiple values for the given dimension.)
	 *
	 * @param query     The query to limit the set to generate the facet content on.
	 * @param dimension The dimension to load the facet for. Ie. the field name.
	 * @param topN      The number of {@link FacetResult#labelValues} to load.
	 * @return Empty {@link Optional} if the dimension was not found.
	 * @throws IOException
	 */
	public Optional<FacetResult> getFacetResult(final Query query, final String dimension, final int topN)
			throws IOException {
		final Facets facets = loadFacets(query);
		return Optionals.ofNullable(facets.getTopChildren(topN, dimension));
	}

	/**
	 * @return
	 * @throws IOException
	 */
	public int getDocumentCount() throws IOException {
		return this.readExecuter
				.read((searcher, taxonomyReader, config) -> Integer.valueOf(searcher.getIndexReader().numDocs()))
				.intValue();
	}

	/**
	 * @param query
	 * @param topN
	 * @return
	 * @throws IOException
	 */
	public List<FacetResult> getFacetResultList(final Query query, final int topN) throws IOException {
		return loadFacets(query).getAllDims(topN);
	}

	/**
	 * @return
	 * @throws IOException
	 */
	public int getFlattenedTaxonomyMetaCount() throws IOException {
		final Integer result = this.readExecuter
				.read((s, taxonomyReader, fc) -> Integer.valueOf(taxonomyReader.getSize()));
		return result.intValue();
	}

	/**
	 * @return
	 * @throws IOException
	 */
	public IntArray getTaxonomyChildren() throws IOException {
		return this.readExecuter.read((s, taxonomyReader, fc) -> {
			return taxonomyReader.getParallelTaxonomyArrays().children();
		});
	}

	/**
	 * @return
	 * @throws IOException
	 */
	public FacetLabel[] getFacetLabels() throws IOException {
		return this.readExecuter.read((s, taxonomyReader, fc) -> {
			final IntArray children = taxonomyReader.getParallelTaxonomyArrays().children();
			final int childrenCount = children.length();
			final var result = new FacetLabel[childrenCount];
			for (int i = 0; i < childrenCount; i++) {
				result[i] = taxonomyReader.getPath(i);
			}
			return result;
		});
	}

	public IntArray getTaxonomyParents() throws IOException {
		return this.readExecuter.read((s, taxonomyReader, fc) -> {
			return taxonomyReader.getParallelTaxonomyArrays().parents();
		});
	}

	public IntArray getTaxonomySiblings() throws IOException {
		return this.readExecuter.read((s, taxonomyReader, fc) -> {
			return taxonomyReader.getParallelTaxonomyArrays().siblings();
		});
	}

	private static void fillReceiverWithHits(final IndexSearcher searcher, final TopDocs topDocs,
			final ReadRequest request, final ReadResponse receiver) throws IOException {
		final int maxDocumentCount = request.getMaxDocumentCount();
		final int maxExplanationCount = request.getMaxExplanationCount();
		final Set<String> fieldsToLoad = request.getDocumentFieldsToLoad();
		final int maxHitCount = request.getMaxHitCount();
		final Query query = request.getQuery();

		final int processCount = min(Math.min(topDocs.totalHits.value, maxHitCount),
				Math.max(maxDocumentCount, maxExplanationCount));

		receiver.setHits(topDocs);
		receiver.setActualDocumentCount(Math.min(processCount, maxDocumentCount));
		receiver.setActualExplanationCount(Math.min(processCount, maxExplanationCount));

		final StoredFields storedFields = searcher.storedFields();
		for (int i = 0; i < processCount; i++) {
			final ScoreDoc scoreDoc = topDocs.scoreDocs[i];
			final int docId = scoreDoc.doc;

			if (i < maxExplanationCount) {
				final Explanation explanation = searcher.explain(query, docId);
				receiver.addExplanation(scoreDoc, explanation, i);
			}

			if (i < maxDocumentCount) {
				final Document document = storedFields.document(docId, fieldsToLoad);
				receiver.addDocument(scoreDoc, document, i);
			}
		}
	}

	private static int min(final long a, final int b) {
		return Math.toIntExact(Math.min(a, b));
	}
}
