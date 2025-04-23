package org.codeturnery.lucene.document;

import org.apache.lucene.facet.FacetsConfig;

/**
 * Thread safe.
 */
public class FacetConfigFactory {
	public FacetsConfig createFacetsConfig(final Iterable<FacetFieldConfig> facetFields) {
		final var facetsConfig = new FacetsConfig();
		fillFacetsConfig(facetsConfig, facetFields);
		return facetsConfig;
	}
	
	public FacetsConfig createFacetsConfig(final FacetFieldConfig[] facetFields) {
		final var facetsConfig = new FacetsConfig();
		fillFacetsConfig(facetsConfig, facetFields);
		return facetsConfig;
	}

	public void fillFacetsConfig(final FacetsConfig facetsConfig, final Iterable<FacetFieldConfig> facetFields) {
		for (final FacetFieldConfig facetField : facetFields) {
			addToFacetsConfig(facetsConfig, facetField);
		}
	}
	
	public void fillFacetsConfig(final FacetsConfig facetsConfig, final FacetFieldConfig[] facetFields) {
		for (final FacetFieldConfig facetField : facetFields) {
			addToFacetsConfig(facetsConfig, facetField);
		}
	}
	
	public void addToFacetsConfig(final FacetsConfig facetsConfig, final FacetFieldConfig facetField) {
		final String facetDim = facetField.getFacetDimension();
		facetsConfig.setHierarchical(facetDim, facetField.isHierarchical());
		facetsConfig.setMultiValued(facetDim, facetField.isMultiValued());
		facetsConfig.setDrillDownTermsIndexing(facetDim, facetField.getDrillDownTermsIndexing());
		facetsConfig.setIndexFieldName(facetDim, facetField.getIndexFieldName());
		facetsConfig.setRequireDimCount(facetDim, facetField.isDimCountRequired());
	}
}
