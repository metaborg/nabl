package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.util.functions.Function1;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.ImmutableTuple3;
import mb.nabl2.util.Tuple3;
import mb.statix.solver.IConstraint;
import mb.statix.solver.ISolverResult;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Spec;
import mb.statix.taico.incremental.strategy.NonIncrementalStrategy;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.Module;
import mb.statix.taico.solver.ASolverCoordinator;
import mb.statix.taico.solver.MState;
import mb.statix.taico.solver.SolverContext;
import mb.statix.taico.solver.SolverCoordinator;
import mb.statix.taico.solver.concurrent.ConcurrentSolverCoordinator;

public class MSTX_solve_constraint extends StatixPrimitive {
    public static final boolean MODULES_OVERRIDE = true;
    public static final boolean OVERRIDE_LOGLEVEL = true;
    public static final String LOGLEVEL = "info"; //"debug" "none"
    public static final boolean CONCURRENT = false;
    public static final boolean QUERY_DEBUG = false;
    public static final boolean CLEAN = false;

    @Inject public MSTX_solve_constraint() {
        super(MSTX_solve_constraint.class.getSimpleName(), 2);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        if (CLEAN) throw new UnsupportedOperationException("Throwing error to clean build!");
        final Spec spec =
                StatixTerms.spec().match(terms.get(0)).orElseThrow(() -> new InterpreterException("Expected spec."));
        reportOverlappingRules(spec);

        final IDebugContext debug = getDebugContext(OVERRIDE_LOGLEVEL ? B.newString(LOGLEVEL) : terms.get(1));

        final IMatcher<Tuple3<String, List<ITermVar>, Set<IConstraint>>> constraintMatcher =
                M.tuple3(M.stringValue(), M.listElems(StatixTerms.varTerm()), StatixTerms.constraints(spec.labels()),
                        (t, r, vs, c) -> ImmutableTuple3.of(r, vs, c));
        
        final Function1<Tuple3<String, List<ITermVar>, Set<IConstraint>>, ITerm> solveConstraint =
                res_vars_constraint -> solveConstraint(spec, res_vars_constraint._1(), res_vars_constraint._2(), res_vars_constraint._3(), debug);
        
        // @formatter:off
        return M.cases(
            constraintMatcher.map(solveConstraint::apply),
            M.listElems(constraintMatcher).map(vars_constraints -> {
                return B.newList(vars_constraints.parallelStream().map(solveConstraint::apply).collect(Collectors.toList()));
            })
        ).match(term);
        // @formatter:on
//        final Tuple3<String, List<ITermVar>, Set<IConstraint>> vars_constraint = M
//                .tuple3(M.stringValue(), M.listElems(StatixTerms.varTerm()), StatixTerms.constraints(spec.labels()),
//                        (t, r, vs, c) -> ImmutableTuple3.of(r, vs, c))
//                .match(term).orElseThrow(() -> new InterpreterException("Expected constraint."));
//
//        final ITerm resultTerm = solveConstraint(spec, vars_constraint._1(), vars_constraint._2(), vars_constraint._3(), debug);
//        return Optional.of(resultTerm);
    }

    private ITerm solveConstraint(Spec spec, String resource, List<ITermVar> topLevelVars, Set<IConstraint> constraints,
            IDebugContext debug) {
        //Create a context and a coordinator
        final SolverContext context = SolverContext.initialContext(new NonIncrementalStrategy(), spec);
        final ASolverCoordinator coordinator = CONCURRENT ? new ConcurrentSolverCoordinator() : new SolverCoordinator();
        context.setCoordinator(coordinator);
        
        //Create the top level module and state. It is added to the context automatically.
        final IModule module = new Module(context, resource);
        final MState state = new MState(context, module);
        final ISubstitution.Transient subst = PersistentSubstitution.Transient.of();
        for(ITermVar var : topLevelVars) {
            final ITermVar nvar = state.freshVar(var.getName());
            subst.put(var, nvar);
            subst.put(nvar, var);
        }
        final ISubstitution.Immutable isubst = subst.freeze();
        
        constraints = constraints.stream().map(c -> c.apply(isubst)).collect(Collectors.toSet());
        
        final ISolverResult resultConfig;
        try {
            resultConfig = coordinator.solve(state, constraints, debug);
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        final IUnifier.Immutable unifier = state.unifier();

        final List<ITerm> errorList = Lists.newArrayList();
        if(resultConfig.hasErrors()) {
            resultConfig.errors().stream().map(c -> makeMessage("Failed", c, unifier)).forEach(errorList::add);
        }

        final Collection<IConstraint> unsolved = resultConfig.delays().keySet();
        if(!unsolved.isEmpty()) {
            unsolved.stream().map(c -> makeMessage("Unsolved", c, unifier)).forEach(errorList::add);
        }

        
        final ITerm substTerm =
                StatixTerms.explicateMapEntries(toplevelSubstitution(topLevelVars, isubst, state.unifier()).entrySet());
        final ITerm solverTerm = B.newBlob(resultConfig.withDelays(ImmutableMap.of()).withErrors(ImmutableSet.of()));
        final ITerm solveResultTerm = B.newAppl("Solution", substTerm, solverTerm);
        final IListTerm errors = B.newList(errorList);
        final IListTerm warnings = B.EMPTY_LIST;
        final IListTerm notes = B.EMPTY_LIST;
        final ITerm resultTerm = B.newTuple(solveResultTerm, errors, warnings, notes);

        return resultTerm;
    }
}
