package org.codeturnery.lucene.navigation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.search.Query;
import org.codeturnery.lucene.query.QueryFactory;
import org.codeturnery.lucene.query.TermConjunction;
import org.eclipse.jdt.annotation.Checks;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Instances of this class can store a data structure used for a tree that aims
 * to ease the navigation to some kind of document (without being concerned with
 * the type of the document).
 * <p>
 * To make the approach more clear the following structure shows an example for
 * such tree. We use a facet for the dimension <code>fabric</code> with the
 * possible values <code>silk</code>, <code>wool</code> and <code>cotton</code>
 * and another dimension <code>colors</code> with the possible values
 * <code>blue</code>, <code>red</code> and <code>yellow</code>. Like said before
 * what is described by those facets is not relevant for the tree. It could be
 * hats, seat covers or anything else.
 * <p>
 * The facet dimension are hierarchically ordered in the tree. This means when
 * the instance is created it is defined what dimension is to be used on the
 * first level, what on the second level and so on. Attempts to change the
 * structure after tree creation will result in undefined and probably broken
 * behavior. Create a new instance with the new structure instead.
 * <p>
 * The first level in this example shows the <code>fabric</code> and the second
 * level the <code>colors</code>.
 * <p>
 * In our imaginary data set are two blue silk items, two red wool items, three
 * red silk items and a yellow cotton item.
 * <p>
 * This will result in the following tree when all facets are expanded. In this
 * example the facet value is followed by the count of items it contains (not to
 * be confused with the count of its sub facets). The facet dimensions are not
 * shown in this output but are available in each tree item too.
 * <ul>
 * <li>blue (2)
 * <ul>
 * <li>silk (2)</li>
 * </ul>
 * </li>
 * <li>red (5)
 * <ul>
 * <li>wool (2)</li>
 * <li>silk (3)</li>
 * </ul>
 * </li>
 * <li>yellow (1)
 * <ul>
 * <li>cotton (1)</li>
 * </ul>
 * </li>
 * </ul>
 */
@SuppressWarnings({ "hiding" })
public class LazyFacetTree {
	/**
	 * This {@link Supplier} will be consulted to narrow results further down. For
	 * example you may want to link it to a string search field in your UI.
	 */
	private final Supplier<Query> baseQuerySupplier;
	/**
	 * The {@link FacetsConfig} used to access the indexReader.
	 */
	private final FacetsConfig facetsConfig;
	/**
	 * The {@link QueryFactory} to be used to create the {@link Query} instances
	 * used to access the index.
	 */
	private final QueryFactory queryFactory;
	private final String usedFieldsField;

	/**
	 * @param baseQuerySupplier The value to set in {@link #baseQuerySupplier}.
	 * @param queryFactory      The value to set in {@link #queryFactory}.
	 * @param facetsConfig      The value to set in {@link #facetsConfig}.
	 */
	public LazyFacetTree(final Supplier<Query> baseQuerySupplier,
			final QueryFactory queryFactory, final FacetsConfig facetsConfig, final String usedFieldsField) {
		this.queryFactory = queryFactory;
		this.facetsConfig = facetsConfig;
		this.baseQuerySupplier = baseQuerySupplier;
		this.usedFieldsField = usedFieldsField;
	}

	/**
	 * Creates the initial {@link LazyFacetTreeItem root item}.
	 * 
	 * @param dimension The dimension the root item should be bound to.
	 * @return The created root item.
	 */
	public LazyFacetTreeItem createRoot(final String dimension) {
		return new LazyFacetTreeItem(Checks.requireNonNull(Collections.emptyMap()), dimension);
	}

