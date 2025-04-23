package org.codeturnery.lucene.access;

import java.io.IOException;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.SearcherTaxonomyManager;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.SearcherTaxonomyManager.SearcherAndTaxonomy;
import org.apache.lucene.search.IndexSearcher;
import org.eclipse.jdt.annotation.Checks;

public class ReadExecuterImpl implements ReadExecuter {
	private final FacetsConfig facetsConfig;
	private final SearcherTaxonomyManager searcherManager;

	public ReadExecuterImpl(final SearcherTaxonomyManager searcherManager, final FacetsConfig facetsConfig) {
		this.searcherManager = searcherManager;
		this.facetsConfig = facetsConfig;
	}

	@Override
	public int readInt(ReadIntFunction function) throws IOException {
		final SearcherAndTaxonomy searcherAndTaxonomy = Checks.requireNonNull(this.searcherManager.acquire());
		final IndexSearcher indexSearcher = Checks.requireNonNull(searcherAndTaxonomy.searcher);
		final TaxonomyReader taxonomyReader = Checks.requireNonNull(searcherAndTaxonomy.taxonomyReader);
		try {
			// taxonomyReader must not be closed here, as it is needed for further usage in other calls
			return function.apply(indexSearcher, taxonomyReader, this.facetsConfig);
		} finally {
			this.searcherManager.release(searcherAndTaxonomy);
		}
	}

	@Override
	public <R> R read(final ReadFunction<R> function) throws IOException {
		final SearcherAndTaxonomy searcherAndTaxonomy = Checks.requireNonNull(this.searcherManager.acquire());
		final IndexSearcher indexSearcher = Checks.requireNonNull(searcherAndTaxonomy.searcher);
		final TaxonomyReader taxonomyReader = Checks.requireNonNull(searcherAndTaxonomy.taxonomyReader);
		try {
			// taxonomyReader must not be closed here, as it is needed for further usage in other calls
			return function.apply(indexSearcher, taxonomyReader, this.facetsConfig);
		} finally {
			this.searcherManager.release(searcherAndTaxonomy);
		}
	}
}
