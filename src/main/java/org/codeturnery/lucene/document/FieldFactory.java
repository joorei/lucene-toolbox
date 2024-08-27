package org.codeturnery.lucene.document;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexableFieldType;
import org.apache.lucene.util.BytesRef;
import org.eclipse.jdt.annotation.Checks;

@SuppressWarnings("static-method")
public class FieldFactory {

	// TODO: use inputStream.readAllBytes() instead? What about BufferedInputStream,
	// is it not necessary here?

	/**
	 * Returns a filled {@link StoredField} instance.
	 * <p>
	 * The given {@link InputStream} will be closed when this method returns.
	 */
	public StoredField createStoredBytes(String field, InputStream inputStream, int inputStreamSize)
			throws IOException {
		try (inputStream; final DataInputStream dis = new DataInputStream(inputStream);) {
			final byte[] bytes = new byte[inputStreamSize];
			dis.readFully(bytes);
			return new StoredField(field, new BytesRef(bytes, 0, inputStreamSize));
		}
	}

	/**
	 * Returns a {@link StoredField} instance, filled with the bytes between the
	 * given offset and length of the given bytes array.
	 */
	public StoredField createStoredBytes(String field, byte[] bytes, int offset, int length) {
		return new StoredField(field, new BytesRef(bytes, offset, length));
	}

	/**
	 * Returns a filled {@link StoredField}.
	 */
	public StoredField createStoredInt(String field, int value) {
		return new StoredField(field, value);
	}

	public IntPoint createIndexedInt(String field, int value) {
		return new IntPoint(field, value);
	}

	public StoredField createStoredLong(String field, long value) {
		return new StoredField(field, value);
	}

	public LongPoint createIndexedLong(String field, long value) {
		return new LongPoint(field, value);
	}

	/**
	 * Creates {@link StringField#TYPE_STORED stored} {@link Field} instance
	 * containing the string <code>1</code> if the given boolean is true and the
	 * string <code>0</code> otherwise.
	 */
	public Field createString(String field, boolean value) {
		return new Field(field, value ? "1" : "0", StringField.TYPE_STORED);
	}

	/**
	 * Creates at least a {@link Field} instance from the given {@link CharSequence
	 * value} and {@link IndexableFieldType fieldType}.
	 * <p>
	 * If <code>taxomize</code> is set to true, the return will additionally contain
	 * a {@link FacetField} filled with the given <code>value</code>.
	 */
	public IndexableField[] createString(String field, CharSequence value, IndexableFieldType fieldType,
			boolean taxomize) {
		final IndexableField[] fields = new IndexableField[taxomize ? 2 : 1];
		fields[0] = createString(field, value, fieldType);

		if (taxomize) {
			fields[1] = createTaxonomyString(field, value.toString());
		}

		return fields;
	}

	/**
	 * Creates a {@link Field} instance with the given {@link IndexableFieldType
	 * fieldType}, containing the given <code>value</code>.
	 */
	public Field createString(String field, CharSequence value, IndexableFieldType fieldType) {
		return new Field(field, value, fieldType);
	}

	/**
	 * Creates a {@link StringField#TYPE_STORED stored} {@link Field} instance,
	 * containing the given <code>value</code>.
	 */
	public Field createStoredString(String field, CharSequence value) {
		return new Field(field, value, StringField.TYPE_STORED);
	}

	/**
	 * Creates a {@link FacetField} instance, containing the given
	 * <code>value</code>.
	 * 
	 * @param value must not be empty
	 */
	public FacetField createTaxonomyString(String field, String value) {
		final String stringValue = Checks.requireNonEmpty(value);
		return new FacetField(field, stringValue);
	}

	/**
	 * A variant of {@link #createString} that can be applied to multiple input
	 * values.
	 * 
	 * For each item in <code>values</code> a {@link Field} is created via
	 * {@link #createString}. If <code>taxomize</code> is set to <code>true</code>,
	 * these fields are created in pairs, with the second one being a
	 * {@link FacetField} created via {@link #createTaxonomyString(String, String)}.
	 */
	public IndexableField[] createStrings(String field, Collection<CharSequence> values, IndexableFieldType fieldType,
			boolean taxomize) {
		final IndexableField[] fields = new IndexableField[values.size() * (taxomize ? 2 : 1)];
		int i = 0;
		for (final CharSequence value : values) {
			fields[i++] = createString(field, value, fieldType);
			if (taxomize) {
				fields[i++] = createTaxonomyString(field, value.toString());
			}
		}

		return fields;
	}

	// TODO: indexing and storing of time may be optimizable
//	protected void indexAndStoreInstant(final String field, final Instant value) {
//		return createString(field, value.toString(), StringField.TYPE_STORED, false);
//		// Add the last modified date as field named "modified".
//		// Use a LongPoint that is indexed (i.e. efficiently filterable with
//		// PointRangeQuery). Milli-second resolution is often too fine. You could
//		// instead create a number based on
//		// year/month/day/hour/minutes/seconds, down the resolution you require.
//		// For example the long value 2011021714 would mean
//		// February 17, 2011, 2-3 PM.
//		return new LongPoint(field, value.getEpochSecond());
//	}
}
