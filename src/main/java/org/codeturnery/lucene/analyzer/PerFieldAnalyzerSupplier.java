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
	private final PerFieldAnalyzerWrapper analyzer;
	private final KeywordAnalyzer keywordAnalyzer;
	private final StandardAnalyzer standardAnalyzer;

	public PerFieldAnalyzerSupplier(Collection<String> keywordFields) {
		this.keywordAnalyzer = new KeywordAnalyzer();
		this.standardAnalyzer = new StandardAnalyzer();
		final var analyzerFields = new HashMap<String, Analyzer>(keywordFields.size());
		addAll(keywordFields, this.keywordAnalyzer, analyzerFields);
		this.analyzer = new PerFieldAnalyzerWrapper(this.standardAnalyzer, analyzerFields);
	}

	@Override
	public Analyzer get() {
		return this.analyzer;
	}
	
	@Override
	public void close() throws IOException {
		IOUtils.close(this.analyzer, this.keywordAnalyzer, this.standardAnalyzer);
	}

	private static <K, V> void addAll(final Iterable<K> keys, final V value, final Map<K, V> map) {
		for (final K key : keys) {
			map.put(key, value);
		}
	}
}
