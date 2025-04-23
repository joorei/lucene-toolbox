package org.codeturnery.lucene.document;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexableFieldType;

/**
 * Helps to fill a {@link Document} to index it into Lucene.
 * <p>
 * You can extend this class with your own domain-specific class, implementing
 * public setters to add specific fields. Instances of that class can then be
 * passed into trusted third party-code, which is restricted to the public
 * setters and not allowed low-level access to the backing {@link Document}
 * directly.
 */
@SuppressWarnings({ "javadoc" })
public class AbstractDocumentBuilder {
	protected final Document document;
	protected final FieldFactory fieldFactory;

	protected AbstractDocumentBuilder(final FieldFactory fieldFactory) {
		this.document = new Document();
		this.fieldFactory = fieldFactory;
	}

	/**
	 * @param field
	 * @param inputStream     Will be closed by this method after reading.
	 * @param inputStreamSize
	 * @throws IOException
	 */
	protected void addStoredBytes(final String field, final InputStream inputStream, final int inputStreamSize)
			throws IOException {
		this.document.add(this.fieldFactory.createStoredBytes(field, inputStream, inputStreamSize));
	}

	/**
	 * 
	 * @param field
	 * @param bytes  Changes made to the arrays content <strong>will</strong> be
	 *               reflected in the written {@link Document} field until indexing
	 *               has been completed.
	 * @param offset
	 * @param length
	 */
	public void addStoredBytes(final String field, final byte[] bytes, int offset, int length) {
		this.document.add(this.fieldFactory.createStoredBytes(field, bytes, offset, length));
	}

	/**
	 * @param field
	 * @param value
	 */
	public void addStoredInt(final String field, final int value) {
		this.document.add(this.fieldFactory.createStoredInt(field, value));
	}

	/**
	 * @param field
	 * @param value
	 */
	public void addIndexedInt(final String field, final int value) {
		this.document.add(this.fieldFactory.createIndexedInt(field, value));
	}

	/**
	 * @param field
	 * @param value
	 */
	protected void addStoredLong(final String field, final long value) {
		this.document.add(this.fieldFactory.createStoredLong(field, value));
	}

	/**
	 * @param field
	 * @param value
	 */
	protected void addIndexedLong(final String field, final long value) {
		this.document.add(this.fieldFactory.createIndexedLong(field, value));
	}

	/**
	 * @param field
	 * @param value
	 */
	protected void addIndexedAndStoredBoolean(final String field, final boolean value) {
		this.document.add(this.fieldFactory.createString(field, value));
	}

	/**
	 * 
	 * @param field
	 * @param value
	 * @param fieldType Describes how the value should be stored. You may refer to
	 *                  predefined instances like {@link StringField#TYPE_STORED}.
	 * @param taxomize
	 */
	public void addString(final String field, final CharSequence value, final IndexableFieldType fieldType,
			final boolean taxomize) {
		final IndexableField[] fields = this.fieldFactory.createString(field, value, fieldType, taxomize);
		for (int i = 0; i < fields.length; i++) {
			this.document.add(fields[i]);
		}
	}

	/**
	 * @param field
	 * @param values
	 * @param fieldType
	 * @param taxomize
	 */
	protected void addStrings(final String field, final Collection<CharSequence> values,
			final IndexableFieldType fieldType, final boolean taxomize) {
		final IndexableField[] fields = this.fieldFactory.createStrings(field, values, fieldType, taxomize);
		for (int i = 0; i < fields.length; i++) {
			this.document.add(fields[i]);
		}
	}
}
