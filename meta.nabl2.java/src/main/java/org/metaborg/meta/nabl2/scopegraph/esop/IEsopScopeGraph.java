package org.metaborg.meta.nabl2.scopegraph.esop;

import org.metaborg.meta.nabl2.scopegraph.IActiveScopes;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.IScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.esop.persistent.PersistentScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.esop.reference.EsopScopeGraph;
import org.metaborg.meta.nabl2.util.collections.IFunction;
import org.metaborg.meta.nabl2.util.collections.IRelation3;
import org.metaborg.meta.nabl2.util.functions.Function1;

import com.google.common.annotations.Beta;

import io.usethesource.capsule.Set;

@Beta
public interface IEsopScopeGraph<S extends IScope, L extends ILabel, O extends IOccurrence>
        extends IScopeGraph<S, L, O> {

    public static final boolean USE_PERSISTENT_SCOPE_GRAPH = Boolean.getBoolean("usePersistentScopeGraph");

    /*
     * Factory method to switch between different scope graph implementations.
     */
    static <S extends IScope, L extends ILabel, O extends IOccurrence> IEsopScopeGraph.Builder<S, L, O> builder() {
        if(USE_PERSISTENT_SCOPE_GRAPH) {
            return new PersistentScopeGraph.Builder<>();
        } else {
            return new EsopScopeGraph<>();
        }
    }

    static <S extends IScope, L extends ILabel, O extends IOccurrence> IEsopScopeGraph.Builder<S, L, O>
            builder(IEsopScopeGraph<S, L, O> scopeGraph) {
        if(USE_PERSISTENT_SCOPE_GRAPH) {
            // TODO: Initialize builder of persistent graphs with an existing graph
            throw new UnsupportedOperationException();
        } else {
            return new EsopScopeGraph<>(scopeGraph);
        }
    }

    IEsopNameResolution<S, L, O> resolve(IResolutionParameters<L> params, IActiveScopes<S, L> scopeCounter,
            Function1<S, String> tracer);

    interface Builder<S extends IScope, L extends ILabel, O extends IOccurrence> {

        Set<S> getAllScopes();

        Set<O> getAllDecls();

        Set<O> getAllRefs();

        IFunction<O, S> getDecls();

        IFunction<O, S> getRefs();

        IRelation3<S, L, S> getDirectEdges();

        IRelation3<O, L, S> getExportEdges();

        IRelation3<S, L, O> getImportEdges();

        // -----------------------

        void addDecl(S scope, O decl);

        void addRef(O ref, S scope);

        void addDirectEdge(S sourceScope, L label, S targetScope);

        void addAssoc(O decl, L label, S scope);

        void addImport(S scope, L label, O ref);

        // -----------------------

        IEsopScopeGraph<S, L, O> result();

    }

}