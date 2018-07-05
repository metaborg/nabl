package mb.nabl2.spoofax.primitives;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.metaborg.util.task.NullCancel;
import org.metaborg.util.task.NullProgress;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.stratego.SDefT;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;

import mb.nabl2.config.NaBL2DebugConfig;
import mb.nabl2.solver.solvers.CallExternal;
import mb.nabl2.solver.solvers.SemiIncrementalMultiFileSolver;
import mb.nabl2.stratego.ConstraintTerms;
import mb.nabl2.stratego.StrategoTerms;
import mb.nabl2.terms.ITerm;

public abstract class ScopeGraphMultiFileAnalysisPrimitive extends AbstractPrimitive {

    private static ILogger logger = LoggerUtils.logger(ScopeGraphMultiFileAnalysisPrimitive.class);

    public ScopeGraphMultiFileAnalysisPrimitive(String name, int tvars) {
        super(name, 0, tvars);
    }

    @Override public boolean call(IContext env, Strategy[] svars, IStrategoTerm[] tvars) throws InterpreterException {
        final StrategoTerms strategoTerms = new StrategoTerms(env.getFactory());

        final List<IStrategoTerm> argSTerms = Arrays.asList(tvars);
        final List<ITerm> argTerms = argSTerms.stream()
                .map(t -> ConstraintTerms.specialize(strategoTerms.fromStratego(t))).collect(Collectors.toList());

        final IStrategoTerm currentSTerm = env.current();
        final ITerm currentTerm = ConstraintTerms.specialize(strategoTerms.fromStratego(currentSTerm));

        final ICancel cancel = new NullCancel();
        final IProgress progress = new NullProgress();

        NaBL2DebugConfig debugConfig = NaBL2DebugConfig.NONE; // FIXME How to get the debug level?
        final SemiIncrementalMultiFileSolver solver =
                new SemiIncrementalMultiFileSolver(debugConfig, callExternal(env, strategoTerms));

        return call(currentTerm, argTerms, solver, cancel, progress).map(result -> {
            final IStrategoTerm resultTerm = strategoTerms.toStratego(ConstraintTerms.explicate(result));
            env.setCurrent(resultTerm);
            return true;
        }).orElse(false);
    }

    protected abstract Optional<? extends ITerm> call(ITerm currentTerm, List<ITerm> argTerms,
            SemiIncrementalMultiFileSolver solver, ICancel cancel, IProgress progress) throws InterpreterException;

    private static CallExternal callExternal(IContext env, StrategoTerms strategoTerms) {
        return (name, args) -> {
            final IStrategoTerm[] sargs = Iterables2.stream(args).map(strategoTerms::toStratego)
                    .collect(Collectors.toList()).toArray(new IStrategoTerm[0]);
            final IStrategoTerm sarg = sargs.length == 1 ? sargs[0] : env.getFactory().makeTuple(sargs);
            final IStrategoTerm prev = env.current();
            try {
                env.setCurrent(sarg);
                final SDefT s = env.lookupSVar(name.replace("-", "_") + "_0_0");
                if(!s.evaluate(env)) {
                    return Optional.empty();
                }
                return Optional.ofNullable(env.current()).map(strategoTerms::fromStratego)
                        .map(ConstraintTerms::specialize);
            } catch(Exception ex) {
                logger.warn("External call to '{}' failed.", ex, name);
                return Optional.empty();
            } finally {
                env.setCurrent(prev);
            }
        };

    }

}