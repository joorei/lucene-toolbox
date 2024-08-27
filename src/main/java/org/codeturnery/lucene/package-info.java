/**
 *
 */
/**
 * In this namespace (and its sub-namespaces) various types are collected that
 * aim to ease the usage of the Lucene library.
 * <p>
 * Within {@link org.codeturnery.lucene.access} you will find classes that work
 * directly with the index directories, e.g. reading and writing documents.
 * <p>
 * Within {@link org.codeturnery.lucene.query} you will find classes that can be
 * used to build queries, which can then be used in
 * {@link org.codeturnery.lucene.access}.
 * <p>
 * Within {@link org.codeturnery.lucene.document} you will find classes that can
 * be used to create {@link org.apache.lucene.document.Document}s, which can
 * then be used in {@link org.codeturnery.lucene.access}.
 * <p>
 * Within {@link org.codeturnery.lucene.analyzer} you will find classes that can
 * be used to create {@link org.apache.lucene.analysis.Analyzer} instances.
 * <p>
 * Within {@link org.codeturnery.lucene.navigation} you will find classes that aim
 * to be usable to provide the means to users to navigate through the data in
 * the Lucene index.
 */
@org.eclipse.jdt.annotation.NonNullByDefault
package org.codeturnery.lucene;