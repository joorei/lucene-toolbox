/**
 * Provides helpers to handle {@link org.apache.lucene.document.Document}s and
 * fields.
 * <p>
 * To access a Lucene index (e.g. using
 * {@link org.codeturnery.lucene.access.IndexManager}) a
 * {@link org.apache.lucene.facet.FacetsConfig} is needed. Such can be created
 * via {@link org.codeturnery.lucene.document.FacetConfigFactory}.
 * <p>
 * To create {@link org.apache.lucene.document.Document}s to index, you can
 * first use {@link org.codeturnery.lucene.document.FieldFactory} and
 * {@link org.codeturnery.lucene.document.CommonFieldTypes} to create the fields
 * that the document shall have.
 * <p>
 * Secondly, by extending
 * {@link org.codeturnery.lucene.document.DocumentBuilder}, you can
 * define methods to add specific fields.
 */
@org.eclipse.jdt.annotation.NonNullByDefault
package org.codeturnery.lucene.document;