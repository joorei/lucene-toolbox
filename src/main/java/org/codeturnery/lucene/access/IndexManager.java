package org.codeturnery.lucene.access;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.SearcherTaxonomyManager;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.ControlledRealTimeReopenThread;
import org.apache.lucene.search.QueryCache;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.IOUtils;
import org.eclipse.jdt.annotation.Checks;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: allow TrackingIndexWriter-like features? See
 * <a href="http://www.lucenetutorial.com/lucene-nrt-hello-world.html">this</a>
 * but note that the class is nowhere found in Lucene 8. Maybe instead
 * {@link ControlledRealTimeReopenThread} or
 * {@link org.apache.lucene.search.ReferenceManager.RefreshListener} can be
 * used?
 * <p>
 * TODO: add {@link QueryCache} feature support?
 */
@SuppressWarnings({ "resource", "hiding" })
public class IndexManager implements Closeable {
	private static final Logger LOGGER = Checks.requireNonNull(LoggerFactory.getLogger(IndexManager.class));

	private final FacetsConfig facetsConfig;
	private final FSDirectory indexDirectory;
	private final FSDirectory taxonomyDirectory;
	private final double ramBufferSizeMb;
	private @Nullable IndexReader indexReader;
	private @Nullable IndexWriter indexWriter;
	private @Nullable DirectoryTaxonomyReader taxonomyReader;
	private @Nullable DirectoryTaxonomyWriter taxonomyWriter;
	private @Nullable SearcherTaxonomyManager searcherManager;
	private @Nullable SearcherTaxonomyManager writeBackedSearcherManager;

	public IndexManager(final Path indexPath, final Path taxonomyPath, final FacetsConfig facetsConfig)
			throws IOException {
		this(indexPath, taxonomyPath, facetsConfig, IndexWriterConfig.DEFAULT_RAM_BUFFER_SIZE_MB);
	}

	public IndexManager(final Path indexPath, final Path taxonomyPath, final FacetsConfig facetsConfig,
			final double ramBufferSizeMb) throws IOException {
		Files.createDirectories(indexPath);
		Files.createDirectories(taxonomyPath);
		this.facetsConfig = facetsConfig;
		this.ramBufferSizeMb = ramBufferSizeMb;

		/*
		 * let Lucene choose the FSDirectory implementation in case this is ported to
		 * different systems
		 */
		this.indexDirectory = Checks.requireNonNull(FSDirectory.open(indexPath));
		this.taxonomyDirectory = Checks.requireNonNull(FSDirectory.open(taxonomyPath));

		LOGGER.debug("Initialized index manager.");
	}

	public WriteExecuter getWriteExecuter(final Analyzer analyzer) throws IOException {
		// first open the index writer and only then the taxonomy writer
		final var indexWriter = getIndexWriter(analyzer);
		final var taxoWriter = getTaxonomyWriter();
		return new WriteExecuterImpl(indexWriter, taxoWriter, this.facetsConfig);
	}

	public ReadExecuter getReadExecuter() throws IOException {
		return new ReadExecuterImpl(getSearcherManager(), this.facetsConfig);
	}

	public ReadExecuter getWriteBackedReaderExecuter(final Analyzer analyzer) throws IOException {
		return new ReadExecuterImpl(getWriteBackedSearcherManager(analyzer), this.facetsConfig);
	}

	public void maybeRefreshAll() throws IOException {
		if (this.searcherManager != null) {
			this.searcherManager.maybeRefresh();
		}
		if (this.writeBackedSearcherManager != null) {
			this.writeBackedSearcherManager.maybeRefresh();
		}
	}

	@Override
	public void close() throws IOException {
		IOUtils.close(this.searcherManager, this.writeBackedSearcherManager, this.taxonomyWriter, this.indexWriter,
				this.taxonomyReader, this.indexReader, this.taxonomyDirectory, this.indexDirectory);
	}

	private SearcherTaxonomyManager getWriteBackedSearcherManager(final Analyzer analyzer) throws IOException {
		if (this.writeBackedSearcherManager == null) {
			// first open the index writer and only then the taxonomy writer
			final var indexWriter = getIndexWriter(analyzer);
			final var taxoWriter = getTaxonomyWriter();
			this.writeBackedSearcherManager = new SearcherTaxonomyManager(indexWriter, null, taxoWriter);
		}
		return Checks.requireNonNull(this.writeBackedSearcherManager);
	}

	private SearcherTaxonomyManager getSearcherManager() throws IOException {
		if (this.searcherManager == null) {
			// first open the index reader and only then the taxonomy reader
			final var indexReader = getIndexReader();
			final var taxonomyReader = getTaxonomyReader();
			this.searcherManager = new SearcherTaxonomyManager(indexReader, taxonomyReader, null);
		}
		return Checks.requireNonNull(this.searcherManager);
	}

	private IndexReader getIndexReader() throws IOException {
		if (this.indexReader == null) {
			this.indexReader = DirectoryReader.open(this.indexDirectory);
		}
		return Checks.requireNonNull(this.indexReader);
	}

	private DirectoryTaxonomyReader getTaxonomyReader() throws IOException {
		if (this.taxonomyReader == null) {
			this.taxonomyReader = new DirectoryTaxonomyReader(this.taxonomyDirectory);
		}
		return Checks.requireNonNull(this.taxonomyReader);
	}

	private DirectoryTaxonomyWriter getTaxonomyWriter() throws IOException {
		if (this.taxonomyWriter == null) {
			this.taxonomyWriter = new DirectoryTaxonomyWriter(this.taxonomyDirectory, OpenMode.CREATE_OR_APPEND);
		}
		return Checks.requireNonNull(this.taxonomyWriter);
	}

	private IndexWriter getIndexWriter(final Analyzer analyzer) throws IOException {
		if (this.indexWriter == null) {
			final var indexWriterConfig = new IndexWriterConfig(analyzer);
			indexWriterConfig.setOpenMode(OpenMode.CREATE_OR_APPEND);
			indexWriterConfig.setRAMBufferSizeMB(this.ramBufferSizeMb);
			this.indexWriter = new IndexWriter(this.indexDirectory, indexWriterConfig);
		}
		return Checks.requireNonNull(this.indexWriter);
	}
}
