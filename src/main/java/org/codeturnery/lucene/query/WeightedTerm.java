package org.codeturnery.lucene.query;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;

/**
 * Connects a term with a multiplier. The multiplier can be used to demote a
 * term (by passing a value between 0 and 1) or to boost a term (by passing a
 * value greater than 1).
 */
@SuppressWarnings({ "hiding", "javadoc" })
public class DemotedTerm {
	private final Term term;
	private final float multiplier;

	/**
	 * @param term
	 * @param multiplier Prefer the usage of
	 *                   {@link QueryFactory#createFilteredQuery(org.apache.lucene.search.Query, Iterable)
	 *                   filtering} over passing <code>0</code> as value. Avoid
	 *                   passing <code>1</code> keept the resulting {@link Query}
	 *                   small and possibly improve performance. The behavior
	 *                   for negative values is undefined. In case you want to
	 *                   filter float values, note that some integer values can not
	 *                   be representable as float exactly. Instead of comparing
	 *                   <code>floatValue != 0</code> you should use a deviation
	 *                   check like <code>floatValue &lt; 0.001</code>. The same is
	 *                   true for <code>1</code> as multiplier.
	 */
	public DemotedTerm(final Term term, final float multiplier) {
		this.term = term;
		this.multiplier = multiplier;
	}

	public float getMultiplier() {
		return this.multiplier;
	}

	public Term getTerm() {
		return this.term;
	}
}
