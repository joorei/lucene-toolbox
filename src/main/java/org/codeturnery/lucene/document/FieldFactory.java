package org.codeturnery.lucene.document;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;

import org.apache.lucene.document.BinaryDocValuesField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexableFieldType;
import org.apache.lucene.util.BytesRef;
import org.codeturnery.bytes.BytesUtil;
import org.eclipse.jdt.annotation.Checks;

/**
 * TODO: is {@link StringField} and {@link BinaryDocValuesField} not needed? do
 * they not have any advantages or use-cases?
 */
@SuppressWarnings("static-method")
public class FieldFactory {
	/**
	 * Returns a filled {@link StoredField} instance.
	 * <p>
	 * The specified number of bytes will be read from the given
	 * {@link InputStream}. It will be closed when this method returns.
	 * <p>
	 * The bytes are read into {@code byte} array and passed to {@link StoredField}.
	 * 
	 * @param field
	 * 
	 * @param inputStream     Will be closed by this method after reading.
	 * @param inputStreamSize
	 * @return
	 * @throws IOException
	 */
	public StoredField createStoredBytes(final String field, final InputStream inputStream, final int inputStreamSize)
			throws IOException {
		try (inputStream) {
			final var bytes = new byte[inputStreamSize];
			final int count = BytesUtil.readInto(inputStream, bytes, 0, inputStreamSize, null);
			if (count != inputStreamSize) {
				throw new IllegalStateException("Expected size (" + inputStreamSize + ") does not equal actually read bytes (" + count + ").");
			}
			return new StoredField(field, bytes, 0, inputStreamSize);
		}
	}

	/**
	 * Returns a {@link StoredField} instance, filled with the bytes between the
	 * given offset and length of the given bytes array.
	 * <p>
	 * Internally, the {@link StoredField} will wrap the given bytes in a
	 * {@link BytesRef} instance. Hence, this method has no advantages over
	 * {@link FieldFactory#createStoredBytes(String, BytesRef)}.
	 * 
	 * @param field
	 * 
	 * @param bytes  Changes made to the arrays content <strong>will</strong> be
	 *               reflected in the written {@link Document} field until indexing
	 *               has been completed.
	 * @param offset
	 * @param length
	 * @return
	 */
	public StoredField createStoredBytes(String field, byte[] bytes, int offset, int length) {
		return new StoredField(field, bytes, offset, length);
	}

	/**
	 * Will use the given {@link BytesRef} to create a {@link StoredField}.
	 *
	 * @param field
	 * @param bytes
	 * @return
	 */
	public StoredField createStoredBytes(String field, BytesRef bytes) {
		return new StoredField(field, bytes);
	}

	/**
	 * Will use the given bytes to create a {@link StoredField}.
	 * <p>
	 * Internally, the {@link StoredField} will wrap the given bytes in a
	 * {@link BytesRef} instance. Hence, this method has no advantages over
	 * {@link FieldFactory#createStoredBytes(String, BytesRef)}.
	 * 
	 * @param name
	 * @param array
	 * @return
	 */
	public StoredField createStoredBytes(final String name, final byte[] array) {
		return new StoredField(name, array);
	}

	/**
	 * Returns a filled {@link StoredField}.
	 * 
	 * @param field
	 * @param value
	 * @return
	 */
	public StoredField createStoredInt(String field, int value) {
		return new StoredField(field, value);
	}

	/**
	 * @param field
	 * @param value
	 * @return
	 */
	public IntPoint createIndexedInt(String field, int value) {
		return new IntPoint(field, value);
	}

	/**
	 * @param field
	 * @param value
	 * @return
	 */
	public StoredField createStoredLong(String field, long value) {
		return new StoredField(field, value);
	}

	/**
	 * @param field
	 * @param value
	 * @return
	 */
	public LongPoint createIndexedLong(String field, long value) {
		return new LongPoint(field, value);
	}

