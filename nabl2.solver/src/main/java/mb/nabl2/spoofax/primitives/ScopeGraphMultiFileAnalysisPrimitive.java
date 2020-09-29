package mb.nabl2.spoofax.primitives;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.stratego.SDefT;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

import mb.nabl2.config.NaBL2DebugConfig;
import mb.nabl2.solver.solvers.CallExternal;
import mb.nabl2.solver.solvers.SemiIncrementalMultiFileSolver;
import mb.nabl2.spoofax.primitives.StrategyCalls.CallableStrategy;
import mb.nabl2.stratego.ConstraintTerms;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.StrategoTerms;

public abstract class ScopeGraphMultiFileAnalysisPrimitive extends AbstractPrimitive {

    private static ILogger logger = LoggerUtils.logger(ScopeGraphMultiFileAnalysisPrimitive.class);

    public ScopeGraphMultiFileAnalysisPrimitive(String name, int tvars) {
        super(name, 0, tvars + 2);
    }

    @Override public boolean call(IContext env, Strategy[] svars, IStrategoTerm[] tvars) throws InterpreterException {
        final StrategoTerms strategoTerms = new StrategoTerms(env.getFactory());

        final List<IStrategoTerm> argSTerms = Arrays.asList(Arrays.copyOf(tvars, tvars.length - 2));
        final List<ITerm> argTerms = argSTerms.stream()
                .map(t -> ConstraintTerms.specialize(strategoTerms.fromStratego(t))).collect(Collectors.toList());

        final IStrategoTerm currentSTerm = env.current();
        final ITerm currentTerm = ConstraintTerms.specialize(strategoTerms.fromStratego(currentSTerm));

        final ICancel cancel = SG_solve_single_constraint.getCancel(tvars[tvars.length - 1]);
        final IProgress progress = SG_solve_single_constraint.getProgress(tvars[tvars.length - 2]);

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
            final IStrategoTerm arg = prepareArguments(args, strategoTerms, env.getFactory());
            try {
                final CallableStrategy strategy = StrategyCalls.lookup(env, name, strCache);
                final Optional<IStrategoTerm> result = strategy.call(arg);
                return result.map(strategoTerms::fromStratego).map(ConstraintTerms::specialize);
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

}