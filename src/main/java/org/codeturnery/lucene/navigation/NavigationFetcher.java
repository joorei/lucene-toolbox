package org.codeturnery.lucene.navigation;

import java.io.IOException;
import java.util.Optional;

import org.apache.lucene.facet.FacetResult;

import org.codeturnery.annotations.NonNegative;
import org.codeturnery.lucene.access.ReadToolbox;
import org.codeturnery.lucene.navigation.LazyFacetTree.LazyFacetTreeItem;
import org.codeturnery.lucene.query.QueryFactory;

public class NavigationFetcher {

	private final ReadToolbox readToolbox;
	private final String usedFieldsField;
	private final QueryFactory queryFactory;

	public NavigationFetcher(final ReadToolbox readToolbox, final String usedFieldsField) {
		this.readToolbox = readToolbox;
		this.usedFieldsField = usedFieldsField;
		this.queryFactory = new QueryFactory();
	}

	/**
	 * @param item
	 * @return The number of items that match the parent selections but do not have
	 *         a value set in the dimension of this item.
	 * @throws IOException
	 */
	public @NonNegative Integer getMissingCount(final LazyFacetTreeItem item) throws IOException {
		return this.readToolbox.loadCount(
				this.queryFactory.createMissingQuery(item.getQuery(), this.usedFieldsField, item.getDimension()));
	}

	/**
	 * Loads the backing facet of the given instance from Lucene.
	 * <p>
	 * Empty {@link Optional} if the facet for the dimension of this instance is
	 * empty. You may still try {@link #getMissingCount(LazyFacetTreeItem)}.
	 * 
	 * @param item
	 * 
	 * @param maxHits Do <strong>not</strong> simply set this to a high value like
	 *                {@link Integer#MAX_VALUE}, as this may result in a
	 *                <code>null</code> return from Lucene for large results.
	 * @return An empty {@link Optional} if the facet of this instance does not
	 *         contain values.
	 * @throws IOException
	 */
	public Optional<FacetResult> getFacet(final LazyFacetTreeItem item, final int maxHits) throws IOException {
		return this.readToolbox.getFacetResult(item.getQuery(), item.getDimension(), maxHits);
	}

}
