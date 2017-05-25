package org.metaborg.meta.nabl2.scopegraph.esop.persistent;

import static org.metaborg.meta.nabl2.scopegraph.esop.persistent.CollectionConverter.liftHashFunctionToRelation;
import static org.metaborg.meta.nabl2.scopegraph.esop.persistent.CollectionConverter.union;
import static org.metaborg.meta.nabl2.util.tuples.HasLabel.labelEquals;

import java.util.Objects;
import java.util.stream.Stream;

import org.metaborg.meta.nabl2.scopegraph.IActiveScopes;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.OpenCounter;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.esop.reference.EsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.util.collections.HashFunction;
import org.metaborg.meta.nabl2.util.collections.HashRelation3;
import org.metaborg.meta.nabl2.util.collections.IFunction;
import org.metaborg.meta.nabl2.util.collections.IInverseFunction;
import org.metaborg.meta.nabl2.util.collections.IRelation3;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.metaborg.meta.nabl2.util.tuples.ImmutableOccurrenceLabelScope;
import org.metaborg.meta.nabl2.util.tuples.ImmutableScopeLabelOccurrence;
import org.metaborg.meta.nabl2.util.tuples.ImmutableScopeLabelScope;
import org.metaborg.meta.nabl2.util.tuples.OccurrenceLabelScope;
import org.metaborg.meta.nabl2.util.tuples.ScopeLabelOccurrence;
import org.metaborg.meta.nabl2.util.tuples.ScopeLabelScope;

import io.usethesource.capsule.BinaryRelation;
import io.usethesource.capsule.BinaryRelation.Immutable;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;

public class PersistentScopeGraph<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements IEsopScopeGraph<S, L, O>, java.io.Serializable {    
    
    private static final long serialVersionUID = 42L;

    private final Set.Immutable<S> allScopes;

    private final IRelation3<O, L, S> sourceEdges;
    private final IRelation3<S, L, S> middleEdges;
    private final IRelation3<S, L, O> targetEdges;

    // TODO
    // private final TernaryRelation.Immutable<S, L, S> directEdges;
    // private final TernaryRelation.Immutable<S, L, O> declarations;
    // private final TernaryRelation.Immutable<S, L, O> references;

    @SuppressWarnings("unchecked")
    public PersistentScopeGraph(final Set.Immutable<S> allScopes, final Set.Immutable<O> allDeclarations,
            final Set.Immutable<O> allReferences, final IFunction<O, S> declarations, final IFunction<O, S> references,
            final IRelation3<S, L, S> directEdges, final IRelation3<O, L, S> assocEdges,
            final IRelation3<S, L, O> importEdges) {
        this.allScopes = allScopes;
              
        this.sourceEdges = union(assocEdges.inverse(), liftHashFunctionToRelation(references, (L) Label.R).inverse()).inverse();
        this.middleEdges = directEdges;
        this.targetEdges = union(importEdges, liftHashFunctionToRelation(declarations, (L) Label.D).inverse());
    }

    public Stream<OccurrenceLabelScope<O, L, S>> sourceEdgeStream() {
        return sourceEdges.stream(ImmutableOccurrenceLabelScope::of);
    }
    
    public Stream<OccurrenceLabelScope<O, L, S>> referenceEdgeStream() {
        // TODO: use hash lookup on label instead of filter
        return sourceEdgeStream().filter(labelEquals(Label.R));
    }
    
    public Stream<OccurrenceLabelScope<O, L, S>> associatedScopeEdgeStream() {
        // TODO: use hash lookup on label instead of filter
        return sourceEdgeStream().filter(labelEquals(Label.R).negate());
    }    
    
    public Stream<ScopeLabelScope<S, L, O>> middleEdgeStream() {
        return middleEdges.stream(ImmutableScopeLabelScope::of);
    }
    
    public Stream<ScopeLabelOccurrence<S, L, O>> targetEdgeStream() {
        return targetEdges.stream(ImmutableScopeLabelOccurrence::of);
    }    
    
    public Stream<ScopeLabelOccurrence<S, L, O>> declarationEdgeStream() {
        // TODO: use hash lookup on label instead of filter        
        return targetEdgeStream().filter(labelEquals(Label.D));
    }
    
    public Stream<ScopeLabelOccurrence<S, L, O>> requireImportEdgeStream() {
        // TODO: use hash lookup on label instead of filter
        return targetEdgeStream().filter(labelEquals(Label.D).negate());
    }
   
