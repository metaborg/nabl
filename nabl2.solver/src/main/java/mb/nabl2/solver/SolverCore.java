package mb.nabl2.solver;

import org.metaborg.util.Ref;
import org.metaborg.util.functions.Function1;

import mb.nabl2.solver.solvers.CallExternal;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.u.IUnifier;

public class SolverCore {

    public final SolverConfig config;
    public final Ref<? extends IUnifier> unifier;
    public final Function1<String, String> fresh;
    public final CallExternal callExternal;

    public SolverCore(SolverConfig config, Ref<? extends IUnifier> unifier, Function1<String, String> fresh) {
        this(config, unifier, fresh, CallExternal.never());
    }

    public SolverCore(SolverConfig config, Ref<? extends IUnifier> unifier, Function1<String, String> fresh,
            CallExternal callExternal) {
        this.config = config;
        this.fresh = fresh;
        this.unifier = unifier;
        this.callExternal = callExternal;
    }

}