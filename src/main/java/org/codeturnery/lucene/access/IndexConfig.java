package org.codeturnery.lucene.access;

import java.nio.file.Path;

import org.apache.lucene.facet.FacetsConfig;

public interface IndexConfig {

	Path getIndexPath();

	Path getTaxonomyPath();

	double getRamBufferSizeMb();

	FacetsConfig getFacetsContig();
}
