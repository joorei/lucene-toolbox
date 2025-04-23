package org.codeturnery.lucene.access;

import org.apache.lucene.document.Document;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

@SuppressWarnings({ "javadoc" })
public interface ReadResponse {
	public void setHits(TopDocs topDocs);

	public void setFacets(Facets facets);

	/**
	 * Must be called after {@link #setActualExplanationCount}.
	 * @param scoreDoc
	 * @param explanation
	 * @param index
	 */
	public void addExplanation(ScoreDoc scoreDoc, Explanation explanation, int index);

	/**
	 * Must be called after {@link #setActualDocumentCount}.
	 * @param scoreDoc
	 * @param document
	 * @param index
	 */
	public void addDocument(ScoreDoc scoreDoc, Document document, int index);
	
	public void setActualExplanationCount(int count);
	
	public void setActualDocumentCount(int count);
}
