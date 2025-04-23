package org.codeturnery.lucene.document;

import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;

public class CommonFieldTypes {

	/**
	 * @see StringField#TYPE_STORED
	 */
	public static final FieldType STORED_STRING = StringField.TYPE_STORED;
	/**
	 * @see StringField#TYPE_NOT_STORED
	 */
	public static final FieldType NOT_STORED_STRING = StringField.TYPE_NOT_STORED;
	/**
	 * Stored value. No normalization. Tokenized via the configured analyzer.
	 * Indexing of term-frequencies, i.e. term-frequency scoring is supported.
	 * Indexing of positions, i.e. phrase queries are supported.
	 */
	public static final FieldType TEXT = createType(true, IndexOptions.DOCS_AND_FREQS_AND_POSITIONS, true, true);
	/**
	 * Stored value. No normalization. No tokenization. No indexing of
	 * term-frequencies, i.e. no term-frequency scoring either. No indexing of
	 * positions, i.e. no phrase queries are supported.
	 */
	public static final FieldType TERM_EXACT_MATCHING = createType(true, IndexOptions.DOCS, true, false);

	/**
	 * Value not stored. Value normalized. Value tokenized. Value not indexed.
	 */
	public static final FieldType TERM_FUZZY_MATCHING = createType(false, IndexOptions.NONE, false, true);

	private static FieldType createType(boolean omitNorms, IndexOptions indexOptions, boolean stored,
			boolean tokenized) {
		final var fieldType = new FieldType();
		fieldType.setOmitNorms(omitNorms);
		fieldType.setIndexOptions(indexOptions);
		fieldType.setStored(stored);
		fieldType.setTokenized(tokenized);
		fieldType.setDocValuesType(DocValuesType.NONE);
		// TERM_FUZZY_MATCHING.setDimensions(0, 0, 0);
		// TERM_FUZZY_MATCHING.setStoreTermVector*
		fieldType.freeze();
		return fieldType;
	}
}
