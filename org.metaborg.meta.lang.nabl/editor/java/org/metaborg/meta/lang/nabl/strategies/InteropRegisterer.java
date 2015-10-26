package org.metaborg.meta.lang.nabl.strategies;

import org.strategoxt.lang.JavaInteropRegisterer;

public class InteropRegisterer extends JavaInteropRegisterer {
	public InteropRegisterer() {
		super(new LibraryInitializer());
	}
}