	/**
	 * @param field
	 * @param value
	 * @return
	 * @deprecated use {@link #createIndexedAndStoredBoolean} instead
	 */
	@Deprecated
	public Field createString(String field, boolean value) {
		return createIndexedAndStoredBoolean(field, value);
	}

	/**
	 * Uses {@link #createStoredString} by storing {@code "1"} for {@code true} and
	 * {@code 0} for {@code false}.
	 * 
	 * @param field
	 * @param value
	 * @return
	 */
	public Field createIndexedAndStoredBoolean(final String field, final boolean value) {
		return createStoredString(field, value ? "1" : "0");
	}

	/**
	 * Creates at least a {@link Field} instance from the given {@link CharSequence
	 * value} and {@link IndexableFieldType fieldType}.
	 * <p>
	 * If <code>taxomize</code> is set to true, the return will additionally contain
	 * a {@link FacetField} filled with the given <code>value</code>.
	 * 
	 * @param field
	 * @param value
	 * 
	 * @param fieldType Describes how the value should be stored. You may refer to
	 *                  predefined instances like those in {@link CommonFieldTypes}.
	 * @param taxomize
	 * @return
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
	 * @param name
	 * @param value
	 * @return
	 */
	public StoredField createStoredField(final String name, final String value) {
		return new StoredField(name, value);
	}

	/**
	 * @param fieldName
	 * @param values
	 * @param taxomize
	 * @return
	 */
	public IndexableField[] createExactMatchingTerms(String fieldName, Set<? extends CharSequence> values,
			boolean taxomize) {
		return createStrings(fieldName, values, CommonFieldTypes.TERM_EXACT_MATCHING, taxomize);
	}

	/**
	 * @param fieldName
	 * @param values
	 * @param taxomize
	 * @return
	 */
	public IndexableField[] createExactMatchingTerm(String fieldName, CharSequence values, boolean taxomize) {
		return createString(fieldName, values, CommonFieldTypes.TERM_EXACT_MATCHING, taxomize);
	}

	/**
	 * Creates a {@link Field} instance with the given {@link IndexableFieldType
	 * fieldType}, containing the given <code>value</code>.
	 * 
	 * @param field
	 * @param value
	 * @param fieldType
	 * @return
	 */
	public Field createString(String field, CharSequence value, IndexableFieldType fieldType) {
		return new Field(field, value, fieldType);
	}

	/**
	 * Calls {@link #createString(String, CharSequence, IndexableFieldType)} with
	 * {@link CommonFieldTypes#STORED_STRING}.
	 * 
	 * @param field
	 * @param value
	 * @return
	 */
	public Field createStoredString(String field, CharSequence value) {
		return createString(field, value, CommonFieldTypes.STORED_STRING);
	}

	/**
	 * Creates a {@link FacetField} instance, containing the given
	 * <code>value</code>.
	 * 
	 * @param field
	 * 
	 * @param value must not be empty
	 * @return
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
	 * these fields are created in pairs, with the second value of each pair being a
	 * {@link FacetField} created via {@link #createTaxonomyString(String, String)}.
	 * 
	 * @param field
	 * @param values
	 * @param fieldType
	 * @param taxomize
	 * @return
	 */
	public IndexableField[] createStrings(final String field, final Collection<? extends CharSequence> values,
			final IndexableFieldType fieldType, final boolean taxomize) {
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

	/**
	 * TODO: indexing and storing of time may be optimizable
	 * 
	 * @param field
	 * @param value
	 * @return
	 */
	protected IndexableField[] indexAndStoreInstant(final String field, final Instant value) {
		return createString(field, value.toString(), CommonFieldTypes.STORED_STRING, false);
		// Add the last modified date as field named "modified".
		// Use a LongPoint that is indexed (i.e. efficiently filterable with
		// PointRangeQuery). Milli-second resolution is often too fine. You could
		// instead create a number based on
		// year/month/day/hour/minutes/seconds, down the resolution you require.
		// For example the long value 2011021714 would mean
		// February 17, 2011, 2-3 PM.
		// return new LongPoint(field, value.getEpochSecond());
	}
}
