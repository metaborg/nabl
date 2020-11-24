package mb.statix.concurrent.solver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.util.task.NullCancel;
import org.metaborg.util.task.NullProgress;

import mb.nabl2.terms.ITerm;
import mb.nabl2.util.Tuple2;
import mb.statix.concurrent.actors.futures.AggregateFuture;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.ITypeChecker;
import mb.statix.concurrent.p_raffrayi.ITypeCheckerContext;
import mb.statix.concurrent.p_raffrayi.IUnitResult;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.Delay;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.solver.persistent.State;
import mb.statix.spec.ApplyMode;
import mb.statix.spec.ApplyResult;
import mb.statix.spec.Rule;
import mb.statix.spec.RuleUtil;
import mb.statix.spec.Spec;

public abstract class AbstractTypeChecker<R> implements ITypeChecker<Scope, ITerm, ITerm, R> {

    protected final Spec spec;
    protected final IDebugContext debug;

    protected AbstractTypeChecker(Spec spec, IDebugContext debug) {
        this.spec = spec;
        this.debug = debug;
    }

    private StatixSolver solver;

    protected Scope makeSharedScope(ITypeCheckerContext<Scope, ITerm, ITerm> context, String name) {
        final Scope s = context.freshScope(name, Collections.emptyList(), true, true);
        context.setDatum(s, s);
        context.shareLocal(s);
        return s;
    }

    protected IFuture<Map<String, IUnitResult<Scope, ITerm, ITerm, GroupResult>>>
            runGroups(ITypeCheckerContext<Scope, ITerm, ITerm> context, Map<String, IStatixGroup> groups, Scope root) {
        final List<IFuture<Tuple2<String, IUnitResult<Scope, ITerm, ITerm, GroupResult>>>> results = new ArrayList<>();
        for(Map.Entry<String, IStatixGroup> entry : groups.entrySet()) {
            final String key = entry.getKey();
            final IFuture<IUnitResult<Scope, ITerm, ITerm, GroupResult>> result =
                    context.add(key, new GroupTypeChecker(entry.getValue(), spec, debug), root);
            results.add(result.thenApply(r -> Tuple2.of(key, r)));
        }
        return new AggregateFuture<>(results)
                .thenApply(es -> es.stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
    }

    protected IFuture<Map<String, IUnitResult<Scope, ITerm, ITerm, UnitResult>>>
            runUnits(ITypeCheckerContext<Scope, ITerm, ITerm> context, Map<String, IStatixUnit> units, Scope root) {
        final List<IFuture<Tuple2<String, IUnitResult<Scope, ITerm, ITerm, UnitResult>>>> results = new ArrayList<>();
        for(Map.Entry<String, IStatixUnit> entry : units.entrySet()) {
            final String key = entry.getKey();
            final IFuture<IUnitResult<Scope, ITerm, ITerm, UnitResult>> result =
                    context.add(key, new UnitTypeChecker(entry.getValue(), spec, debug), root);
            results.add(result.thenApply(r -> Tuple2.of(key, r)));
        }
        return new AggregateFuture<>(results)
                .thenApply(es -> es.stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
    }

    protected IFuture<SolverResult> runSolver(ITypeCheckerContext<Scope, ITerm, ITerm> context, Optional<Rule> rule,
            Scope... scopes) {
        if(!rule.isPresent()) {
            for(Scope scope : scopes) {
                context.initScope(scope, Collections.emptyList(), false);
            }
            return CompletableFuture.completedFuture(SolverResult.of(spec));
        }
        final List<Scope> scopeList = Arrays.asList(scopes);
        final IState.Immutable unitState = State.of(spec).withResource(context.id());
        final ApplyResult applyResult;
        try {
            if((applyResult = RuleUtil.apply(unitState.unifier(), rule.get(), scopeList, null, ApplyMode.STRICT)
                    .orElse(null)) == null) {
                return CompletableFuture.completedExceptionally(
                        new IllegalArgumentException("Cannot apply initial rule to root scope."));
            }
        } catch(Delay delay) {
            return CompletableFuture.completedExceptionally(
                    new IllegalArgumentException("Cannot apply initial rule to root scope.", delay));
        }
        solver = new StatixSolver(applyResult.body(), spec, unitState, Completeness.Immutable.of(), debug,
                new NullProgress(), new NullCancel(), context);
        return solver.solve(scopeList);
    }

    @Override public IFuture<ITerm> getExternalDatum(ITerm datum) {
        final IFuture<ITerm> externalDatum;
        if(solver != null) {
            externalDatum = solver.getExternalRepresentation(datum);
        } else {
            return CompletableFuture.completedFuture(datum);
        }
        return externalDatum;
    }

}