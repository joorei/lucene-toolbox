package org.codeturnery.lucene.query;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Loads a {@link Properties} file from an input path and converts it into
 * {@link WeightedTerm}s.
 * <p>
 * The key in each entry is expected to be the term while each value is expected
 * to be an integer in a specific range.
 */
@SuppressWarnings({ "javadoc", "null" })
public class IntWeightLoader {
	private final WeightedTermFactory weightedTermFactory;
	private final String fieldName;

	public IntWeightLoader(final String fieldName, final WeightedTermFactory weightedTermFactory) {
		this.weightedTermFactory = weightedTermFactory;
		this.fieldName = fieldName;
	}

	public List<WeightedTerm> loadWeightedTermsFromFile(final String path) throws FileNotFoundException, IOException {
		try (final Reader reader = new FileReader(Paths.get(path).toFile());) {
			final Properties properties = new Properties();
			properties.load(reader);
			return getWeightedTerms(properties);
		}
	}

	public List<WeightedTerm> getWeightedTerms(final Properties properties) {
		final Set<Entry<Object, Object>> propertiesSet = properties.entrySet();
		/*
		 * As we may filter some out we do not know how many values will actually result
		 * from the properties file, but we know it will never be more than the input
		 * count. Thus we don't need to create arrays bigger than that count.
		 */
		final var terms = new ArrayList<WeightedTerm>(Math.min(16, properties.size()));
		for (final Map.Entry<Object, Object> entry : propertiesSet) {
			final int weight = Integer.parseInt((String) entry.getValue());
			final String term = (String) entry.getKey();
			final WeightedTerm demotedTerm = this.weightedTermFactory.createWeightedTerm(this.fieldName, term, weight);
			if (null != demotedTerm) {
				terms.add(demotedTerm);
			}
		}
		return terms;
	}
}
