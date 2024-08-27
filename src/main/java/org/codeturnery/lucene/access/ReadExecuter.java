package org.codeturnery.lucene.access;

import java.io.IOException;

import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.search.IndexSearcher;

public interface ReadExecuter {
	public <R> R read(final ReadFunction<R> function) throws IOException;

	/**
	 * Instances of this function are provided with the necessary parameters to
	 * search a Lucene index.
	 *
	 * @param <R> The type of the return of this function.
	 */
	@FunctionalInterface
	interface ReadFunction<R> {
		R apply(final IndexSearcher searcher, final TaxonomyReader taxonomyReader, final FacetsConfig config) throws IOException;
	}
}
