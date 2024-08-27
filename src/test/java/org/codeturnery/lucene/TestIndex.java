package org.codeturnery.lucene;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.index.IndexOptions;
import org.codeturnery.lucene.access.IndexManager;
import org.codeturnery.lucene.access.WriteToolbox;
import org.codeturnery.lucene.analyzer.PerFieldAnalyzerSupplier;

@SuppressWarnings("null")
public class TestIndex {
	static final String CATEGORY_DIMENSION = "CATEGORY";
	static final String USED_FIELDS_DIMENSION = "USED_FIELDS";
	static final String NAME_DIMENSION = "NAME";
	static final String COLOR_DIMENSION = "COLOR";
	
	private final Path indexPath;
	private final Path taxonomyPath;

	public TestIndex(final Path directory) throws IOException {
		this.indexPath = directory.resolve("test_index");
		this.taxonomyPath = directory.resolve("test_taxonomy");
		Files.createDirectories(this.indexPath);
		Files.createDirectories(this.taxonomyPath);

		try (final var analyzerSupplier = getAnalyzerSupplier(); final var luceneIndex = getIndexManager();) {
			final var writeToolbox = new WriteToolbox(luceneIndex.getWriteExecuter(analyzerSupplier.get()));
			for (final var entry : getFixtures()) {
				writeToolbox.accept(entry);
			}
			writeToolbox.commit();
		}
	}

	IndexManager getIndexManager() throws IOException {
		return new IndexManager(this.indexPath, this.taxonomyPath, getFacetsConfig());
	}

	static FacetsConfig getFacetsConfig() {
		final var facetsConfig = new FacetsConfig();
		facetsConfig.setHierarchical(CATEGORY_DIMENSION, false);
		facetsConfig.setMultiValued(CATEGORY_DIMENSION, true);
		facetsConfig.setHierarchical(COLOR_DIMENSION, false);
		facetsConfig.setMultiValued(COLOR_DIMENSION, true);
		facetsConfig.setHierarchical(USED_FIELDS_DIMENSION, false);
		facetsConfig.setMultiValued(USED_FIELDS_DIMENSION, true);
		facetsConfig.setHierarchical(NAME_DIMENSION, false);
		facetsConfig.setMultiValued(NAME_DIMENSION, false);
		return facetsConfig;
	}

	static FieldType getExactMatchFieldType() {
		final FieldType exactMatchField = new FieldType();
		exactMatchField.setStored(true);
		exactMatchField.setTokenized(false);
		exactMatchField.setOmitNorms(true);
		exactMatchField.setIndexOptions(IndexOptions.DOCS);
		exactMatchField.freeze();
		return exactMatchField;
	}

	static <T extends Closeable & Supplier<Analyzer>> T getAnalyzerSupplier() {
		final var keywordFields = new ArrayList<String>(2);
		keywordFields.add(CATEGORY_DIMENSION);
		keywordFields.add(NAME_DIMENSION);
		return (T) new PerFieldAnalyzerSupplier(keywordFields);
	}

	static List<Document> getFixtures() {
		return Arrays.asList(
				create("car", Arrays.asList("mobility", "technical", "expansive"), Arrays.asList("yellow", "green")),
				create("chicken", Arrays.asList("animal", "farm"), Arrays.asList("white")),
				create("train", Arrays.asList("mobility", "technical", "public transportation"),
						Arrays.asList("green")),
				create("horse", Arrays.asList("mobility", "animal"), Arrays.asList("brown", "white")),
				create("blob", Collections.emptyList(), Arrays.asList("rainbow")),
				create("unknown", Collections.emptyList(), Arrays.asList()));
	}

	private static Document create(final String name, final List<String> categories, final List<String> colors) {
		final var document = new Document();
		final var usedFields = new ArrayList<String>(2);
		document.add(new Field(NAME_DIMENSION, name, getExactMatchFieldType()));
		document.add(new FacetField(NAME_DIMENSION, name));
		usedFields.add(NAME_DIMENSION);
		for (final var value : categories) {
			document.add(new Field(CATEGORY_DIMENSION, value, getExactMatchFieldType()));
			document.add(new FacetField(CATEGORY_DIMENSION, value));
			usedFields.add(CATEGORY_DIMENSION);
		}
		for (final var value : colors) {
			document.add(new Field(COLOR_DIMENSION, value, getExactMatchFieldType()));
			document.add(new FacetField(COLOR_DIMENSION, value));
			usedFields.add(COLOR_DIMENSION);
		}
		for (final var value : usedFields) {
			document.add(new Field(USED_FIELDS_DIMENSION, value, getExactMatchFieldType()));
		}
		return document;
	}
}