    @Deprecated
    @Override
    public Set.Immutable<S> getAllScopes() {
        return allScopes;
    }

    @Deprecated
    @Override
    public Set.Immutable<O> getAllDecls() {
        return Stream.concat(declarationEdgeStream().map(tuple -> tuple.occurrence()),
                associatedScopeEdgeStream().map(tuple -> tuple.occurrence())).collect(CapsuleCollectors.toSet());
    }
   
    @Deprecated
    @Override
    public IFunction<O, S> getDecls() {
        final IFunction.Mutable<O, S> result = HashFunction.create();
        
        // filter and project
        declarationEdgeStream().iterator()
                .forEachRemaining(tuple -> result.put(tuple.occurrence(), tuple.scope()));

        return result;
    }

    @Deprecated
    @Override
    public Set.Immutable<O> getAllRefs() {
        return Stream.concat(referenceEdgeStream().map(tuple -> tuple.occurrence()),
                requireImportEdgeStream().map(tuple -> tuple.occurrence())).collect(CapsuleCollectors.toSet());
    }
    
    @Deprecated
    @Override
    public IFunction<O, S> getRefs() {
        final IFunction.Mutable<O, S> result = HashFunction.create();
                
        // filter and project        
        referenceEdgeStream().iterator()
                .forEachRemaining(tuple -> result.put(tuple.occurrence(), tuple.scope()));
        
        return result;
    }

    @Deprecated
    @Override
    public IRelation3<S, L, S> getDirectEdges() {
        return middleEdges;
    }

    @Deprecated
    @Override
    public IRelation3<O, L, S> getExportEdges() {
        final IRelation3.Mutable<S, L, O> result = HashRelation3.create();

        // filter
        associatedScopeEdgeStream().iterator()
                .forEachRemaining(tuple -> result.put(tuple.scope(), tuple.label(), tuple.occurrence()));

        return result.inverse();
    }

    @Deprecated
    @Override
    public IRelation3<S, L, O> getImportEdges() {
        final IRelation3.Mutable<S, L, O> result = HashRelation3.create();

        // filter
        requireImportEdgeStream().iterator()
                .forEachRemaining(tuple -> result.put(tuple.scope(), tuple.label(), tuple.occurrence()));

        return result;
    }

    @Override
    public IEsopNameResolution<S, L, O> resolve(IResolutionParameters<L> params, OpenCounter<S, L> scopeCounter,
            Function1<S, String> tracer) {
        final IEsopNameResolution<S, L, O> one = new PersistentNameResolution<>(this, params, scopeCounter);
        final IEsopNameResolution<S, L, O> two = new AllShortestPathsNameResolution<>(this, params, scopeCounter);
        
        return new BiSimulationNameResolution<>(one, two);
    }

