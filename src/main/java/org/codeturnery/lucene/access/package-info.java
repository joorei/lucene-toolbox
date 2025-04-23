/**
 * Classes in this package attempt to ease the reading from and writing to a
 * Lucene index.
 * <p>
 * Usage:
 * 
 * <pre>
 * <code>
 * var facetsConfig = new FacetsConfig();
 * // configure facets as needed
 * var indexPath = Paths.get("path/to/index");
 * var taxonomyPath = Path.get("path/to/taxonomy");
 * try (IndexManager indexManager = new IndexManager(indexPath, taxonomyPath, facetsConfig);) {
 *     var analyzer = new StandardAnalyzer(); // use the analyzer of your choice
 *     var writeExecuter = indexManager.getWriteExecuter(analyzer);
 *     var readExecuter = indexManager.getReadExecuter();
 *
 *     var writeResult = writeExecuter.write((indexWriter, taxonomyWriter, facetsConfig) -> {
 *         // your custom code to write arbitrary data into the Lucene index
 *         // ...
 *         return null;
 *     });
 *     long documentId = writeExecuter.writeSingleDocument((indexWriter, taxonomyWriter, facetsConfig) -> {
 *         // your custom code to write a document into the Lucene index
 *         // ...
 *         return 0;
 *     });
 *
 *     var writeToolbox = new WriteToolbox(writeExecuter);
 *     // use various prepared methods to manipulate the Lucene index
 *     // ...
 *
 *     var readResult = readExecuter.read((searcher, taxonomyReader, config) -> {
 *         // your code to read data from the Lucene index
 *         // ...
 *         return null;
 *     });
 *    
 *     var readToolbox = new ReadToolbox(readExecutor);
 *     // Use various prepared methods to read from the Lucene index.
 *     // Depending on the method, you can use a Lucene Query instance or
 *     // a ReadRequest and ReadResponse instance pair. In the latter case
 *     // all request data is stored in the ReadRequest instance and all
 *     // response data will be written into the ReadResponse instance,
 *     // from which you can retrieve it when the method finishes.
 * }
 * </code>
 * </pre>
 * <p>
 * If you're unsure what to use, try the
 * {@link org.codeturnery.lucene.access.ReadToolbox}/{@link org.codeturnery.lucene.access.WriteToolbox}
 * first and move to the
 * {@link org.codeturnery.lucene.access.ReadExecuter}/{@link org.codeturnery.lucene.access.WriteExecuter}
 * if their methods don't suffice.
 */
@org.eclipse.jdt.annotation.NonNullByDefault
package org.codeturnery.lucene.access;
