package mb.nabl2.solver;

import org.metaborg.util.Ref;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

import mb.nabl2.solver.solvers.CallExternal;
import mb.nabl2.terms.unification.u.IUnifier;

public class SolverCore {

    public final SolverConfig config;
    public final Ref<? extends IUnifier> unifier;
    public final Function1<String, String> fresh;
    public final CallExternal callExternal;
    public final ICancel cancel;
    public final IProgress progress;

    public SolverCore(SolverConfig config, Ref<? extends IUnifier> unifier, Function1<String, String> fresh,
            ICancel cancel, IProgress progress) {
        this(config, unifier, fresh, CallExternal.never(), cancel, progress);
    }

    public SolverCore(SolverConfig config, Ref<? extends IUnifier> unifier, Function1<String, String> fresh,
            CallExternal callExternal, ICancel cancel, IProgress progress) {
        this.config = config;
        this.fresh = fresh;
        this.unifier = unifier;
        this.callExternal = callExternal;
        this.cancel = cancel;
        this.progress = progress;
    }

}