	/**
	 * Part of a {@link LazyFacetTree} providing facet values.
	 * <p>
	 * The item will fetch on instantiation its facets. This means it will not not
	 * only know the number of its direct sub items but also the count of direct sub
	 * items in each of those.
	 */
	public class LazyFacetTreeItem {
		/**
		 * All selections made in the parents of this item. Each entry represent a
		 * parent with the key being its dimension and the value being the selected
		 * value in that dimension.
		 * <p>
		 * TODO: handle multiple parents having the same dimension. Either with
		 * exception or by using something else than a Map.
		 */
		private final Map<LazyFacetTreeItem, @Nullable String> parentSelections;
		/**
		 * The dimension ("field") this instance is bound to.
		 */
		private final String dimension;

		/**
		 * Creates a node within the tree.
		 * <p>
		 * The node can either be the root node or a sub node. To create a root node the
		 * <code>parentSelections</code> must be empty. If <code>parentSelections</code>
		 * contains entries, each one is considered a selection within a different
		 * parent.
		 * <p>
		 * As each node is connected to a dimension, the key of an entry will be the
		 * dimension to be searched in. The corresponding value will be the value that
		 * was selected in that dimension.
		 * <p>
		 * For a more visual example imagine a flat list of possible values, all
		 * generated from the same pre-selected dimension. Eg. for the dimension
		 * <code>color</code> we would see <code>blue</code>, <code>red</code> and
		 * <code>yellow</code>. This list is already backed by a
		 * root-{@link LazyFacetTreeItem} without any <code>parentSelections</code>. Now
		 * if <code>blue</code> within this list is selected a new sublist will be
		 * created, again backed by a {@link LazyFacetTreeItem} that received the
		 * selection <code>color</code> as key and <code>blue</code> as value as single
		 * entry in the <code>parentSelections</code> parameter.
		 * <p>
		 * The dimension represented by this new sub-{@link LazyFacetTreeItem} is
		 * determined by the <code>dimensionHierarchy</code> parameter given on
		 * instantiation of the {@link LazyFacetTree}. The first element in that list
		 * will be used for the root-{@link LazyFacetTreeItem}, the second one for the
		 * child of the root, the third one for that child and so on.
		 * <p>
		 * For the given example the first element was <code>color</code>. If the second
		 * element would happen to be <code>fabric</code> then the
		 * sub-{@link LazyFacetTreeItem} will use that to show the values for that
		 * dimension in the sub-list, eg. <code>wool</code>, <code>silk</code> and
		 * <code>cotton</code>.
		 * 
		 * @param parentSelections The value to set in {@link #parentSelections}.
		 * @param dimension        The value to set in {@link #dimension}.
		 */
		protected LazyFacetTreeItem(final Map<LazyFacetTreeItem, @Nullable String> parentSelections,
				final String dimension) {
			this.parentSelections = parentSelections;
			this.dimension = dimension;
		}

		/**
		 * @return The value of {@link #dimension}.
		 */
		public String getDimension() {
			return this.dimension;
		}

		/**
		 * Get the query that will match all documents that matches all the following:
		 * <ul>
		 * <li>The current {@link Query}: This is the query currently returned by the
		 * query supplier passed into the {@link LazyFacetTree} of this item on
		 * instantiation of the tree.</li>
		 * <li>All selected parents: If this item is the child of another
		 * {@link LazyFacetTreeItem}, then within its parent a specific value was
		 * selected to create this child. The parent is connected to a dimension and the
		 * selected value will be used to limit the query to values within that
		 * dimension. This is true not only for the direct parent of this item but for
		 * all parents of its parent. If this item is the root then no limitation is
		 * done.</li>
		 * <li>The given <code>selections</code>: As with the parent item explained
		 * above, this item too can have its values selected. If done so the values can
		 * be given as <code>selections</code> to limit the query to the document that
		 * contain the value (selection) for the dimension this
		 * {@link LazyFacetTreeItem} corresponds to.
		 * </ul>
		 * 
		 * @param orConjunction If the given selections should be conjuncted as
		 *                      <code>OR</code> (any selection must match) or as
		 *                      <code>AND</code> (all selections must match).
		 * @param selections    The selected values in the dimension of this item.
		 * @return The created query. Can be used to limit the result according to this
		 *         instance.
		 */
		public Query getQuery(final boolean orConjunction, final @Nullable String... selections) {
			final var dimensions = getParentDimensions();
			if (selections.length != 0) {
				dimensions.put(this.dimension, new TermConjunction(orConjunction, selections));
			}
			return createDrillDownQuery(dimensions);
		}

