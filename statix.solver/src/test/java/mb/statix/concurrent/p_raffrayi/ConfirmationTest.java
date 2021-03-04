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
import mb.nabl2.util.CapsuleUtil;

import static mb.nabl2.terms.build.TermBuild.B;
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
    
    private static final <R> UnitResult<Scope, ITerm, ITerm, R> emptyResult(String id, R analysis) {
        return UnitResult.of(id, ScopeGraph.Immutable.of(), new HashSet<>(), new ArrayList<>(), analysis, new ArrayList<>(), null);
    }
    
    private static final <R> IUnitResult<Scope, ITerm, ITerm, R> resultWithQueries(String id, R analysis, Set<IRecordedQuery<Scope, ITerm, ITerm>> queries) {
        return UnitResult.<Scope, ITerm, ITerm, R>builder()
            .from(emptyResult(id, analysis))
            .addAllQueries(queries)
            .build();
    }

    @Test(timeout = 10000) public void testSingleDenial()
        throws ExecutionException, InterruptedException {
        final IFuture<IUnitResult<Scope, ITerm, ITerm, Boolean>> future =
            run(".", new ITypeChecker<Scope, ITerm, ITerm, Boolean>() {

                @Override public IFuture<Boolean> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit, List<Scope> roots,
                    IInitialState<Scope, ITerm, ITerm, Boolean> initialState) {
                    Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                    IFuture<IUnitResult<Scope, ITerm, ITerm, Boolean>> unit1Result = unit.add("one", new FalseUnit(), Arrays.asList(s), AInitialState.cached(emptyResult("one", true)));

                    IUnitResult<Scope, ITerm, ITerm, Boolean> previousResult = ConfirmationTest.<Boolean>resultWithQueries("two", false, Collections.singleton(
                        RecordedQuery.<Scope, ITerm, ITerm>of(s, new ConstantClosureWf<ITerm>(B.newInt(1)), new TrueDataWf(), new HashcodeLabelOrder(), new FalseDataOrder())));

                    IFuture<IUnitResult<Scope, ITerm, ITerm, Boolean>> unit2Result = unit.add("two", new EmptyUnit(), Arrays.asList(s), AInitialState.cached(previousResult));

                    unit.closeScope(s);

                    return AggregateFuture.apply(unit1Result, unit2Result).thenApply(r -> r._1().analysis() & r._2().analysis());
                }

            }, Arrays.asList(B.newInt(1)));

        final IUnitResult<Scope, ITerm, ITerm, Boolean> result = future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());
        assertTrue(result.analysis());
    }

    // Type checkers
    
    private static class EmptyUnit implements ITypeChecker<Scope, ITerm, ITerm, Boolean> {

        @Override public IFuture<Boolean> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit, List<Scope> rootScopes,
            IInitialState<Scope, ITerm, ITerm, Boolean> initialState) {
            
            rootScopes.forEach(s -> {
                unit.initScope(s, Collections.emptyList(), false);
            });
            
            return CompletableFuture.completedFuture(true);
        }
        
    }
    
    private static class FalseUnit implements ITypeChecker<Scope, ITerm, ITerm, Boolean> {

        @Override public IFuture<Boolean> run(ITypeCheckerContext<Scope, ITerm, ITerm> unit, List<Scope> rootScopes,
            IInitialState<Scope, ITerm, ITerm, Boolean> initialState) {
            
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
    
    private class TrueDataWf implements DataWf<Scope, ITerm, ITerm> {

        @Override public IFuture<Boolean> wf(ITerm d, ITypeCheckerContext<Scope, ITerm, ITerm> context, ICancel cancel) throws InterruptedException {
            return CompletableFuture.completedFuture(true);
        }
        
    }
    
    private class HashcodeLabelOrder implements LabelOrder<ITerm> {

        @Override public boolean lt(EdgeOrData<ITerm> l1, EdgeOrData<ITerm> l2) {
            return l1.match(() -> true, e1 ->  l2.match(() -> false, e2 -> e1.hashCode() < e2.hashCode()));
        }
        
    }
    
    private class FalseDataOrder implements DataLeq<Scope, ITerm, ITerm> {

        @Override public IFuture<Boolean> leq(ITerm d1, ITerm d2, ITypeCheckerContext<Scope, ITerm, ITerm> context, ICancel cancel) throws InterruptedException {
            return CompletableFuture.completedFuture(true);
        }
    }
}
