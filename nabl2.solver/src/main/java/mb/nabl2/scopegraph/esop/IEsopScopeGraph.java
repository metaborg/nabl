package mb.nabl2.scopegraph.esop;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.metaborg.util.functions.Function1;

import com.google.common.annotations.Beta;

import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;
import mb.nabl2.scopegraph.IScopeGraph;
import mb.nabl2.scopegraph.esop.reference.EsopScopeGraph;
import mb.nabl2.util.Tuple2;

@Beta
public interface IEsopScopeGraph<S extends IScope, L extends ILabel, O extends IOccurrence, V>
        extends IScopeGraph<S, L, O> {

    public static final boolean USE_PERSISTENT_SCOPE_GRAPH = Boolean.getBoolean("usePersistentScopeGraph");

    /*
     * Factory method to switch between different scope graph implementations.
     */
    static <S extends IScope, L extends ILabel, O extends IOccurrence, V> IEsopScopeGraph.Transient<S, L, O, V>
            builder() {
        if(USE_PERSISTENT_SCOPE_GRAPH) {
            throw new IllegalArgumentException("Persisent scope graphs are temporarily removed.");
        } else {
            return EsopScopeGraph.Transient.of();
        }
    }

    boolean isOpen(S scope, L label);

    Collection<? extends Map.Entry<Tuple2<S, L>, V>> incompleteDirectEdges();

    Collection<? extends Map.Entry<Tuple2<S, L>, V>> incompleteImportEdges();

    boolean isComplete();

    interface Immutable<S extends IScope, L extends ILabel, O extends IOccurrence, V>
            extends IEsopScopeGraph<S, L, O, V>, IScopeGraph.Immutable<S, L, O> {

        IEsopScopeGraph.Transient<S, L, O, V> melt();

    }

    interface Transient<S extends IScope, L extends ILabel, O extends IOccurrence, V>
            extends IEsopScopeGraph<S, L, O, V> {

        boolean addDecl(S scope, O decl);

        boolean addRef(O ref, S scope);

        boolean addDirectEdge(S sourceScope, L label, S targetScope);

        boolean addIncompleteDirectEdge(S scope, L label, V var, Function1<V, ? extends Iterable<? extends V>> norm);

        boolean addExportEdge(O decl, L label, S scope);

        boolean addImportEdge(S scope, L label, O ref);

        boolean addIncompleteImportEdge(S scope, L label, V var, Function1<V, ? extends Iterable<? extends V>> norm);

        Iterable<V> incompleteVars();

        boolean addAll(IEsopScopeGraph<S, L, O, V> other, Function1<V, ? extends Iterable<? extends V>> norm);

        List<CriticalEdge> reduceAll(Function1<V, ? extends Iterable<? extends V>> norm, Function1<V, S> fs,
                Function1<V, O> fo);

        List<CriticalEdge> reduce(Iterable<? extends V> vs, Function1<V, ? extends Iterable<? extends V>> norm,
                Function1<V, S> fs, Function1<V, O> fo);

        // -----------------------

        IEsopScopeGraph.Immutable<S, L, O, V> freeze();

    }

}