package org.metaborg.meta.nabl2.solver;

import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.unification.IUnifier;
import org.metaborg.meta.nabl2.util.functions.Function1;

public class SolverCore {

    public final SolverConfig config;
    public final IUnifier unifier;
    public final Function1<String, ITermVar> fresh;

    public SolverCore(SolverConfig config, IUnifier unifier, Function1<String, ITermVar> fresh) {
        this.config = config;
        this.fresh = fresh;
        this.unifier = unifier;
    }

}