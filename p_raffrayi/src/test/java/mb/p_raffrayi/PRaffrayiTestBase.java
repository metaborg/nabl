package mb.p_raffrayi;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Function2;
import org.metaborg.util.future.AggregateFuture;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.task.NullCancel;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Streams;

import ch.qos.logback.classic.Logger;
import io.usethesource.capsule.Set.Immutable;
import mb.p_raffrayi.IScopeImpl;
import mb.p_raffrayi.ITypeChecker;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.impl.Broker;
import mb.p_raffrayi.impl.IInitialState;
import mb.p_raffrayi.impl.diff.IScopeGraphDifferOps;

public abstract class PRaffrayiTestBase {

    private final ScopeImpl scopeImpl = new ScopeImpl();

    protected <L, R> IFuture<IUnitResult<Scope, L, IDatum, R>> run(String id,
            ITypeChecker<Scope, L, IDatum, R> typeChecker, Iterable<L> edgeLabels) {
        return Broker.debug(id, typeChecker, scopeImpl, edgeLabels, new DifferOps(), new NullCancel(), 0.3, 50);
        //return Broker.run(id, typeChecker, scopeImpl, edgeLabels, new NullCancel());
    }

    protected <R> IFuture<IUnitResult<Scope, IDatum, IDatum, R>> run(String id,
            ITypeChecker<Scope, IDatum, IDatum, R> typeChecker, Iterable<IDatum> edgeLabels,
            IInitialState<Scope, IDatum, IDatum, R> initialState) {
        return Broker.debug(id, typeChecker, scopeImpl, edgeLabels, initialState, new DifferOps(),
                new NullCancel(), 0.3, 50);
        //return Broker.run(id, typeChecker, scopeImpl, edgeLabels, initialState, new NullCancel());
    }

    ///////////////////////////////////////////////////////////////////////////

    protected final static class Scope implements IDatum {

        private final String id;
        private final int index;

        public Scope(String id, int index) {
            this.id = id;
            this.index = index;
        }

        @Override public int hashCode() {
            return Objects.hash(id, index);
        }

        @Override public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            Scope other = (Scope) obj;
            return Objects.equals(id, other.id) && index == other.index;
        }

        @Override public String toString() {
            return String.format("%s-%d", id, index);
        }

    }

    protected static interface IDatum {
        default List<Scope> scopes() {
            return Arrays.asList();
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    private class ScopeImpl implements IScopeImpl<Scope, IDatum> {

        private int count = 0;
        private java.util.Set<Scope> scopes = new HashSet<>();

        @Override public Scope make(String id, String name) {
            Scope s = new Scope(id, count++);
            scopes.add(s);
            return s;
        }

        @Override public String id(Scope scope) {
            return scope.id;
        }

        @Override public Collection<Scope> getAllScopes(IDatum datum) {
            return Collections.unmodifiableSet(scopes);
        }

        @Override public IDatum substituteScopes(IDatum datum, Map<Scope, Scope> substitution) {
            throw new RuntimeException("Not implemented for tests.");
        }

    };

    private class DifferOps implements IScopeGraphDifferOps<Scope, IDatum> {

        @Override public Immutable<Scope> getScopes(IDatum datum) {
            return CapsuleUtil.toSet(datum.scopes());
        }

        @Override public IFuture<Boolean> matchDatums(IDatum currentDatum, IDatum previousDatum,
                Function2<Scope, Scope, IFuture<Boolean>> scopeMatch) {
            if(currentDatum.scopes().size() != previousDatum.scopes().size()) {
                return CompletableFuture.completedFuture(false);
            }

            return new AggregateFuture<>(
                    Streams.zip(currentDatum.scopes().stream(), previousDatum.scopes().stream(), scopeMatch::apply)
                            .collect(Collectors.toList()))
                                    .thenApply((List<Boolean> r) -> r.stream().allMatch(Boolean::booleanValue));
        }

    }

}
