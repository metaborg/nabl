package mb.nabl2.spoofax.primitives;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.log.PrintlineLogger;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.metaborg.util.task.NullCancel;
import org.metaborg.util.task.NullProgress;
import org.metaborg.util.task.ThreadCancel;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.stratego.SDefT;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.util.TermUtils;

import mb.nabl2.config.NaBL2DebugConfig;
import mb.nabl2.solver.solvers.CallExternal;
import mb.nabl2.solver.solvers.SemiIncrementalMultiFileSolver;
import mb.nabl2.spoofax.primitives.StrategyCalls.CallableStrategy;
import mb.nabl2.stratego.ConstraintTerms;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.StrategoBlob;
import mb.nabl2.terms.stratego.StrategoTerms;

public abstract class ScopeGraphMultiFileAnalysisPrimitive extends AbstractPrimitive {

    private static ILogger logger = LoggerUtils.logger(ScopeGraphMultiFileAnalysisPrimitive.class);
    private static final PrintlineLogger celog = PrintlineLogger.logger(CallExternal.class);


    public ScopeGraphMultiFileAnalysisPrimitive(String name, int tvars) {
        super(name, 0, tvars);
    }

    @Override public boolean call(IContext env, Strategy[] svars, IStrategoTerm[] tvars) throws InterpreterException {
        final StrategoTerms strategoTerms = new StrategoTerms(env.getFactory());

        final List<IStrategoTerm> argSTerms = Arrays.asList(tvars);
        final List<ITerm> argTerms = argSTerms.stream()
                .map(t -> ConstraintTerms.specialize(strategoTerms.fromStratego(t))).collect(Collectors.toList());

        final IStrategoTerm currentSTerm = ScopeGraphMultiFileAnalysisPrimitive.getActualCurrent(env.current());
        final ITerm currentTerm = ConstraintTerms.specialize(strategoTerms.fromStratego(currentSTerm));

        final ICancel cancel = ScopeGraphMultiFileAnalysisPrimitive.getCancel(env.current());
        final IProgress progress = ScopeGraphMultiFileAnalysisPrimitive.getProgress(env.current());

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

    static CallExternal callExternal(IContext env, StrategoTerms strategoTerms) {
        final HashMap<String, SDefT> strCache = new HashMap<>();
        return (name, args) -> {
            celog.debug("calling external {}({})", name, args);
            final IStrategoTerm arg = prepareArguments(args, strategoTerms, env.getFactory());
            try {
                final CallableStrategy strategy = StrategyCalls.lookup(env, name, strCache);
                final Optional<IStrategoTerm> result = strategy.call(arg);
                final Optional<ITerm> resultTerm = result.map(strategoTerms::fromStratego).map(ConstraintTerms::specialize);
                celog.debug("* result: {}", resultTerm);
                return resultTerm;
            } catch(Exception ex) {
                logger.warn("External call to '{}' failed.", ex, name);
                return Optional.empty();
            }
        };

    }

    private static IStrategoTerm prepareArguments(Collection<? extends ITerm> args, StrategoTerms strategoTerms,
            ITermFactory factory) {
        if(args.size() == 1) {
            return strategoTerms.toStratego(args.iterator().next());
        }
        final IStrategoTerm[] argTerms;
        {
            argTerms = new IStrategoTerm[args.size()];
            int i = 0;
            for(ITerm arg : args) {
                argTerms[i] = strategoTerms.toStratego(arg);
                i++;
            }
        }
        return factory.makeTuple(argTerms);
    }

    // the methods below support wrapping the current term in a special constructor to pass cancellation tokens
    // these cannot be passed as term arguments, since older versions of NaBL2 are used, and crash if we change the interface

    private static final String WITH_CANCEL_PROGRESS_OP = "WithCancelProgress";

    static IStrategoTerm getActualCurrent(IStrategoTerm current) throws InterpreterException {
        if(TermUtils.isAppl(current, WITH_CANCEL_PROGRESS_OP, 3)) {
            return current.getSubterm(0);
        } else {
            return current;
        }
    }

    static IProgress getProgress(IStrategoTerm current) throws InterpreterException {
        if(TermUtils.isAppl(current, WITH_CANCEL_PROGRESS_OP, 3)) {
            final IStrategoTerm progressTerm = current.getSubterm(2);
            if(TermUtils.isTuple(progressTerm, 0)) {
                return new NullProgress();
            } else {
                return StrategoBlob.match(progressTerm, IProgress.class)
                        .orElseThrow(() -> new InterpreterException("Expected progress."));
            }
        } else {
            return new NullProgress();
        }
    }

    static ICancel getCancel(IStrategoTerm current) throws InterpreterException {
        if(TermUtils.isAppl(current, WITH_CANCEL_PROGRESS_OP, 3)) {
            final IStrategoTerm cancelTerm = current.getSubterm(1);
            if(TermUtils.isTuple(cancelTerm, 0)) {
                return new NullCancel();
            } else {
                return StrategoBlob.match(cancelTerm, ICancel.class)
                        .orElseThrow(() -> new InterpreterException("Expected cancel."));
            }
        } else {
            return new ThreadCancel();
        }
    }

}
