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
 * Eases the creation of {@link ReadExecuter} and {@link WriteExecuter} to read/write from/to Lucene indexes.
 * <p>
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
	/**
	 * Do not use directly! Created and set via {@link #getIndexReader} when needed.
	 */
	private @Nullable IndexReader indexReader;
	/**
	 * Do not use directly! Created and set via {@link #getIndexWriter} when needed.
	 */
	private @Nullable IndexWriter indexWriter;
	/**
	 * Do not use directly! Created and set via {@link #getTaxonomyReader} when needed.
	 */
	private @Nullable DirectoryTaxonomyReader taxonomyReader;
	/**
	 * Do not use directly! Created and set via {@link #getTaxonomyWriter} when needed.
	 */
	private @Nullable DirectoryTaxonomyWriter taxonomyWriter;
	/**
	 * Do not use directly! Created and set via {@link #getSearcherManager} when needed.
	 */
	private @Nullable SearcherTaxonomyManager searcherManager;
	/**
	 * Do not use directly! Created and set via {@link #getWriteBackedSearcherManager} when needed.
	 */
	private @Nullable SearcherTaxonomyManager writeBackedSearcherManager;

	/**
	 * The RAM buffer size is automatically set to {@value IndexWriterConfig#DEFAULT_RAM_BUFFER_SIZE_MB} MB.
	 * 
	 * @param indexPath The path to the directory where the index is or shall be stored. Will be created automatically if missing.
	 * @param taxonomyPath The path to the directory where the taxonomy is or shall be stored. Will be created automatically if missing.
	 * @param facetsConfig The facet configuration to be used. If the index/taxonomy was already created, this configuration must be the same as the one used during creation.
	 * @throws IOException
	 */
	public IndexManager(final Path indexPath, final Path taxonomyPath, final FacetsConfig facetsConfig)
			throws IOException {
		this(indexPath, taxonomyPath, facetsConfig, IndexWriterConfig.DEFAULT_RAM_BUFFER_SIZE_MB);
	}
	
	public IndexManager(final IndexConfig indexConfig) throws IOException {
		this(indexConfig.getIndexPath(), indexConfig.getTaxonomyPath(), indexConfig.getFacetsContig(), indexConfig.getRamBufferSizeMb());
	}

	/**
	 * @param indexPath The path to the directory where the index is or shall be stored. Will be created automatically if missing.
	 * @param taxonomyPath The path to the directory where the taxonomy is or shall be stored. Will be created automatically if missing.
	 * @param facetsConfig The facet configuration to be used. If the index/taxonomy was already created, this configuration must be the same as the one used during creation.
	 * @param ramBufferSizeMb The RAM buffer size to use.
	 * @throws IOException
	 */
	public IndexManager(final Path indexPath, final Path taxonomyPath, final FacetsConfig facetsConfig,
			final double ramBufferSizeMb) throws IOException {
		if (indexPath.equals(taxonomyPath)) {
			throw new IllegalArgumentException("Index and taxonomy must use different directories");
		}
		assertDirectoryExists(indexPath);
		assertDirectoryExists(taxonomyPath);
		this.facetsConfig = facetsConfig;
		this.ramBufferSizeMb = ramBufferSizeMb;

		/*
		 * let Lucene choose the FSDirectory implementation in case this is ported to
		 * different systems
		 */
		this.indexDirectory = Checks.requireNonNull(FSDirectory.open(indexPath));
		this.taxonomyDirectory = Checks.requireNonNull(FSDirectory.open(taxonomyPath));

		LOGGER.debug("Initialized index manager with index '" + indexPath + "' and taxonomy '" + taxonomyPath + "'.");
	}
	
	protected void assertDirectoryExists(final Path path) throws IOException {
		if (!Files.exists(path)) {
			Files.createDirectories(path);
		} else if (!Files.isDirectory(path)) {
			throw new IllegalArgumentException("Directory must exist as a directory or not at all: " + path);
		}
	}

	/**
	 * @param analyzer
	 * @return A new instance for each call, backed by the same {@link Analyzer} as when this method was first called. FIXME: this is inacceptable regarding a clean method API
	 * @throws IOException
	 */
	public WriteExecuter getWriteExecuter(final Analyzer analyzer) throws IOException {
		// first open the index writer and only then the taxonomy writer
		final var indexWriter = getIndexWriter(analyzer);
		final var taxoWriter = getTaxonomyWriter();
		return new WriteExecuterImpl(indexWriter, taxoWriter, this.facetsConfig);
	}

	/**
	 * TODO: return the same instance every time?
	 * @return
	 * @throws IOException
	 */
	public ReadExecuter getReadExecuter() throws IOException {
		return new ReadExecuterImpl(getSearcherManager(), this.facetsConfig);
	}

	/**
	 * TODO: return the same instance every time? FIXME: how to handle different analyzers?
	 * @param analyzer
	 * @return
	 * @throws IOException
	 */
	public ReadExecuter getWriteBackedReaderExecuter(final Analyzer analyzer) throws IOException {
		return new ReadExecuterImpl(getWriteBackedSearcherManager(analyzer), this.facetsConfig);
	}

	/**
	 * If you've retrieved {@link SearcherTaxonomyManager} instances via {@link #getSearcherManager} or {@link #getWriteBackedSearcherManager}
	 * and want them to account for changes in the index after their creation (or last refresh), you need to call this method.
	 * @throws IOException
	 */
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

	/**
	 * After retrieval, consider if calling the {@link #maybeRefreshAll()} method is necessary for your use-case.
	 * 
	 * @param analyzer
	 * @return FIXME: the same instance after the first call, even if different analyzers are given on each call
	 * @throws IOException
	 */
	private SearcherTaxonomyManager getWriteBackedSearcherManager(final Analyzer analyzer) throws IOException {
		if (this.writeBackedSearcherManager == null) {
			// first open the index writer and only then the taxonomy writer
			final var indexWriter = getIndexWriter(analyzer);
			final var taxoWriter = getTaxonomyWriter();
			this.writeBackedSearcherManager = new SearcherTaxonomyManager(indexWriter, null, taxoWriter);
		}
		return Checks.requireNonNull(this.writeBackedSearcherManager);
	}

	/**
	 * After retrieval, consider if calling the {@link #maybeRefreshAll()} method is necessary for your use-case.
	 * 
	 * @return The same {@link SearcherTaxonomyManager} instance on every call in this {@link IndexManager} instance. 
	 * @throws IOException
	 */
	private SearcherTaxonomyManager getSearcherManager() throws IOException {
		if (this.searcherManager == null) {
			// first open the index reader and only then the taxonomy reader
			final var indexReader = getIndexReader();
			final var taxonomyReader = getTaxonomyReader();
			this.searcherManager = new SearcherTaxonomyManager(indexReader, taxonomyReader, null);
		}
		return Checks.requireNonNull(this.searcherManager);
	}

	/**
	 * @return The same {@link IndexReader} instance on every call in this {@link IndexManager} instance. 
	 * @throws IOException
	 */
	private IndexReader getIndexReader() throws IOException {
		if (this.indexReader == null) {
			this.indexReader = DirectoryReader.open(this.indexDirectory);
		}
		return Checks.requireNonNull(this.indexReader);
	}

	/**
	 * @return The same {@link DirectoryTaxonomyReader} instance on every call in this {@link IndexManager} instance. 
	 * @throws IOException
	 */
	private DirectoryTaxonomyReader getTaxonomyReader() throws IOException {
		if (this.taxonomyReader == null) {
			this.taxonomyReader = new DirectoryTaxonomyReader(this.taxonomyDirectory);
		}
		return Checks.requireNonNull(this.taxonomyReader);
	}

	/**
	 * @return The same {@link DirectoryTaxonomyWriter} instance on every call in this {@link IndexManager} instance. 
	 * @throws IOException
	 */
	private DirectoryTaxonomyWriter getTaxonomyWriter() throws IOException {
		if (this.taxonomyWriter == null) {
			this.taxonomyWriter = new DirectoryTaxonomyWriter(this.taxonomyDirectory, OpenMode.CREATE_OR_APPEND);
		}
		return Checks.requireNonNull(this.taxonomyWriter);
	}

	/**
	 * @param analyzer 
	 * @return The same {@link IndexWriter} instance on every call in this {@link IndexManager} instance. FIXME: here too, passing different analyzers is a problem
	 * @throws IOException
	 */
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
