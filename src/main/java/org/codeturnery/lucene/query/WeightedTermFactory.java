package org.codeturnery.lucene.query;

import org.apache.lucene.index.Term;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Creates {@link WeightedTerm} instances from different ranges than {@code 0}
 * to {@code 1} for demotions and greater than {@code 1} for boosts.
 * <p>
 * The weighting always is applied to a term and a corresponding field. E.g. if
 * a term <code>football</code> is demoted and <code>sports</code> is given as
 * field name, then the term <code>football</code> will be scored normally if it
 * appears in the field <code>balls</code>.
 */
public class WeightedTermFactory {
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
	 * @param center The value that corresponds to an unweighted term (i.e.
	 *               corresponding to a multiplier of {@code 1} in the
	 *               {@link WeightedTerm} instance).
	 * @param range  Determines the valid minimum and maximum when creating
	 *               {@link WeightedTerm}s with this instance.
	 */
	public WeightedTermFactory(final int center, final int range) {
		this.one = center;
		this.min = center - range;
		this.max = center + range;
	}

	/**
	 * @param term
	 * @param weight
	 * @return Will be <code>null</code> if the given value would result in a
	 *         multiplier of <code>1</code>.
	 */
	public @Nullable WeightedTerm createWeightedTerm(final Term term, final int weight) {
		if (this.one == weight) {
			// would result in no boosting anyway
			return null;
		}

		return createWeightedTerm(term, (float) weight);
	}

	public @Nullable WeightedTerm createWeightedTerm(final String field, final String term, final int weight) {
		return createWeightedTerm(new Term(field, term), weight);
	}

	public @Nullable WeightedTerm createWeightedTerm(final String field, final String term, final float weight,
			final float deviation) {
		return createWeightedTerm(new Term(field, term), weight, deviation);
	}

	public @Nullable WeightedTerm createWeightedTerm(final Term term, final float weight, final float deviation) {
		if (this.one < weight + deviation && this.one > weight - deviation) {
			// would result in no boosting anyway
			return null;
		}

		return createWeightedTerm(term, weight);
	}

	protected @Nullable WeightedTerm createWeightedTerm(final Term term, final float weigth) {
		if (weigth > this.max || weigth < this.min) {
			throw new IllegalArgumentException();
		}

		final float multiplier = convert(weigth);
		return new WeightedTerm(term, multiplier);
	}

	/**
	 * @param value
	 * @return
	 */
	protected float convert(final float value) {
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
	protected float convert(final float oldMin, final float oldMax, final float newMin, final float newMax,
			final float value) {
		final float oldRange = oldMax - oldMin;
		final float newRange = newMax - newMin;
		return (((value - oldMin) * newRange) / oldRange) + newMin;
	}
}