    public static class Builder<S extends IScope, L extends ILabel, O extends IOccurrence>
            implements IEsopScopeGraph.Builder<S, L, O> {

        private final Set.Transient<S> allScopes;
        private final Set.Transient<O> allDeclarations;
        private final Set.Transient<O> allReferences;

        private final IFunction.Mutable<O, S> declarations;
        private final IFunction.Mutable<O, S> references;

        private final IRelation3.Mutable<S, L, S> directEdges;
        private final IRelation3.Mutable<O, L, S> assocEdges;
        private final IRelation3.Mutable<S, L, O> importEdges;
       
        private IEsopScopeGraph<S, L, O> result = null;

        public Builder() {
            this.allScopes = Set.Transient.of();
            this.allDeclarations = Set.Transient.of();
            this.allReferences = Set.Transient.of();

            this.declarations = HashFunction.create();
            this.references = HashFunction.create();

            this.directEdges = HashRelation3.create();
            this.assocEdges = HashRelation3.create();
            this.importEdges = HashRelation3.create();
        }

        void requireNonSealed() {
            if (result != null) {
                throw new IllegalStateException("Mutation prohibited, builder is already closed.");
            }
        }
        
        void requireNonEqual(Object one, Object two) {
            if (Objects.equals(one, two)) {
                throw new IllegalArgumentException(String.format("Arguments must not equal:\n\t%s\n\t%s", one, two));
            }
        }
        
        public void addDirectEdge(S sourceScope, L label, S targetScope) {
            requireNonSealed();

            allScopes.__insert(sourceScope);
            allScopes.__insert(targetScope);
            directEdges.put(sourceScope, label, targetScope);
        }
        
        public void addDecl(S scope, O decl) {
            requireNonSealed();            
            
            allScopes.__insert(scope);
            allDeclarations.__insert(decl);
            declarations.put(decl, scope);
        }
        
        public void addAssoc(O decl, L label, S scope) {
            requireNonSealed();
            requireNonEqual(Label.P, label);
            
            allScopes.__insert(scope);
            allDeclarations.__insert(decl);
            assocEdges.put(decl, label, scope);
        }      
        
        public void addRef(O ref, S scope) {
            requireNonSealed();
            
            allScopes.__insert(scope);
            allReferences.__insert(ref);
            references.put(ref, scope);
        }

        public void addImport(S scope, L label, O ref) {
            requireNonSealed();
            requireNonEqual(Label.R, label);
            
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
            return assocEdges;
        }

        @Override
        public IRelation3<S, L, O> getImportEdges() {
            return importEdges;
        }

        @Override
        public IEsopScopeGraph<S, L, O> result() {
            if (result == null) {
                final EsopScopeGraph<S, L, O> one = new EsopScopeGraph<>();
                               
                declarations.keySet().forEach(o -> one.addDecl(declarations.get(o).get(), o));
                references.keySet().forEach(o -> one.addDecl(references.get(o).get(), o));

                directEdges.stream(ImmutableScopeLabelScope::of)
                        .forEach(sls -> one.addDirectEdge(sls.sourceScope(), sls.label(), sls.targetScope()));

                assocEdges.stream(ImmutableOccurrenceLabelScope::of)
                        .forEach(slo -> one.addAssoc(slo.occurrence(), slo.label(), slo.scope()));

                importEdges.stream(ImmutableScopeLabelOccurrence::of)
                        .forEach(slo -> one.addImport(slo.scope(), slo.label(), slo.occurrence()));               
                                                                
                final IEsopScopeGraph<S, L, O> two = new PersistentScopeGraph<>(allScopes.freeze(), allDeclarations.freeze(),
                        allReferences.freeze(), declarations, references, directEdges, assocEdges, importEdges);
                
                result = new BiSimulationScopeGraph<>(one, two);                
            }

            return result;
        }
    }
}

class CollectionConverter {

    // TODO: release Capsule and change input type to BinaryRelation.Immutable
    public static final <T, U> IFunction<T, U> relationToHashFunction(BinaryRelation<T, U> input) {
        final IFunction.Mutable<T, U> output = HashFunction.create();
        input.entryIterator().forEachRemaining(entry -> output.put(entry.getKey(), entry.getValue()));
        return output;
    }
    
    public static final <T, U> BinaryRelation.Immutable<T, U> hashFunctionToRelation(IInverseFunction<T, U> input) {
        return (Immutable<T, U>) hashFunctionToRelation(input.inverse()).inverse();
    }
    
    public static final <T, U> BinaryRelation.Immutable<T, U> hashFunctionToRelation(IFunction<T, U> input) {
        final BinaryRelation.Transient<T, U> output = BinaryRelation.Transient.of();
        input.keySet().forEach(key -> output.__insert(key, input.get(key).get()));
        return output.freeze();
    }

    public static final <T, U, V> IRelation3<T, U, V> liftHashFunctionToRelation(IInverseFunction<T, V> input,
            U intermediate) {
        return liftHashFunctionToRelation(input.inverse(), intermediate).inverse();
    }
    
    public static final <T, U, V> IRelation3<T, U, V> liftHashFunctionToRelation(IFunction<T, V> input,
            U intermediate) {
        final IRelation3.Mutable<T, U, V> output = HashRelation3.create();
        input.keySet().forEach(key -> output.put(key, intermediate, input.get(key).get()));
        return output;
    }
    
    public static final <T extends IScope, U extends ILabel, V extends IOccurrence> IRelation3<T, U, V> union(IRelation3<T, U, V> one, IRelation3<T, U, V> two) {
        final IRelation3.Mutable<T, U, V> result = HashRelation3.create();

        one.stream(ImmutableScopeLabelOccurrence::of).iterator().forEachRemaining(tuple -> result.put(tuple.scope(), tuple.label(), tuple.occurrence()));
        two.stream(ImmutableScopeLabelOccurrence::of).iterator().forEachRemaining(tuple -> result.put(tuple.scope(), tuple.label(), tuple.occurrence()));

        return result;
    }
    
}
