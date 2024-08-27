/**
 *
 */
/**
 * This namespace provides you with classes to open an index and write documents
 * within it or read information from it.
 * <p>
 * To use it you need to create a
 * {@link org.codeturnery.lucene.access.WriteExecuterImpl} instance. Afterwards
 * you can:
 * <ul>
 * <li>Retrieve a {@link org.codeturnery.lucene.access.ReadToolbox} to load
 * information from the index.</li>
 * <li>Retrieve a {@link org.codeturnery.lucene.access.WriteToolbox} to change
 * information in the index.</li>
 * <li>Use the {@link org.codeturnery.lucene.access.WriteExecuterImpl} itself to
 * manage the index itself.</li>
 * </ul>
 */
@org.eclipse.jdt.annotation.NonNullByDefault
package org.codeturnery.lucene.access;