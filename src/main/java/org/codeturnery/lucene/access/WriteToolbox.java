package org.codeturnery.lucene.access;

import java.io.IOException;
import org.apache.lucene.document.Document;
import org.eclipse.jdt.annotation.Checks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WriteToolbox {
	private static final Logger LOGGER = Checks.requireNonNull(LoggerFactory.getLogger(WriteToolbox.class));

	private final WriteExecuter writeManager;

	public WriteToolbox(final WriteExecuter writeManager) {
		this.writeManager = writeManager;
	}

	/**
	 * Commits the changes to the index.
	 * <p>
	 * You may want to call {@link #merge()} afterwards. Regardless of merging you
	 * must reopen the reader for the changes to be visible.
	 */
	public long[] commit() throws IOException {
		return this.writeManager.write((indexWriter, taxonomyWriter, facetsConfig) -> {
			LOGGER.trace("Committing all write requests.");

			// first commit the taxonomy writer and only then the index writer
			// TODO: what of the calls are actually needed?
			indexWriter.flush();
			final long taxoPrepare = taxonomyWriter.prepareCommit();
			final long indexPrepare = indexWriter.prepareCommit();
			final long taxoCommit = taxonomyWriter.commit();
			final long indexCommit = indexWriter.commit();
			return new long[] { taxoPrepare, indexPrepare, taxoCommit, indexCommit };
		});
	}

	public long[] initializeIndex() throws IOException {
		return this.writeManager.write((indexWriter, taxonomyWriter, facetsConfig) -> {
			// initialize index if not created yet, otherwise opening a reader would fail
			LOGGER.trace("Initializing empty index and empty taxonomy index if not yet present.");
			final long taxCommit = taxonomyWriter.commit();
			final long indexCommit = indexWriter.commit();
			return new long[] { taxCommit, indexCommit };
		});
	}

	public void merge() throws IOException {
		this.writeManager.write((indexWriter, taxonomyWriter, facetsConfig) -> {
			LOGGER.trace("Merging index into a single segment.");
			indexWriter.forceMerge(1);
			return null;
		});
	}

	public long accept(final Document document) throws IOException {
		return this.writeManager.writeSingleDocument((indexWriter, taxonomyWriter, facetsConfig) -> {
			LOGGER.trace("Writing document into index.");
			// TODO: we expect the caller for now to take care to not add duplicates
			return indexWriter.addDocument(facetsConfig.build(taxonomyWriter, document));
		});
	}

	/**
	 * Call {@link #commit()} to complete the purge.
	 * @return 
	 * 
	 * @throws IOException
	 */
	public Long purge() throws IOException {
		return this.writeManager.write((indexWriter, taxonomyWriter, facetsConfig) -> {
			LOGGER.trace("Purging index.");
			return Long.valueOf(indexWriter.deleteAll());
		});
	}
}
