package org.codeturnery.lucene.access;

import java.io.IOException;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.SearcherTaxonomyManager;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.SearcherTaxonomyManager.SearcherAndTaxonomy;
import org.apache.lucene.search.IndexSearcher;
import org.eclipse.jdt.annotation.Checks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadExecuterImpl implements ReadExecuter {
	private static final Logger LOGGER = Checks.requireNonNull(LoggerFactory.getLogger(ReadExecuterImpl.class));

	private final FacetsConfig facetsConfig;
	private final SearcherTaxonomyManager searcherManager;

	public ReadExecuterImpl(final SearcherTaxonomyManager searcherManager, final FacetsConfig facetsConfig) {
		this.searcherManager = searcherManager;
		this.facetsConfig = facetsConfig;
	}

	@Override
	public <R> R read(final ReadFunction<R> function) throws IOException {
		final SearcherAndTaxonomy searcherAndTaxonomy = Checks.requireNonNull(this.searcherManager.acquire());
		final IndexSearcher indexSearcher = Checks.requireNonNull(searcherAndTaxonomy.searcher);
		final TaxonomyReader taxonomyReader = Checks.requireNonNull(searcherAndTaxonomy.taxonomyReader);
		try {
			return function.apply(indexSearcher, taxonomyReader, this.facetsConfig);
		} finally {
			this.searcherManager.release(searcherAndTaxonomy);
		}
	}
}
