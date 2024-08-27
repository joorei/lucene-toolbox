package org.codeturnery.lucene.experimental;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.IOUtils;
import org.codeturnery.lucene.access.PojoReadRequest;
import org.codeturnery.lucene.access.PojoReadResponse;
import org.codeturnery.lucene.access.ReadToolbox;
import org.codeturnery.lucene.query.QueryFactory;
import org.codeturnery.lucene.query.TermConjunction;
import org.eclipse.jdt.annotation.Nullable;

public class Snippets {
	

	// using an ReaderManager is also possible
	public static void reopenReader(DirectoryReader indexReader, DirectoryTaxonomyReader taxonomyReader) throws IOException {
		// TODO: After a reopen() on the IndexReader, refresh() the TaxonomyReader. No
		// search should be performed on the new IndexReader until refresh() has
		// finished.
		// TODO: correct reopening order?
		final DirectoryReader newIndexReader = DirectoryReader.openIfChanged(indexReader);
		if (newIndexReader != null) {
			final IndexReader oldIndexReader = indexReader;
			indexReader = newIndexReader;
			IOUtils.close(oldIndexReader);
		}
		final @Nullable DirectoryTaxonomyReader newTaxonomyReader = TaxonomyReader.openIfChanged(taxonomyReader);
		if (newTaxonomyReader != null) {
			final TaxonomyReader oldTaxonomyReader = taxonomyReader;
			taxonomyReader = newTaxonomyReader;
			IOUtils.close(oldTaxonomyReader);
		}
		
		// TODO: return the (potentially) new readers
	}
	

	public TreeNode<CharSequence, Document> groupQueryResult(final QueryFactory queryFactory, final ReadToolbox readToolbox) throws IOException {
		// prepare hardcoded deciders
		final Collection<Function<Document, Collection<CharSequence>>> groupDeciders = new ArrayList<>(3);
		// groupDeciders.add(document -> Arrays.asList(document.get("TAG")));
		// more field names
		// ...
		
		// prepare hardcoded sorters
		final Collection<Comparator<Document>> childrenSorters = new ArrayList<>(1);
		final Collection<Comparator<CharSequence>> subNodeSorters = new ArrayList<>(1);
		subNodeSorters.add((o1, o2) -> o1.toString().compareToIgnoreCase(o2.toString()));
		childrenSorters.add((o1, o2) -> o1.get("TAG").compareTo(o2.get("TAG")));
		// create grouper and sorter
		final ElementGrouper<CharSequence, Document> grouper = new ElementGrouper<>(groupDeciders);
//		TreeSorter<CharSequence,Document> sorter = new TreeSorter<>(subNodeSorters, childrenSorters);
//		// execute grouping and sorting
		final Query query = queryFactory.createMatchAllQuery();
		final Set<String> fields = new HashSet<>();
		//fields.add("TAG");
		// more field names
		// ...
		
		final var readRequest = new PojoReadRequest();
		readRequest.setQuery(query);
		readRequest.setMaxDocumentCount(Integer.MAX_VALUE);
		readRequest.setDocumentFieldsToLoad(fields);
		final var response = new PojoReadResponse();
		readToolbox.loadDocuments(readRequest, response);
		final Document[] result = response.getDocuments();
		final TreeNode<CharSequence, Document> insertionSortedNode = grouper.apply(Arrays.asList(result));
		// return sorter.apply(); TODO: grouping and sorting should only happen for
		// nodes actually used
		return insertionSortedNode;
//		final Map<String,String> navigatedFacets = new HashMap<>(1);
//		navigatedFacets.put(OpusField.CONTENTS.name(), "Loli");
//		return getFacetNodes(OpusField.PARODY.name(), navigatedFacets);
	}

	public TreeNode<CharSequence, Document> getFacetNodes(final QueryFactory queryFactory, final String currentDimension,
			final Map<String, String> navigatedFacets, final Query currentQuery, final ReadToolbox readToolbox, final FacetsConfig facetsConfig) throws IOException {
		final Map<String, TermConjunction> conjunctions = new HashMap<>(navigatedFacets.size());
		for (final Entry<String, String> navigatedFacet : navigatedFacets.entrySet()) {
			conjunctions.put(navigatedFacet.getKey(),
					new TermConjunction(false, new String[] { navigatedFacet.getValue() }));
		}
		final Query query = queryFactory.createDrillDownQuery(currentQuery, conjunctions,
				facetsConfig);
		// TODO: handle empty optional
		final var facetResult = readToolbox.getFacetResult(query, currentDimension, Integer.MAX_VALUE).orElseThrow();
		final TreeNode<CharSequence, Document> root = new TreeNode<>(null);
//		root.addSubNode(root, facetResult.dim + " (" + facetResult.childCount + ")");
//		if (facetResult == null) {
//			return root;
//		}
		for (final LabelAndValue labelAndValue : facetResult.labelValues) {
			final TreeNode<CharSequence, Document> subChild = new TreeNode<>(root);
			root.addSubNode(subChild, labelAndValue.label + " (" + labelAndValue.value + ")");
		}

		return root;
	}
}
