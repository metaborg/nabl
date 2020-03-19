package mb.nabl2.spoofax.primitives;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.util.functions.PartialFunction1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.metaborg.util.task.NullProgress;
import org.metaborg.util.task.ThreadCancel;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.core.UndefinedStrategyException;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.stratego.SDefT;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.HybridInterpreter;
import org.strategoxt.lang.InteropContext;

import mb.nabl2.config.NaBL2DebugConfig;
import mb.nabl2.solver.solvers.CallExternal;
import mb.nabl2.solver.solvers.SemiIncrementalMultiFileSolver;
import mb.nabl2.stratego.ConstraintTerms;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.StrategoTerms;

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

        final ICancel cancel = new ThreadCancel();
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

    static CallExternal callExternal(IContext env, StrategoTerms strategoTerms) {
        final HashMap<String, SDefT> strCache = new HashMap<>();
        return (name, args) -> {
            final IStrategoTerm arg = prepareArguments(args, strategoTerms, env.getFactory());
            try {
                PartialFunction1<IStrategoTerm, IStrategoTerm> evalStrategy = callStrategy(env, strCache, name);
                final Optional<IStrategoTerm> result = evalStrategy.apply(arg);
                return result.map(strategoTerms::fromStratego).map(ConstraintTerms::specialize);
            } catch(Exception ex) {
                logger.warn("External call to '{}' failed.", ex, name);
                return Optional.empty();
            }
        };

    }

    private static PartialFunction1<IStrategoTerm, IStrategoTerm> callStrategy(IContext env,
            HashMap<String, SDefT> strCache, String name) throws InterpreterException {
        final String cname = name.replace('-', '_') + "_0_0";
        if(env instanceof InteropContext) {
            return callStrategyJar((InteropContext) env, name);
        } else {
            return callStrategyCtree(env, strCache, name, cname);
        }
    }

    private static PartialFunction1<IStrategoTerm, IStrategoTerm> callStrategyJar(InteropContext context, String name) {
        final HybridInterpreter interpreter = HybridInterpreter.getInterpreter(context.getContext());
        return arg -> {
            final IStrategoTerm prev = interpreter.current();
            try {
                interpreter.setCurrent(arg);
                if(!interpreter.invoke(name)) {
                    return Optional.empty();
                }
                return Optional.of(interpreter.current());
            } catch(UndefinedStrategyException ex) {
                logger.warn("Cannot find strategy '{}'", name);
                return Optional.empty();
            } catch(InterpreterException ex) {
                logger.warn("External call to '{}' failed.", ex, name);
                return Optional.empty();
            } finally {
                interpreter.setCurrent(prev);
            }
        };
    }

    private static PartialFunction1<IStrategoTerm, IStrategoTerm> callStrategyCtree(IContext env,
            HashMap<String, SDefT> strCache, String name, final String cname) throws InterpreterException {
        final SDefT s;
        if(strCache.containsKey(name)) {
            s = strCache.get(name);
        } else {
            s = env.lookupSVar(cname);
            if(s == null) {
                logger.warn("Cannot find strategy '{}'", name);
                return arg -> Optional.empty();
            }
            strCache.put(name, s);
        }
        return arg -> {
            final IStrategoTerm prev = env.current();
            try {
                if(!s.evaluate(env)) {
                    return Optional.empty();
                }
                return Optional.of(env.current());
            } catch(InterpreterException ex) {
                logger.warn("External call to '{}' failed.", ex, name);
                return Optional.empty();
            } finally {
                env.setCurrent(prev);
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