package mb.p_raffrayi;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.junit.Ignore;
import org.junit.Test;
import org.metaborg.util.future.AggregateFuture;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.task.ICancel;

import mb.p_raffrayi.IIncrementalTypeCheckerContext;
import mb.p_raffrayi.IRecordedQuery;
import mb.p_raffrayi.ITypeChecker;
import mb.p_raffrayi.ITypeCheckerContext;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.PRaffrayiTestBase;
import mb.p_raffrayi.PRaffrayiTestBase.IDatum;
import mb.p_raffrayi.PRaffrayiTestBase.Scope;
import mb.p_raffrayi.impl.AInitialState;
import mb.p_raffrayi.impl.IInitialState;
import mb.p_raffrayi.impl.RecordedQuery;
import mb.p_raffrayi.impl.UnitResult;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.reference.ScopeGraph;

public class ConfirmationTest extends PRaffrayiTestBase {

    private static final <R> UnitResult<Scope, IDatum, IDatum, R> emptyResult(String id, R analysis) {
        return UnitResult.of(id, ScopeGraph.Immutable.of(), new HashSet<>(), new ArrayList<>(), analysis, new ArrayList<>(), new HashMap<>(), null);
    }

    private static final <R> IUnitResult<Scope, IDatum, IDatum, R> resultWithQueries(String id, R analysis, Set<IRecordedQuery<Scope, IDatum, IDatum>> queries) {
        return UnitResult.<Scope, IDatum, IDatum, R>builder()
            .from(emptyResult(id, analysis))
            .addAllQueries(queries)
            .build();
    }

    @Ignore("Testing for unstarted units not yet supported.")
    @Test(timeout = 10000) public void testSingleDenial()
        throws ExecutionException, InterruptedException {
        final IDatum l1 = new IDatum() {};

        final IFuture<IUnitResult<Scope, IDatum, IDatum, Boolean>> future =
            run(".", new ITypeChecker<Scope, IDatum, IDatum, Boolean>() {

                @Override public IFuture<Boolean> run(IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Boolean> unit, List<Scope> roots,
                    IInitialState<Scope, IDatum, IDatum, Boolean> initialState) {
                    Scope s = unit.freshScope("s", Collections.emptySet(), false, true);

                    IFuture<IUnitResult<Scope, IDatum, IDatum, Boolean>> unit1Result = unit.add("one", new FalseUnit(), Arrays.asList(s), AInitialState.cached(emptyResult("one", true)));

                    IUnitResult<Scope, IDatum, IDatum, Boolean> previousResult = ConfirmationTest.<Boolean>resultWithQueries("two", false, Collections.singleton(
                        RecordedQuery.<Scope, IDatum, IDatum>of(s, new ConstantClosureWf<IDatum>(l1), new TrueDataWf(), new HashcodeLabelOrder(), new FalseDataOrder(), Env.empty())));

                    IFuture<IUnitResult<Scope, IDatum, IDatum, Boolean>> unit2Result = unit.add("two", new EmptyUnit(), Arrays.asList(s), AInitialState.cached(previousResult));

                    unit.closeScope(s);

                    return AggregateFuture.apply(unit1Result, unit2Result).thenApply(r -> r._1().analysis() & r._2().analysis());
                }

            }, Arrays.asList(l1));

        final IUnitResult<Scope, IDatum, IDatum, Boolean> result = future.asJavaCompletion().get();
        assertTrue(result.failures().isEmpty());
        assertTrue(result.analysis());
    }

    // Type checkers

    private static class EmptyUnit implements ITypeChecker<Scope, IDatum, IDatum, Boolean> {

        @Override public IFuture<Boolean> run(IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Boolean> unit, List<Scope> rootScopes,
            IInitialState<Scope, IDatum, IDatum, Boolean> initialState) {

            rootScopes.forEach(s -> {
                unit.initScope(s, Collections.emptyList(), false);
            });

            return CompletableFuture.completedFuture(true);
        }

    }

    private static class FalseUnit implements ITypeChecker<Scope, IDatum, IDatum, Boolean> {

        @Override public IFuture<Boolean> run(IIncrementalTypeCheckerContext<Scope, IDatum, IDatum, Boolean> unit, List<Scope> rootScopes,
            IInitialState<Scope, IDatum, IDatum, Boolean> initialState) {

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

    private class TrueDataWf implements DataWf<Scope, IDatum, IDatum> {

        @Override public IFuture<Boolean> wf(IDatum d, ITypeCheckerContext<Scope, IDatum, IDatum> context, ICancel cancel) throws InterruptedException {
            return CompletableFuture.completedFuture(true);
        }

    }

    private class HashcodeLabelOrder implements LabelOrder<IDatum> {

        @Override public boolean lt(EdgeOrData<IDatum> l1, EdgeOrData<IDatum> l2) {
            return l1.match(() -> true, e1 ->  l2.match(() -> false, e2 -> e1.hashCode() < e2.hashCode()));
        }

    }

    private class FalseDataOrder implements DataLeq<Scope, IDatum, IDatum> {

        @Override public IFuture<Boolean> leq(IDatum d1, IDatum d2, ITypeCheckerContext<Scope, IDatum, IDatum> context, ICancel cancel) throws InterruptedException {
            return CompletableFuture.completedFuture(true);
        }
    }
}
