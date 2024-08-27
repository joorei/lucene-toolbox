package org.codeturnery.lucene.query;

import java.util.List;
import java.util.function.Function;
import org.apache.lucene.index.Term;
import org.eclipse.jdt.annotation.Nullable;

@SuppressWarnings({ "javadoc" })
abstract public class DemotionLoader<T> implements Function<T, List<DemotedTerm>> {
	private final String fieldName;
	/**
	 * The value to be interpreted as one multiplier, meaning the corresponding term
	 * should neither be boosted nor demoted.
	 */
	private final int one;
	/**
	 * The minimum allowed value in the input.
	 */
	private final int min;
	/**
	 * The maximum allowed value in the input.
	 */
	private final int max;

	/**
	 * @param fieldName The field to limit the term demotions to. Eg. if a term
	 *                  <code>football</code> is demoted and <code>sports</code> is
	 *                  given as field name, then the term <code>football</code>
	 *                  will be scored normally in the field <code>balls</code>.
	 * @param center
	 * @param range
	 */
	public DemotionLoader(final String fieldName, final int center, final int range) {
		this.fieldName = fieldName;
		this.one = center;
		this.min = center - range;
		this.max = center + range;
	}

	/**
	 * @param key
	 * @param preference
	 * @return Will be <code>null</code> if the given value would result in a
	 *         multiplier of <code>1</code>.
	 */
	public @Nullable DemotedTerm createDemotedTerm(final String key, final int preference) {
		if (this.one == preference) {
			// would result in no boosting anyway
			return null;
		}

		return createDemotedTerm(key, (float) preference);
	}

	public @Nullable DemotedTerm createDemotedTerm(final String key, final float preference, final float deviation) {
		if (this.one < preference + deviation && this.one > preference - deviation) {
			// would result in no boosting anyway
			return null;
		}

		return createDemotedTerm(key, preference);
	}

	private @Nullable DemotedTerm createDemotedTerm(final String key, final float preference) {
		if (preference > this.max || preference < this.min) {
			throw new IllegalArgumentException();
		}

		final float multiplier = convert(preference);
		final var term = new Term(this.fieldName, key);
		return new DemotedTerm(term, multiplier);
	}

	/**
	 * @param value
	 * @return
	 */
	private float convert(final float value) {
		return convert(this.min, this.max, 0, Float.MAX_VALUE, value);
	}

	/**
	 * @param oldMin
	 * @param oldMax
	 * @param newMin
	 * @param newMax
	 * @param value
	 * @return
	 */
	private static float convert(final float oldMin, final float oldMax, final float newMin, final float newMax,
			final float value) {
		final float oldRange = oldMax - oldMin;
		final float newRange = newMax - newMin;
		return (((value - oldMin) * newRange) / oldRange) + newMin;
	}
}
