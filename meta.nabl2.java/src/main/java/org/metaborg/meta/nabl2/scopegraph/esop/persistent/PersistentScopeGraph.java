package org.metaborg.meta.nabl2.scopegraph.esop.persistent;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.OpenCounter;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.util.collections.HashFunction;
import org.metaborg.meta.nabl2.util.collections.HashRelation3;
import org.metaborg.meta.nabl2.util.collections.IFunction;
import org.metaborg.meta.nabl2.util.collections.IRelation3;
import org.metaborg.meta.nabl2.util.functions.Function1;

import io.usethesource.capsule.Set;

public class PersistentScopeGraph<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements IEsopScopeGraph<S, L, O>, java.io.Serializable {

    private static final long serialVersionUID = 42L;

    private final Set.Immutable<S> allScopes;
    private final Set.Immutable<O> allDeclarations;
    private final Set.Immutable<O> allReferences;

    private final IFunction<O, S> declarations;
    private final IFunction<O, S> references;

    private final IRelation3<S, L, S> directEdges;
    private final IRelation3<O, L, S> exportEdges;
    private final IRelation3<S, L, O> importEdges;

    public PersistentScopeGraph(final Set.Immutable<S> allScopes, final Set.Immutable<O> allDeclarations,
            final Set.Immutable<O> allReferences, final IFunction<O, S> declarations, final IFunction<O, S> references,
            final IRelation3<S, L, S> directEdges, final IRelation3<O, L, S> exportEdges,
            final IRelation3<S, L, O> importEdges) {
        this.allScopes = allScopes;
        this.allDeclarations = allDeclarations;
        this.allReferences = allReferences;

        this.declarations = declarations;
        this.references = references;

        this.directEdges = directEdges;
        this.exportEdges = exportEdges;
        this.importEdges = importEdges;
    }

    @Override
    public Set.Immutable<S> getAllScopes() {
        return allScopes;
    }

    @Override
    public Set.Immutable<O> getAllDecls() {
        return allDeclarations;
    }

    @Override
    public Set.Immutable<O> getAllRefs() {
        return allReferences;
    }

    @Override
    public IFunction<O, S> getDecls() {
        return declarations;
    }

    @Override
    public IFunction<O, S> getRefs() {
        return references;
    }

    @Override
    public IRelation3<S, L, S> getDirectEdges() {
        return directEdges;
    }

    @Override
    public IRelation3<O, L, S> getExportEdges() {
        return exportEdges;
    }

    @Override
    public IRelation3<S, L, O> getImportEdges() {
        return importEdges;
    }

    @Override
    public PersistentNameResolution<S, L, O> resolve(IResolutionParameters<L> params, OpenCounter<S, L> scopeCounter, Function1<S, String> tracer) {
        return new PersistentNameResolution<>(this, params, scopeCounter);
    }

    public static class Builder<S extends IScope, L extends ILabel, O extends IOccurrence>
            implements IEsopScopeGraph.Builder<S, L, O> {

        private final Set.Transient<S> allScopes;
        private final Set.Transient<O> allDeclarations;
        private final Set.Transient<O> allReferences;

        private final IFunction.Mutable<O, S> declarations;
        private final IFunction.Mutable<O, S> references;

        private final IRelation3.Mutable<S, L, S> directEdges;
        private final IRelation3.Mutable<O, L, S> exportEdges;
        private final IRelation3.Mutable<S, L, O> importEdges;
       
        private IEsopScopeGraph<S, L, O> result = null;

        public Builder() {
            this.allScopes = Set.Transient.of();
            this.allDeclarations = Set.Transient.of();
            this.allReferences = Set.Transient.of();

            this.declarations = HashFunction.create();
            this.references = HashFunction.create();

            this.directEdges = HashRelation3.create();
            this.exportEdges = HashRelation3.create();
            this.importEdges = HashRelation3.create();
        }

        void requireNonSealed() {
            if (result != null) {
                throw new IllegalStateException("Mutation prohibited, builder is already closed.");
            }
        }
        
        public void addDecl(S scope, O decl) {
            requireNonSealed();            
            
            allScopes.__insert(scope);
            allDeclarations.__insert(decl);
            declarations.put(decl, scope);
        }

        public void addRef(O ref, S scope) {
            requireNonSealed();
            
            allScopes.__insert(scope);
            allReferences.__insert(ref);
            references.put(ref, scope);
        }

        public void addDirectEdge(S sourceScope, L label, S targetScope) {
            requireNonSealed();
            
            allScopes.__insert(sourceScope);
            directEdges.put(sourceScope, label, targetScope);
        }

        public void addAssoc(O decl, L label, S scope) {
            requireNonSealed();
            
            allScopes.__insert(scope);
            allDeclarations.__insert(decl);
            exportEdges.put(decl, label, scope);
        }

        public void addImport(S scope, L label, O ref) {
            requireNonSealed();            
            
            allScopes.__insert(scope);
            allReferences.__insert(ref);
            importEdges.put(scope, label, ref);
        }

        @Override
        public Set<S> getAllScopes() {
            return allScopes;
        }

        @Override
        public Set<O> getAllDecls() {
            return allDeclarations;
        }

        @Override
        public Set<O> getAllRefs() {
            return allReferences;
        }

        @Override
        public IFunction<O, S> getDecls() {
            return declarations;
        }

        @Override
        public IFunction<O, S> getRefs() {
            return references;
        }

        @Override
        public IRelation3<S, L, S> getDirectEdges() {
            return directEdges;
        }

        @Override
        public IRelation3<O, L, S> getExportEdges() {
            return exportEdges;
        }

        @Override
        public IRelation3<S, L, O> getImportEdges() {
            return importEdges;
        }

        @Override
        public IEsopScopeGraph<S, L, O> result() {
            if (result == null) {
                result = new PersistentScopeGraph<>(allScopes.freeze(), allDeclarations.freeze(),
                        allReferences.freeze(), declarations, references, directEdges, exportEdges, importEdges);
            }

            return result;
        }
    }

}