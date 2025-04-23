package org.codeturnery.lucene.document;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

/**
 * Helps to fill a {@link Document} to index it into Lucene.
 */
public class DocumentBuilder {
	protected final Document document;

	public DocumentBuilder() {
		this.document = new Document();
	}

	/**
	 * Add all fields in the given array to the {@link #document backing document}.
	 * <p>
	 * You may create fields using {@link FieldFactory} or other means.
	 * 
	 * @param fields
	 */
	public void addAll(final IndexableField[] fields) {
		for (int i = 0; i < fields.length; i++) {
			add(fields[i]);
		}
	}

	public void addAll(final Iterable<IndexableField> fields) {
		for (final IndexableField field : fields) {
			add(field);
		}
	}

	/**
	 * Use this method to get all field names currently in use in the build
	 * document.
	 * <p>
	 * You may create new fields from the result and add them back to this builder,
	 * to support "missing fields" searches.
	 * <p>
	 * Collecting all field names from the document before adding fields is
	 * necessary, as the fields in the document can not be changed while iterating
	 * over them.
	 * 
	 * @return The current field names in the build document without duplicates.
	 *         Decoupled from the document, i.e. will not change when the document
	 *         changes.
	 */
	public Set<String> getFieldNamesInUse() {
		final List<IndexableField> fieldsInUse = this.document.getFields();
		final Set<String> fieldNamesInUse = new HashSet<>(fieldsInUse.size());
		for (final IndexableField fieldInUse : fieldsInUse) {
			fieldNamesInUse.add(fieldInUse.name());
		}
		return fieldNamesInUse;
	}

	/**
	 * Add the given field to the {@link #document backing document}.
	 * <p>
	 * You may create fields using {@link FieldFactory} or other means.
	 * 
	 * @param field
	 */
	public void add(final IndexableField field) {
		this.document.add(field);
	}

	public Document getDocument() {
		return this.document;
	}
}
