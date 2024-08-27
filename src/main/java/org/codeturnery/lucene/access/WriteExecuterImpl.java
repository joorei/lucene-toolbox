package org.codeturnery.lucene.access;

import java.io.Closeable;
import java.io.IOException;

import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.index.IndexWriter;

/**
 * Wrapper class around multiple Lucene writer instances. Frees the application
 * logic from handling these instances manually. Instead, functions can be
 * passed into this instance to execute different tasks. These functions will be
 * provided with the most recent (not necessarily up-to-date) Lucene instances.
 * <p>
 * Note that this class will not automatically changes or update readers to work
 * on newly written data.
 * <p>
 * Note {@link Closeable} objects given as parameter when creating an instance
 * of this class will <strong>not</strong> be automatically closed when the
 * resulting instance is closed. <strong>Ensure that the {@link TaxonomyWriter}
 * is closed first and only then the {@link IndexWriter}.</strong>
 */
public class WriteExecuterImpl implements WriteExecuter {
	private final IndexWriter indexWriter;
	private final TaxonomyWriter taxonomyWriter;
	private final FacetsConfig facetsConfig;

	public WriteExecuterImpl(final IndexWriter indexWriter, final TaxonomyWriter taxonomyWriter,
			final FacetsConfig facetsConfig) {
		this.indexWriter = indexWriter;
		this.taxonomyWriter = taxonomyWriter;
		this.facetsConfig = facetsConfig;
	}

	@Override
	public <R> R write(final WriteFunction<R> function) throws IOException {
		return function.apply(this.indexWriter, this.taxonomyWriter, this.facetsConfig);
	}

	@Override
	public long writeSingleDocument(SingleDocumentWriteFunction function) throws IOException {
		return function.apply(this.indexWriter, this.taxonomyWriter, this.facetsConfig);
	}
}
