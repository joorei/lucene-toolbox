package org.codeturnery.lucene.document;

import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.FacetsConfig.DimConfig;
import org.apache.lucene.facet.FacetsConfig.DrillDownTermsIndexing;

/**
 * Implement this interface to provide {@link FacetConfigFactory} with instances
 * from which a {@link FacetsConfig} is filled.
 * <p>
 * Using an enum as implementation can be effective for many different fields.
 */
public interface FacetFieldConfig {
	/**
	 * Identifies the facet field.
	 * 
	 * @return
	 */
	public String getFacetDimension();

	/**
	 * Return {@code false} to keep the default.
	 * 
	 * @return
	 * 
	 * @see DimConfig#multiValued
	 * @see FacetsConfig#setMultiValued
	 */
	public boolean isMultiValued();

	/**
	 * Return {@code false} to keep the default.
	 * 
	 * @return
	 * 
	 * @see DimConfig#hierarchical
	 * @see FacetsConfig#setHierarchical
	 */
	public boolean isHierarchical();

	/**
	 * Return {@link DrillDownTermsIndexing#ALL} to keep the default.
	 * 
	 * @return
	 * 
	 * @see DimConfig#drillDownTermsIndexing
	 * @see FacetsConfig#setDrillDownTermsIndexing
	 */
	public DrillDownTermsIndexing getDrillDownTermsIndexing();

	/**
	 * Return {@link FacetsConfig#DEFAULT_INDEX_FIELD_NAME} to keep the default.
	 * 
	 * @return
	 * 
	 * @see DimConfig#indexFieldName
	 * @see FacetsConfig#setIndexFieldName
	 */
	public String getIndexFieldName();

	/**
	 * Return {@code false} to keep the default.
	 * 
	 * @return
	 * 
	 * @see DimConfig#requireDimCount
	 * @see FacetsConfig#setRequireDimCount
	 */
	public boolean isDimCountRequired();
}
