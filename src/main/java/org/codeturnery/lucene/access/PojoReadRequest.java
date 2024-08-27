package org.codeturnery.lucene.access;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.eclipse.jdt.annotation.Checks;
import org.eclipse.jdt.annotation.Nullable;

/**
 * <strong>experimental</strong>: TODO: this is just a quick-and-dirty implementation of a {@link ReadRequest} to get started.
 */
@SuppressWarnings({ "javadoc", "null" })
public class PojoReadRequest implements ReadRequest {
	private @Nullable Query query;
	private int maxDocumentCount;
	private Set<String> documentFieldsToLoad = Collections.emptySet();
	private @Nullable ScoreDoc startDocument;
	private boolean scoreInclusion;
	private int maxHitCount;
	private int maxExplanationCount;

	@Override
	public Query getQuery() {
		return Checks.requireNonNull(this.query);
	}

	@Override
	public int getMaxDocumentCount() {
		return this.maxDocumentCount;
	}

	@Override
	public Set<String> getDocumentFieldsToLoad() {
		return this.documentFieldsToLoad;
	}

	@Override
	public Optional<ScoreDoc> getAfterDocument() {
		return Optional.ofNullable(this.startDocument);
	}

	@Override
	public int getMaxHitCount() {
		return this.maxHitCount;
	}

	@Override
	public int getMaxExplanationCount() {
		return this.maxExplanationCount;
	}

	public void setQuery(Query query) {
		this.query = query;
	}

	public void setMaxDocumentCount(int maxDocumentCount) {
		this.maxDocumentCount = maxDocumentCount;
	}

	public void setDocumentFieldsToLoad(Set<String> documentFieldsToLoad) {
		this.documentFieldsToLoad = documentFieldsToLoad;
	}

	public void setStartDocument(final ScoreDoc startDocument) {
		this.startDocument = startDocument;
	}

	public void setScoreInclusion(boolean scoreInclusion) {
		this.scoreInclusion = scoreInclusion;
	}

	public void setMaxHitCount(int maxHitCount) {
		this.maxHitCount = maxHitCount;
	}

	public void setMaxExplanationCount(int maxExplanationCount) {
		this.maxExplanationCount = maxExplanationCount;
	}

	@Override
	public boolean getScoreInclusion() {
		return this.scoreInclusion;
	}

}
