package mb.p_raffrayi;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.task.NullCancel;
import org.metaborg.util.tuple.Tuple2;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;

import io.usethesource.capsule.Set.Immutable;
import mb.p_raffrayi.impl.Broker;
import mb.scopegraph.oopsla20.diff.BiMap;

public abstract class PRaffrayiTestBase {

    private final ScopeImpl scopeImpl = new ScopeImpl();

    private final PRaffrayiSettings settings = PRaffrayiSettings.of(true, true, true, true);

    protected <L, R extends IResult<Scope, L, IDatum> & ITypeCheckerState<Scope, L, IDatum>> IFuture<IUnitResult<Scope, L, IDatum, R, R>> run(String id,
            ITypeChecker<Scope, L, IDatum, R, R> typeChecker, Iterable<L> edgeLabels) {
        return Broker.debug(id, settings, typeChecker, scopeImpl, edgeLabels,
                new NullCancel(), 0.3, 50);
    }

    protected <R extends IResult<Scope, Integer, IDatum>> IFuture<IUnitResult<Scope, Integer, IDatum, R, EmptyI>> run(String id,
            ITypeChecker<Scope, Integer, IDatum, R, EmptyI> typeChecker, Iterable<Integer> edgeLabels, boolean changed,
            IUnitResult<Scope, Integer, IDatum, R, EmptyI> previousResult) {
        return Broker.debug(id, settings, typeChecker, scopeImpl, edgeLabels, changed,
                previousResult, new NullCancel(), 0.3, 50);
    }

    protected <R> IFuture<IUnitResult<Scope, Integer, IDatum, Result<Integer, R>, EmptyI>> run(TestTypeChecker<R> typeChecker,
            Iterable<Integer> edgeLabels, IUnitResult<Scope, Integer, IDatum, Result<Integer, R>, EmptyI> previousResult) {
        return Broker.debug(typeChecker.getId(), settings, typeChecker, scopeImpl, edgeLabels, typeChecker.isChanged(),
                previousResult, new NullCancel(), 0.3, 50);
    }

    ///////////////////////////////////////////////////////////////////////////

    protected abstract class TestTypeChecker<R> implements ITypeChecker<Scope, Integer, IDatum, Result<Integer, R>, EmptyI> {

        private final String id;
        private final boolean changed;

        protected TestTypeChecker(String id, boolean changed) {
            this.id = id;
            this.changed = changed;
        }

        public String getId() {
            return id;
        }

        public boolean isChanged() {
            return changed;
        }
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

        @Override public List<Scope> scopes() {
            return ImmutableList.of(this);
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

        @Override public IDatum substituteScopes(IDatum datum, Map<Scope, Scope> substitution) {
            return datum;
        }

        @Override public Immutable<Scope> getScopes(IDatum datum) {
            return CapsuleUtil.toSet(datum.scopes());
        }

        @Override public IDatum embed(Scope scope) {
            return scope;
        }

        @Override public Optional<BiMap.Immutable<Scope>> matchDatums(IDatum currentDatum, IDatum previousDatum) {
            if(currentDatum.scopes().size() != previousDatum.scopes().size()) {
                return Optional.empty();
            }

            final BiMap.Transient<Scope> result = BiMap.Transient.of();
            final List<Tuple2<Scope, Scope>> matches =
                    Streams.zip(currentDatum.scopes().stream(), previousDatum.scopes().stream(), Tuple2::of)
                            .collect(Collectors.toList());

            for(Tuple2<Scope, Scope> match : matches) {
                if(!result.canPut(match._1(), match._2())) {
                    return Optional.empty();
                }
                result.put(match._1(), match._2());
            }
            return Optional.of(result.freeze());
        }

    };

    ///////////////////////////////////////////////////////////////////////////

    protected static class Result<L, T> implements IResult<Scope, L, IDatum>, ITypeCheckerState<Scope, L, IDatum> {

        private final T value;

        private Result(T value) {
            this.value = value;
        }

        public static <L, T> Result<L, T> of(T value) {
            return new Result<>(value);
        }

        public T value() {
            return value;
        }

        @Override public IDatum getExternalRepresentation(IDatum datum) {
            return datum;
        }

        @Override public Optional<IDatum> tryGetExternalDatum(IDatum datum) {
            return Optional.of(datum);
        }

    }

    protected static class EmptyO implements IResult<Scope, Object, IDatum>, ITypeCheckerState<Scope, Object, IDatum> {

        private static final PRaffrayiTestBase.EmptyO instance = new PRaffrayiTestBase.EmptyO();

        public static PRaffrayiTestBase.EmptyO of() {
            return instance;
        }

        @Override public IDatum getExternalRepresentation(IDatum datum) {
            return datum;
        }

        @Override public Optional<IDatum> tryGetExternalDatum(IDatum datum) {
            return Optional.of(datum);
        }

    }

    protected static class EmptyI implements IResult<Scope, Integer, IDatum>, ITypeCheckerState<Scope, Integer, IDatum> {

        private static final PRaffrayiTestBase.EmptyI instance = new PRaffrayiTestBase.EmptyI();

        public static PRaffrayiTestBase.EmptyI of() {
            return instance;
        }

        @Override public IDatum getExternalRepresentation(IDatum datum) {
            return datum;
        }

        @Override public Optional<IDatum> tryGetExternalDatum(IDatum datum) {
            return Optional.of(datum);
        }

    }

}
