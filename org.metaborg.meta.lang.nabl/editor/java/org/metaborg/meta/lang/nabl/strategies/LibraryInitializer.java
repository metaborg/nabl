package org.metaborg.meta.lang.nabl.strategies;

import java.util.Collections;
import java.util.List;

import org.strategoxt.lang.Context;
import org.strategoxt.lang.RegisteringStrategy;

public class LibraryInitializer extends org.strategoxt.lang.LibraryInitializer {

	@Override
	protected List<RegisteringStrategy> getLibraryStrategies() {
		return Collections.emptyList();
	}

	@Override
	protected void initializeLibrary(Context context) {
	}

}
