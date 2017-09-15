package org.metaborg.meta.nabl2.solver;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.PartialFunction2;

public class SolverCore {

    public final SolverConfig config;
    public final Function1<ITerm, ITerm> find;
    public final Function1<String, ITermVar> fresh;
    public final PartialFunction2<String, Iterable<? extends ITerm>, ITerm> callExternal;

    public SolverCore(SolverConfig config, Function1<ITerm, ITerm> find, Function1<String, ITermVar> fresh) {
        this(config, find, fresh, PartialFunction2.never());
    }

    public SolverCore(SolverConfig config, Function1<ITerm, ITerm> find, Function1<String, ITermVar> fresh,
            PartialFunction2<String, Iterable<? extends ITerm>, ITerm> callExternal) {
        this.config = config;
        this.fresh = fresh;
        this.find = find;
        this.callExternal = callExternal;
    }

}