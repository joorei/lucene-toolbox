package org.codeturnery.lucene.access;

import org.apache.lucene.document.Document;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

/**
 * An instance of this class can be provided to methods to search the Lucene
 * index. Such method will store the result via the available setters.
 * Afterwards you can retrieve the results via the getter methods.
 */
@SuppressWarnings({ "javadoc", "null" })
public class PojoReadResponse implements ReadResponse {
	private TopDocs hits;
	private Facets facets;
	/**
	 * Initial value not set to <code>null</code> but an empty array, to have a
	 * valid return in case no explanations are added.
	 */
	private Explanation[] explanations = new Explanation[0];
	private int actualExplanationCount = 0;
	private int actualDocumentCount = 0;
	/**
	 * Initial value not set to <code>null</code> but an empty array, to have a
	 * valid return in case no documents are added.
	 */
	private Document[] documents = new Document[0];

	public Explanation[] getExplanations() {
		return this.explanations;
	}

	@Override
	public void setActualDocumentCount(final int actualDocumentCount) {
		this.actualDocumentCount = actualDocumentCount;
	}

	public Document[] getDocuments() {
		return this.documents;
	}

	public TopDocs getHits() {
		return this.hits;
	}

	public Facets getFacets() {
		return this.facets;
	}

	@Override
	public void setHits(final TopDocs hits) {
		this.hits = hits;
	}

	@Override
	public void setFacets(final Facets facets) {
		this.facets = facets;
	}

	@Override
	public void addExplanation(final ScoreDoc scoreDoc, final Explanation explanation, final int index) {
		if (isEmpty(this.explanations)) {
			this.explanations = new Explanation[this.actualExplanationCount];
		}
		this.explanations[index] = explanation;
	}

	@Override
	public void addDocument(final ScoreDoc scoreDoc, final Document document, final int index) {
		if (isEmpty(this.documents)) {
			this.documents = new Document[this.actualDocumentCount];
		}
		this.documents[index] = document;
	}

	@Override
	public void setActualExplanationCount(final int count) {
		this.actualExplanationCount = count;
	}

	private static <T> boolean isEmpty(final T[] array) {
		return array.length == 0;
	}
}
