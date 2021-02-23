package mb.statix.concurrent.p_raffrayi;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.metaborg.util.task.ICancel;

import mb.nabl2.terms.ITerm;
import mb.statix.concurrent.actors.futures.AggregateFuture;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.impl.AInitialState;
import mb.statix.concurrent.p_raffrayi.impl.IInitialState;
import mb.statix.concurrent.p_raffrayi.impl.RecordedQuery;
import mb.statix.concurrent.p_raffrayi.impl.UnitResult;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataLeq;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataWf;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelOrder;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelWf;
import mb.statix.scopegraph.reference.EdgeOrData;
import mb.statix.scopegraph.reference.ScopeGraph;
import mb.statix.scopegraph.terms.Scope;

public class ConfirmationTest extends PRaffrayiTestBase {
    
    private static final <R> UnitResult<Scope, Integer, ITerm, R> emptyResult(String id, R analysis) {
        return UnitResult.of(id, ScopeGraph.Immutable.of(), new HashSet<>(), analysis, new ArrayList<>(), null);
    }
    
    private static final <R> IUnitResult<Scope, Integer, ITerm, R> resultWithQueries(String id, R analysis, Set<IRecordedQuery<Scope, Integer, ITerm>> queries) {
        return UnitResult.<Scope, Integer, ITerm, R>builder()
            .from(emptyResult(id, analysis))
            .addAllQueries(queries)
            .build();
    }

    @Test(timeout = 1000000) public void testSingleDenial()
        throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, Integer, ITerm, Boolean>> future =
            run(".", new ITypeChecker<Scope, Integer, ITerm, Boolean>() {

                @Override public IFuture<Boolean> run(ITypeCheckerContext<Scope, Integer, ITerm> unit, List<Scope> roots,
                    IInitialState<Scope, Integer, ITerm, Boolean> initialState) {
                    Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                    IFuture<IUnitResult<Scope, Integer, ITerm, Boolean>> unit1Result = unit.add("one", new FalseUnit(), Arrays.asList(s), AInitialState.cached(emptyResult("one", true)));
                    
                    IUnitResult<Scope, Integer, ITerm, Boolean> previousResult = ConfirmationTest.<Boolean>resultWithQueries("two", false, Collections.singleton(
                        RecordedQuery.<Scope, Integer, ITerm>of(s, new ConstantClosureWf<>(1), new TrueDataWf(), new IntegerLabelOrder(), new FalseDataOrder(), null, null)));
                    IFuture<IUnitResult<Scope, Integer, ITerm, Boolean>> unit2Result = unit.add("two", new EmptyUnit(), Arrays.asList(s), AInitialState.cached(previousResult));

                    unit.closeScope(s);

                    return AggregateFuture.apply(unit1Result, unit2Result).thenApply(r -> r._1().analysis() & r._2().analysis());
                }

            }, Arrays.asList(1, 2, 3));

        final IUnitResult<Scope, Integer, ITerm, Boolean> result = future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());
        assertTrue(result.analysis());
    }
    
    // Type checkers
    
    private static class EmptyUnit implements ITypeChecker<Scope, Integer, ITerm, Boolean> {

        @Override public IFuture<Boolean> run(ITypeCheckerContext<Scope, Integer, ITerm> unit, List<Scope> rootScopes,
            IInitialState<Scope, Integer, ITerm, Boolean> initialState) {
            
            rootScopes.forEach(s -> {
                unit.initScope(s, Collections.emptyList(), false);
            });
            
            return CompletableFuture.completedFuture(true);
        }
        
    }
    
    private static class FalseUnit implements ITypeChecker<Scope, Integer, ITerm, Boolean> {

        @Override public IFuture<Boolean> run(ITypeCheckerContext<Scope, Integer, ITerm> unit, List<Scope> rootScopes,
            IInitialState<Scope, Integer, ITerm, Boolean> initialState) {
            
            rootScopes.forEach(s -> {
                unit.initScope(s, Collections.emptyList(), false);
            });
            
            return CompletableFuture.completedFuture(false);
        }
        
    }
    
    /// Name resolution
    
    private class ConstantClosureWf<L> implements LabelWf<L> {
        
        private final L lbl;
        
        public ConstantClosureWf(L lbl) {
            this.lbl = lbl;
        }

        @Override public Optional<LabelWf<L>> step(L l) {
            if(l.equals(lbl)) {
                return Optional.of(this);
            }
            return Optional.empty();
        }

        @Override public boolean accepting() {
            return true;
        }
        
    }
    
    private class TrueDataWf implements DataWf<ITerm> {

        @Override public boolean wf(ITerm d, ICancel cancel) throws InterruptedException {
            return true;
        }
        
    }
    
    private class IntegerLabelOrder implements LabelOrder<Integer> {

        @Override public boolean lt(EdgeOrData<Integer> l1, EdgeOrData<Integer> l2) {
            return l1.match(() -> true, e1 ->  l2.match(() -> false, e2 -> e1 < e2));
        }
        
    }
    
    private class FalseDataOrder implements DataLeq<ITerm> {

        @Override public boolean leq(ITerm d1, ITerm d2, ICancel cancel) throws InterruptedException {
            return false;
        }
    }
}
