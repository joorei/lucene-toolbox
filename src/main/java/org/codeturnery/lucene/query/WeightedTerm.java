package org.codeturnery.lucene.query;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;

/**
 * Connects a term with a weight to boost or demote the terms relevance when
 * fetching documents.
 * <p>
 * Instead of passing {@code 0} as multiplier, prefer the usage of
 * {@link QueryFactory#createFilteredQuery(Query, Iterable) filtering} for a
 * more direct approach.
 * <p>
 * Avoid passing <code>1</code> to keep the resulting {@link Query} small and
 * possibly improve performance, as the value will neither boost nor demote the
 * term.
 * <p>
 * When you attempt to check for {@code 0} and {@code 1}, note that some integer
 * values can not be representable as float exactly. Instead of comparing
 * <code>multiplier != 0</code> you should use a deviation check like
 * <code>multiplier &lt; 0.001</code>.
 * 
 * @param term       The term to which the weight shall be applied to, i.e. the
 *                   term to boost or demote.
 * @param multiplier The weight of the corresponding term. Demote (between 0 and
 *                   1) or boost (greater than 1) the given {@link Term}. The
 *                   behavior for negative values is undefined.
 * 
 */
public record WeightedTerm(Term term, float multiplier) {
}
