package org.metaborg.meta.nabl2.solver;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.unification.IUnifier;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.PartialFunction2;

public class SolverCore {

    public final SolverConfig config;
    public final IUnifier unifier;
    public final Function1<String, String> fresh;
    public final PartialFunction2<String, Iterable<? extends ITerm>, ITerm> callExternal;

    public SolverCore(SolverConfig config, IUnifier unifier, Function1<String, String> fresh) {
        this(config, unifier, fresh, PartialFunction2.never());
    }

    public SolverCore(SolverConfig config, IUnifier unifier, Function1<String, String> fresh,
            PartialFunction2<String, Iterable<? extends ITerm>, ITerm> callExternal) {
        this.config = config;
        this.fresh = fresh;
        this.unifier = unifier;
        this.callExternal = callExternal;
    }

}