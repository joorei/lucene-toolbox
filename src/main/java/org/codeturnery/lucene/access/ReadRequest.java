package org.codeturnery.lucene.access;

import java.util.Optional;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;

@SuppressWarnings({ "javadoc" })
public interface ReadRequest {
	/**
	 * The query to execute and get the {@link Document} instances or {@link Facets}
	 * for.
	 * 
	 * @return
	 */
	public Query getQuery();

	/**
	 * The maximum number of {@link Document} instances provided via
	 * {@link ReadResponse#addDocument(ScoreDoc, Document, int)}.
	 * 
	 * @return
	 */
	public int getMaxDocumentCount();

	/**
	 * Each {@link Document} passed into
	 * {@link ReadResponse#addDocument(ScoreDoc, Document, int)} will be initialized
	 * with the fields defined to be loaded. Fields not present in the returned
	 * {@link Set} may not be accessible in the resulting documents.
	 * 
	 * @return
	 */
	public Set<String> getDocumentFieldsToLoad();

	/**
	 * Return the {@link ScoreDoc} that marks the beginning of the result to return.
	 * <p>
	 * Useful to implement a pagination.
	 * <p>
	 * The returned {@link ScoreDoc} will not be included in the request result.
	 * 
	 * @return The {@link ScoreDoc} after which the request result should start or
	 *         an empty {@link Optional} if the request result should start at the
	 *         beginning.
	 */
	public Optional<ScoreDoc> getAfterDocument();

	/**
	 * @return True if the score should not only be calculated and used for result
	 *         ordering but also be numerically set in the request result.
	 */
	public boolean getScoreInclusion();

	/**
	 * The maximum number of {@link Explanation} instances provided via
	 * {@link ReadResponse#addExplanation(ScoreDoc, Explanation, int)}.
	 * 
	 * @return
	 */
	public int getMaxExplanationCount();

	/**
	 * The maximum number of results to request from the index.
	 * 
	 * @return
	 */
	public int getMaxHitCount();
}