		/**
		 * @return Like {@link #getQuery(boolean, String...)}, but without any
		 *         selections.
		 */
		public Query getQuery() {
			return createDrillDownQuery(getParentDimensions());
		}

		/**
		 * Provides access to sub facets of this instance.
		 * <p>
		 * Each possible return corresponds to an element in the
		 * {@link FacetResult#labelValues labelValues} of the {@link NavigationFetcher#getFacet(LazyFacetTreeItem, int)
		 * facet} of this instance. The instance returned for the given key provides
		 * access to the {@link FacetResult} loaded for that key and the keys of all
		 * parents. Including all parent keys in an <code>AND</code> conjunction when
		 * loading the child ensures that the resulting tree has a drill-down nature.
		 * 
		 * @param selection     Normally one of {@link LabelAndValue#label labels} in
		 *                      the {@link NavigationFetcher#getFacet(LazyFacetTreeItem, int) facet} of this instance. But
		 *                      as the labels may be incomplete other values are allowed
		 *                      too.
		 * @param nextDimension The dimension of the item created.
		 * @return A newly created {@link LazyFacetTreeItem} configured as child of this
		 *         instance.
		 */
		public LazyFacetTreeItem createChild(final String selection, final String nextDimension) {
			final var newSelections = new HashMap<LazyFacetTreeItem, @Nullable String>(getDepth() + 1);
			newSelections.putAll(this.parentSelections);
			final @Nullable String previous = newSelections.put(this, selection);
			if (previous != null) {
				throw new IllegalStateException("Loop encountered. Current item is referenced in parent selections.");
			}
			return new LazyFacetTreeItem(newSelections, nextDimension);
		}

		public LazyFacetTreeItem createMissingChild(final String nextDimension) {
			return createChild(null, nextDimension);
		}

		/**
		 * @return The level of this item in the tree. The root has a depth of
		 *         <code>0</code>. Its children a depth of <code>1</code>. The children
		 *         of the children of the root item a depth of <code>2</code> and so on.
		 */
		public int getDepth() {
			return this.parentSelections.size();
		}

		/**
		 * @return The selection in each parent converted into a
		 *         {@link TermConjunction}.
		 */
		private Map<String, TermConjunction> getParentDimensions() {
			// making the map one element bigger in case a TermConjunction for current
			// selections is added
			final var dimensions = new HashMap<String, TermConjunction>(getDepth() + 1);
			for (final Entry<LazyFacetTreeItem, @Nullable String> parentSelection : this.parentSelections.entrySet()) {
				final String dimension = Checks.requireNonNull(parentSelection.getKey()).getDimension();
				final @Nullable String selection = parentSelection.getValue();
				final var termConjunction = new TermConjunction(LazyFacetTree.this.usedFieldsField, selection);
				dimensions.put(dimension, termConjunction);
			}
			return dimensions;
		}

		/**
		 * @param dimensions The dimensions and corresponding {@link TermConjunction} to
		 *                   limit the returned {@link Query} to.
		 * @return A query matching the {@link Query} retrieved via
		 *         {@link LazyFacetTree#baseQuerySupplier} and the given dimension
		 *         selections.
		 */
		private Query createDrillDownQuery(final Map<String, TermConjunction> dimensions) {
			final Query baseQuery = Checks.requireNonNull(LazyFacetTree.this.baseQuerySupplier.get());
			final FacetsConfig facetsConfig = LazyFacetTree.this.facetsConfig;

			return LazyFacetTree.this.queryFactory.createDrillDownQuery(baseQuery, dimensions, facetsConfig);
		}
	}
}
