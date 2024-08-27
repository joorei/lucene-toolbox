package org.codeturnery.lucene;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.codeturnery.lucene.access.ReadExecuter;
import org.codeturnery.lucene.access.ReadToolbox;
import org.codeturnery.lucene.navigation.LazyFacetTree;
import org.codeturnery.lucene.navigation.NavigationFetcher;
import org.codeturnery.lucene.navigation.LazyFacetTree.LazyFacetTreeItem;
import org.codeturnery.lucene.query.QueryFactory;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.io.TempDir;

@TestInstance(Lifecycle.PER_CLASS)
public class ReaderTest {
	@TempDir
	static Path sharedTempDir;

	TestIndex testIndex; 
	
	@BeforeAll
	void initializeIndex() throws IOException {
		this.testIndex = new TestIndex(sharedTempDir);
	}
	
	@Test
	void testMissingCount() throws IOException {
		try (final var analyzerSupplier = TestIndex.getAnalyzerSupplier();
				final var luceneIndex = this.testIndex.getIndexManager();) {
			final ReadExecuter readExecuter = luceneIndex.getReadExecuter();
			final var reader = new ReadToolbox(readExecuter);
			final var queryFactory = new QueryFactory();
			final Integer missingCategoryCount = reader.loadCount(queryFactory.createMissingQuery(
				new MatchAllDocsQuery(),
				TestIndex.USED_FIELDS_DIMENSION,
				TestIndex.CATEGORY_DIMENSION
			));
			// only one document has no categories set
			assertEquals(2, missingCategoryCount.intValue());
			final Integer missingNameCount = reader.loadCount(queryFactory.createMissingQuery(
					new MatchAllDocsQuery(),
					TestIndex.USED_FIELDS_DIMENSION,
					TestIndex.NAME_DIMENSION
			));
			// all documents have their name set
			assertEquals(0, missingNameCount.intValue());
			final Integer missingFieldsCount = reader.loadCount(queryFactory.createMissingQuery(
					new MatchAllDocsQuery(),
					TestIndex.USED_FIELDS_DIMENSION,
					TestIndex.USED_FIELDS_DIMENSION
			));
			// every document has at least one value set in the USED_FIELDS_DIMENSION field, but as
			// USED_FIELDS_DIMENSION is never set as value itself in this field we get all documents (5).
			assertEquals(6, missingFieldsCount.intValue());
		}
	}
	
	@Test
	void testMissingDocuments() throws IOException {
		try (final var analyzerSupplier = TestIndex.getAnalyzerSupplier();
				final var luceneIndex = this.testIndex.getIndexManager();) {
			final ReadExecuter readExecuter = luceneIndex.getReadExecuter();
			final var reader = new ReadToolbox(readExecuter);
			final var navigationFetcher = new NavigationFetcher(reader, TestIndex.USED_FIELDS_DIMENSION);
			final var queryFactory = new QueryFactory();
			final var baseQuery = queryFactory.createMatchAllQuery();
			final var tree = new LazyFacetTree(() -> baseQuery, queryFactory, TestIndex.getFacetsConfig(), TestIndex.USED_FIELDS_DIMENSION);
			var item = tree.createRoot(TestIndex.CATEGORY_DIMENSION);
			var labelsAndValues = new LabelAndValue[] {
					new LabelAndValue("mobility", 3),
					new LabelAndValue("technical", 2),
					new LabelAndValue("animal", 2),
					new LabelAndValue("expansive", 1),
					new LabelAndValue("farm", 1),
					new LabelAndValue("public transportation", 1),
			};
			compareFacets(item, navigationFetcher, 6, 2, labelsAndValues, TestIndex.CATEGORY_DIMENSION);
			item = item.createMissingChild(TestIndex.COLOR_DIMENSION);
			labelsAndValues = new LabelAndValue[] {
					new LabelAndValue("rainbow", 1),
			};
			compareFacets(item, navigationFetcher, 1, 1, labelsAndValues, TestIndex.COLOR_DIMENSION);
		}
	}
	
	private void compareFacets(LazyFacetTreeItem item, final NavigationFetcher navigationFetcher, int expectedCategoryCount, int expectedMissingCount, LabelAndValue[] expectedLabelAndValues, final String expectedDimension) throws IOException {
		final var limit = 10;
		final var maybeFacet = navigationFetcher.getFacet(item, limit);
		final int missingCount = navigationFetcher.getMissingCount(item);
		assertEquals(expectedDimension, item.getDimension());
		assertEquals(expectedMissingCount, missingCount);
		final int categoryCount = maybeFacet.isPresent() ? maybeFacet.get().childCount : 0;
		assertEquals(expectedCategoryCount, categoryCount);
		maybeFacet.ifPresentOrElse(facet -> {
			final var labelAndValues = facet.labelValues;
			assertEquals(expectedLabelAndValues.length, facet.labelValues.length);
			for (int i = 0; i < labelAndValues.length; i++) {
				assertEquals(expectedLabelAndValues[i].label, labelAndValues[i].label);
				assertEquals(expectedLabelAndValues[i].value, labelAndValues[i].value);
			}
		}, Assert::fail);
	}
}
