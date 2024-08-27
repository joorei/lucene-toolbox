package org.codeturnery.lucene.experimental;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * This function will iterate through the given elements and places each
 * elements into the buckets it belongs to. An element can be placed into
 * multiple buckets. A decider must be given which will be invoked with the
 * element and returns a list of bucket keys the element belongs into.
 * <p>
 * The order of the resulting buckets as well as the order inside each bucket is
 * determined by the order the input elements are given in.
 * 
 * @param <K> The type of the key assigned for each bucket.
 * @param <V> The type of the elements inside each bucket.
 */
// TODO: rename to TreeGrouper
public class ElementGrouper<K, V> implements Function<Iterable<V>, TreeNode<K, V>> {
	/**
	 * Determines in which buckets an element will be placed. Must not be
	 * <code>null</code>.
	 */
	private final Collection<Function<V, Collection<K>>> groupDeciders;

	/**
	 * Creates an instance of this class with the given <code>groupDecider</code>.
	 * 
	 * @param theGroupDeciders The {@link Function}s determining in which buckets an
	 *                         element will be placed. Must not be
	 *                         <code>null</code>.
	 */
	public ElementGrouper(final Collection<Function<V, Collection<K>>> theGroupDeciders) {
		this.groupDeciders = theGroupDeciders;
	}

	/**
	 * Applies this function.
	 *
	 * @param elements The elements to group into buckets. Must not be
	 *                 <code>null</code>.
	 * @return A mapping between bucket keys and buckets, with each bucket
	 *         containing the corresponding elements. Will not be <code>null</code>.
	 */
	@Override
	public TreeNode<K, V> apply(final Iterable<V> elements) {
		final TreeNode<K, V> root = new TreeNode<>(null);
		for (final V element : elements) {
			final List<Iterable<K>> leveledKeys = applyDeciders(element);
			root.addChild(element, leveledKeys.iterator());
		}

		return root;
	}

	/**
	 * 
	 * @param element
	 * @return A list containing the result of each applied decider as elements.
	 */
	protected List<Iterable<K>> applyDeciders(final V element) {
		final List<Iterable<K>> deciderResults = new ArrayList<>(this.groupDeciders.size());
		for (final Function<V, Collection<K>> decider : this.groupDeciders) {
			final Collection<K> deciderResult = decider.apply(element);
			/*
			 * Keeping an empty result in the returned list would result in the element not
			 * being added to any node. As filtering is expected to have been already
			 * applied we interpret an empty result as "add it to the current node" instead.
			 */
			if (deciderResult.isEmpty()) {
				return deciderResults;
			}
			deciderResults.add(deciderResult);
		}
		return deciderResults;
	}
}
