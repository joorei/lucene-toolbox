package org.codeturnery.lucene.access;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

abstract public class PropertyBasedIndexConfiguration implements IndexConfig {

	protected final Properties properties;

	public PropertyBasedIndexConfiguration(final String configFile)
			throws InvalidPropertiesFormatException, IOException {
		this.properties = new Properties();
		try (final var fis = getClass().getClassLoader().getResourceAsStream(configFile);) {
			this.properties.loadFromXML(fis);
		}
	}

	@Override
	public Path getIndexPath() {
		return Paths.get(this.properties.getProperty("indexPath"));
	}

	@Override
	public Path getTaxonomyPath() {
		return Paths.get(this.properties.getProperty("taxonomyPath"));
	}

	@Override
	public double getRamBufferSizeMb() {
		return Double.parseDouble(this.properties.getProperty("ramBufferSizeMb"));
	}
}
