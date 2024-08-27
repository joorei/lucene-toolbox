package org.codeturnery.lucene.access;

import java.io.IOException;

import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.index.IndexWriter;

public interface WriteExecuter {
	public <R> R write(final WriteFunction<R> function) throws IOException;
	public long writeSingleDocument(final SingleDocumentWriteFunction function) throws IOException;

	@FunctionalInterface
	interface WriteFunction<R> {
		R apply(final IndexWriter indexWriter, final TaxonomyWriter taxonomyWriter, final FacetsConfig facetsConfig) throws IOException;
	}
	
	@FunctionalInterface
	interface SingleDocumentWriteFunction {
		long apply(final IndexWriter indexWriter, final TaxonomyWriter taxonomyWriter, final FacetsConfig facetsConfig) throws IOException;
	}
}
