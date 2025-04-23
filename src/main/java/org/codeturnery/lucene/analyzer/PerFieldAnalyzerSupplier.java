package org.codeturnery.lucene.analyzer;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.IOUtils;

public class PerFieldAnalyzerSupplier implements Supplier<Analyzer>, Closeable {
	private final Closeable[] closeables;
	private final PerFieldAnalyzerWrapper analyzer;

	/**
	 * Uses a {@link StandardAnalyzer} as default and a {@link KeywordAnalyzer} for
	 * each provided <code>keywordField</code>.
	 * 
	 * @param keywordFields
	 */
	@SuppressWarnings("resource")
	public PerFieldAnalyzerSupplier(Collection<String> keywordFields) {
		final var keywordAnalyzer = new KeywordAnalyzer();
		final var standardAnalyzer = new StandardAnalyzer();
		final var analyzerFields = new HashMap<String, Analyzer>(keywordFields.size());
		addAll(keywordFields, keywordAnalyzer, analyzerFields);
		this.analyzer = new PerFieldAnalyzerWrapper(standardAnalyzer, analyzerFields);

		this.closeables = new Closeable[] {this.analyzer, keywordAnalyzer, standardAnalyzer };
	}

	@Override
	public Analyzer get() {
		return this.analyzer;
	}

	@Override
	public void close() throws IOException {
		IOUtils.close(this.closeables);
	}

	private static <K, V> void addAll(final Iterable<K> keys, final V value, final Map<K, V> map) {
		for (final K key : keys) {
			map.put(key, value);
		}
	}
}
