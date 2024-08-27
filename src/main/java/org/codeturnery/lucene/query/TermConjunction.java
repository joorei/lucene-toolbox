package org.codeturnery.lucene.query;

import java.util.Collection;

import org.eclipse.jdt.annotation.Checks;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Instances hold an arbitrary number of {@link String} terms with the
 * information if they <em>all</em> or <em>any</em> are to be present as a value
 * of some field.
 */
// TODO: rework class (restructure, rename or change architecture where it is used): problematic is currently the null handling and the setField field, which needs a better name or could possibly removed in different class up or down in the hierarchy
public class TermConjunction {
	private final String[] terms;
	private final boolean orConjunction;
	private final @Nullable String setField;

	/**
	 * 
	 * @param terms         The terms to set.
	 * @param orConjunction <code>true</code> if a <code>OR</code> conjunction
	 *                      should be used, meaning a document matches the
	 *                      corresponding dimension if at least one term appears in
	 *                      the document field corresponding to the dimension.
	 *                      <code>false</code> if a <code>AND</code> conjunction
	 *                      should be used, meaning <strong>all</strong> terms must
	 *                      be present in the corresponding document field for the
	 *                      document to match.
	 */
	public TermConjunction(final boolean orConjunction, final String[] terms) {
		this.terms = requireNonNull(terms);
		this.orConjunction = orConjunction;
		this.setField = null;
	}
	
	public TermConjunction(final boolean orConjunction, final Collection<String> terms) {
		this.terms = requireNonNull(terms.toArray(new String[0]));
		this.orConjunction = orConjunction;
		this.setField = null;
	}
	
	public TermConjunction(final boolean orConjunction, final String setField, final @Nullable String ...terms) {
		if (terms.length == 0) {
			throw new IllegalArgumentException();
		}
		this.terms = terms;
		this.orConjunction = orConjunction;
		this.setField = Checks.requireNonEmpty(setField);
	}

	/**
	 * Shortcut to create an instance with only a single term, in which case the
	 * conjunction doesn't matter.
	 *
	 * @param term The term to set.
	 */
	public TermConjunction(final @Nullable String term) {
		this.terms = requireNonNull(new String[] { term });
		this.orConjunction = false;
		this.setField = null;
	}
	
	public TermConjunction(final String nullTrackingField, final @Nullable String term) {
		this.terms = new String[] { term };
		this.orConjunction = false;
		this.setField = nullTrackingField;
	}

	/**
	 * @return
	 */
	public String[] getTerms() {
		return this.terms;
	}

	/**
	 * @return
	 */
	public boolean isOrConjunction() {
		return this.orConjunction;
	}
	
	public String getNullTrackingField() {
		return Checks.requireNonNull(this.setField);
	}
	
	private static String[] requireNonNull(final String ...strings) {
		if (strings.length == 0) {
			throw new IllegalArgumentException();
		}
		if (Checks.isAnyNull(strings)) {
			throw new IllegalArgumentException("no term must be null");
		}
		return strings;
	}
}
