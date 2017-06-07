package org.metaborg.meta.nabl2.solver;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.util.functions.Function1;

public class SolverCore {

    public final SolverConfig config;
    public final Function1<ITerm, ITerm> find;
    public final Function1<String, ITermVar> fresh;

    public SolverCore(SolverConfig config, Function1<ITerm, ITerm> find, Function1<String, ITermVar> fresh) {
        this.config = config;
        this.fresh = fresh;
        this.find = find;
    }

}