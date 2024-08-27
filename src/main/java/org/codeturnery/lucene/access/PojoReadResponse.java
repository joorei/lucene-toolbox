package org.codeturnery.lucene.access;

import org.apache.lucene.document.Document;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

/**
 * <strong>experimental</strong>: TODO: this is just a quick-and-dirty implementation of a {@link ReadResponse} to get started.
 */
@SuppressWarnings({ "javadoc", "null" })
public class PojoReadResponse implements ReadResponse {
	private int actualHitCount = -1;
	private TopDocs hits;
	private Facets facets;
	private Explanation[] explanations;
	private int actualExplanationCount;
	private int actualDocumentCount;
	private Document[] documents = new Document[0];

	public Explanation[] getExplanations() {
		return this.explanations;
	}

	public int getActualDocumentCount() {
		return this.actualDocumentCount;
	}

	@Override
	public void setActualDocumentCount(int actualDocumentCount) {
		this.actualDocumentCount = actualDocumentCount;
	}

	public Document[] getDocuments() {
		return this.documents;
	}

	public int getActualHitCount() {
		return this.actualHitCount;
	}

	public TopDocs getHits() {
		return this.hits;
	}

	public Facets getFacets() {
		return this.facets;
	}

	@Override
	public void setHits(TopDocs hits) {
		this.hits = hits;
	}

	@Override
	public void setFacets(Facets facets) {
		this.facets = facets;
	}

	@Override
	public void addExplanation(final ScoreDoc scoreDoc, final Explanation explanation, final int index) {
		if (this.explanations == null) {
			this.explanations = new Explanation[this.actualExplanationCount];
		}
		this.explanations[index] = explanation;
	}

	@Override
	public void addDocument(ScoreDoc scoreDoc, Document document, final int index) {
		if (this.documents.length == 0) {
			this.documents = new Document[this.actualDocumentCount];
		}
		this.documents[index] = document;
	}

	@Override
	public void setActualExplanationCount(int count) {
		this.actualExplanationCount = count;
	}
}
