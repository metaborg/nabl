package org.metaborg.meta.nabl2.scopegraph.esop.persistent;

import static org.metaborg.meta.nabl2.scopegraph.esop.persistent.CollectionConverter.liftHashFunctionToRelation;
import static org.metaborg.meta.nabl2.scopegraph.esop.persistent.CollectionConverter.union;
import static org.metaborg.meta.nabl2.util.tuples.HasLabel.labelEquals;

import java.util.Objects;
import java.util.stream.Stream;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.esop.reference.EsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.util.collections.HashTrieFunction;
import org.metaborg.meta.nabl2.util.collections.HashTrieRelation3;
import org.metaborg.meta.nabl2.util.collections.IFunction;
import org.metaborg.meta.nabl2.util.collections.IInverseFunction;
import org.metaborg.meta.nabl2.util.collections.IRelation3;
import org.metaborg.meta.nabl2.util.tuples.ImmutableOccurrenceLabelScope;
import org.metaborg.meta.nabl2.util.tuples.ImmutableScopeLabelOccurrence;
import org.metaborg.meta.nabl2.util.tuples.ImmutableScopeLabelScope;
import org.metaborg.meta.nabl2.util.tuples.OccurrenceLabelScope;
import org.metaborg.meta.nabl2.util.tuples.ScopeLabelOccurrence;
import org.metaborg.meta.nabl2.util.tuples.ScopeLabelScope;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.PartialFunction1;
import org.metaborg.util.functions.Predicate3;

import io.usethesource.capsule.BinaryRelation;
import io.usethesource.capsule.BinaryRelation.Immutable;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;

public class PersistentScopeGraph<S extends IScope, L extends ILabel, O extends IOccurrence, V>
        implements IEsopScopeGraph.Immutable<S, L, O, V>, java.io.Serializable {    
    
    private static final long serialVersionUID = 42L;

    private final Set.Immutable<S> allScopes;

    private final IRelation3.Immutable<O, L, S> sourceEdges;
    private final IRelation3.Immutable<S, L, S> middleEdges;
    private final IRelation3.Immutable<S, L, O> targetEdges;
    
    private final IRelation3.Immutable<S, L, V> incompleteDirectEdges;
    private final IRelation3.Immutable<S, L, V> incompleteImportEdges;    

    // TODO
    // private final TernaryRelation.Immutable<S, L, S> directEdges;
    // private final TernaryRelation.Immutable<S, L, O> declarations;
    // private final TernaryRelation.Immutable<S, L, O> references;

    @SuppressWarnings("unchecked")
    public PersistentScopeGraph(final Set.Immutable<S> allScopes, final Set.Immutable<O> allDeclarations,
            final Set.Immutable<O> allReferences, final IFunction<O, S> declarations, final IFunction<O, S> references,
            final IRelation3.Immutable<S, L, S> directEdges, final IRelation3<O, L, S> assocEdges,
            final IRelation3.Immutable<S, L, O> importEdges,
            final IRelation3.Immutable<S, L, V> incompleteDirectEdges,
            final IRelation3.Immutable<S, L, V> incompleteImportEdges) {
        this.allScopes = allScopes;
              
        this.sourceEdges = (IRelation3.Immutable<O, L, S>) union(assocEdges.inverse(), liftHashFunctionToRelation(references, (L) Label.R).inverse()).inverse();
        this.middleEdges = directEdges;
        this.targetEdges = union(importEdges, liftHashFunctionToRelation(declarations, (L) Label.D).inverse());
        
        this.incompleteDirectEdges = incompleteDirectEdges;
        this.incompleteImportEdges = incompleteImportEdges;
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
    public IFunction.Immutable<O, S> getDecls() {
        final IFunction.Transient<O, S> result = HashTrieFunction.Transient.of();
        
        // filter and project
        declarationEdgeStream().iterator()
                .forEachRemaining(tuple -> result.put(tuple.occurrence(), tuple.scope()));

        return result.freeze();
    }

    @Deprecated
    @Override
    public Set.Immutable<O> getAllRefs() {
        return Stream.concat(referenceEdgeStream().map(tuple -> tuple.occurrence()),
                requireImportEdgeStream().map(tuple -> tuple.occurrence())).collect(CapsuleCollectors.toSet());
    }
    
    @Deprecated
    @Override
    public IFunction.Immutable<O, S> getRefs() {
        final IFunction.Transient<O, S> result = HashTrieFunction.Transient.of();
                
        // filter and project        
        referenceEdgeStream().iterator()
                .forEachRemaining(tuple -> result.put(tuple.occurrence(), tuple.scope()));
        
        return result.freeze();
    }

    @Deprecated
    @Override
    public IRelation3.Immutable<S, L, S> getDirectEdges() {
        return middleEdges;
    }

    @Deprecated
    @Override
    public IRelation3.Immutable<O, L, S> getExportEdges() {
        final IRelation3.Transient<S, L, O> result = HashTrieRelation3.Transient.of();

        // filter
        associatedScopeEdgeStream().iterator()
                .forEachRemaining(tuple -> result.put(tuple.scope(), tuple.label(), tuple.occurrence()));
       
        final IRelation3.Transient<O, L, S> inverse = (IRelation3.Transient<O, L, S>) result.inverse();
        
        return inverse.freeze();
    }

    @Deprecated
    @Override
    public IRelation3.Immutable<S, L, O> getImportEdges() {
        final IRelation3.Transient<S, L, O> result = HashTrieRelation3.Transient.of();

        // filter
        requireImportEdgeStream().iterator()
                .forEachRemaining(tuple -> result.put(tuple.scope(), tuple.label(), tuple.occurrence()));

        return result.freeze();
    }

    public IEsopNameResolution<S, L, O> resolve(IResolutionParameters<L> params, Function1<S, String> tracer) {
        final IEsopNameResolution<S, L, O> one = new PersistentNameResolution<>(this, params);
        final IEsopNameResolution<S, L, O> two = new AllShortestPathsNameResolution<>(this, params);
        
        // return new BiSimulationNameResolution<>(one, two);
        // return one;
        return two;
    }

    public static class Builder<S extends IScope, L extends ILabel, O extends IOccurrence, V>
            implements IEsopScopeGraph.Transient<S, L, O, V> {

        private final Set.Transient<S> allScopes;
        private final Set.Transient<O> allDeclarations;
        private final Set.Transient<O> allReferences;

        private final IFunction.Transient<O, S> declarations;
        private final IFunction.Transient<O, S> references;

        private final IRelation3.Transient<S, L, S> directEdges;
        private final IRelation3.Transient<O, L, S> assocEdges;
        private final IRelation3.Transient<S, L, O> importEdges;
        
        private final IRelation3.Transient<S, L, V> incompleteDirectEdges;
        private final IRelation3.Transient<S, L, V> incompleteImportEdges;
       
        private IEsopScopeGraph.Immutable<S, L, O, V> result = null;

        public Builder() {
            this.allScopes = Set.Transient.of();
            this.allDeclarations = Set.Transient.of();
            this.allReferences = Set.Transient.of();

            this.declarations = HashTrieFunction.Transient.of();
            this.references = HashTrieFunction.Transient.of();

            this.directEdges = HashTrieRelation3.Transient.of();
            this.assocEdges = HashTrieRelation3.Transient.of();
            this.importEdges = HashTrieRelation3.Transient.of();
            
            this.incompleteDirectEdges = HashTrieRelation3.Transient.of();
            this.incompleteImportEdges = HashTrieRelation3.Transient.of();
        }
        
        private Builder(
                Set.Transient<S> allScopes, 
                Set.Transient<O> allDeclarations, 
                Set.Transient<O> allReferences,
                IFunction.Transient<O, S> declarations, 
                IFunction.Transient<O, S> references,
                IRelation3.Transient<S, L, S> directEdges,
                IRelation3.Transient<O, L, S> assocEdges,
                IRelation3.Transient<S, L, O> importEdges,
                IRelation3.Transient<S, L, V> incompleteDirectEdges,
                IRelation3.Transient<S, L, V> incompleteImportEdges) {
            this.allScopes = allScopes;
            this.allDeclarations = allDeclarations;
            this.allReferences = allReferences;

            this.declarations = declarations;
            this.references = references;

            this.directEdges = directEdges;
            this.assocEdges = assocEdges;
            this.importEdges = importEdges;        
            
            this.incompleteDirectEdges = incompleteDirectEdges;
            this.incompleteImportEdges = incompleteImportEdges;
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
        
        public boolean addDirectEdge(S sourceScope, L label, S targetScope) {
            requireNonSealed();

            allScopes.__insert(sourceScope);
            allScopes.__insert(targetScope);
            return directEdges.put(sourceScope, label, targetScope);
        }
        
        public boolean addDecl(S scope, O decl) {
            requireNonSealed();            
            
            allScopes.__insert(scope);
            allDeclarations.__insert(decl);
            return declarations.put(decl, scope);
        }
        
        public boolean addAssoc(O decl, L label, S scope) {
            requireNonSealed();
            requireNonEqual(Label.P, label);
            
            allScopes.__insert(scope);
            allDeclarations.__insert(decl);
            return assocEdges.put(decl, label, scope);
        }      
        
        public boolean addRef(O ref, S scope) {
            requireNonSealed();
            
            allScopes.__insert(scope);
            allReferences.__insert(ref);
            return references.put(ref, scope);
        }

        public boolean addImportEdge(S scope, L label, O ref) {
            requireNonSealed();
            requireNonEqual(Label.R, label);
            
            allScopes.__insert(scope);
            allReferences.__insert(ref);
            return importEdges.put(scope, label, ref);
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
        public IEsopScopeGraph.Immutable<S, L, O, V> freeze() {
            if (result == null) {
                final EsopScopeGraph.Transient<S, L, O, V> one = EsopScopeGraph.Transient.of();
                               
                declarations.keySet().forEach(o -> one.addDecl(declarations.get(o).get(), o));
                references.keySet().forEach(o -> one.addRef(o, references.get(o).get()));

                directEdges.stream(ImmutableScopeLabelScope::of)
                        .forEach(sls -> one.addDirectEdge(sls.sourceScope(), sls.label(), sls.targetScope()));

                assocEdges.stream(ImmutableOccurrenceLabelScope::of)
                        .forEach(slo -> one.addExportEdge(slo.occurrence(), slo.label(), slo.scope()));

                importEdges.stream(ImmutableScopeLabelOccurrence::of)
                        .forEach(slo -> one.addImportEdge(slo.scope(), slo.label(), slo.occurrence()));
                
                incompleteDirectEdges.stream()
                        .forEach(slo -> one.addIncompleteDirectEdge(slo._1(), slo._2(), slo._3()));
                
                incompleteImportEdges.stream()
                        .forEach(slo -> one.addIncompleteImportEdge(slo._1(), slo._2(), slo._3()));                
                                                                
                final IEsopScopeGraph.Immutable<S, L, O, V> two = new PersistentScopeGraph<>(allScopes.freeze(), allDeclarations.freeze(),
                        allReferences.freeze(), declarations.freeze(), references.freeze(), directEdges.freeze(), assocEdges.freeze(), importEdges.freeze(), incompleteDirectEdges.freeze(), incompleteImportEdges.freeze());
                
                result = new BiSimulationScopeGraph<>(one.freeze(), two);                
            }

            return result;
        }

        @Override
        public boolean isOpen(S scope, L label) {
            return incompleteDirectEdges().contains(scope, label) || incompleteImportEdges().contains(scope, label);
        }

        @Override
        public IRelation3<S, L, V> incompleteDirectEdges() {
            return incompleteDirectEdges;
        }

        @Override
        public IRelation3<S, L, V> incompleteImportEdges() {
            return incompleteImportEdges;
        }

        @Override
        public boolean isComplete() {
            return incompleteDirectEdges().isEmpty() && incompleteImportEdges().isEmpty();
        }

        @Override
        public boolean addIncompleteDirectEdge(S scope, L label, V var) {
            return incompleteDirectEdges.put(scope, label, var);
        }

        @Override
        public boolean addExportEdge(O decl, L label, S scope) {
            return addAssoc(decl, label, scope);
        }

        @Override
        public boolean addIncompleteImportEdge(S scope, L label, V var) {
            return incompleteImportEdges.put(scope, label, var);
        }

        @Override
        public boolean addAll(IEsopScopeGraph<S, L, O, V> other) {
            boolean change = false;
            
            change |= other.getDecls().stream()
                    .map(tuple -> this.addDecl(tuple._2(), tuple._1()))
                    .reduce(Boolean::logicalOr)
                    .orElse(false);
            
            change |= other.getRefs().stream()
                    .map(tuple -> this.addRef(tuple._1(), tuple._2()))
                    .reduce(Boolean::logicalOr)
                    .orElse(false);

            change |= other.getDirectEdges().stream()
                    .map(tuple -> this.addDirectEdge(tuple._1(), tuple._2(), tuple._3()))
                    .reduce(Boolean::logicalOr)
                    .orElse(false);

            change |= other.getExportEdges().stream()
                    .map(tuple -> this.addExportEdge(tuple._1(), tuple._2(), tuple._3()))
                    .reduce(Boolean::logicalOr)
                    .orElse(false);

            change |= other.getImportEdges().stream()
                    .map(tuple -> this.addImportEdge(tuple._1(), tuple._2(), tuple._3()))
                    .reduce(Boolean::logicalOr)
                    .orElse(false);

            change |= other.incompleteDirectEdges().stream()
                    .map(tuple -> this.addIncompleteDirectEdge(tuple._1(), tuple._2(), tuple._3()))
                    .reduce(Boolean::logicalOr)
                    .orElse(false);
            
            change |= other.incompleteImportEdges().stream()
                    .map(tuple -> this.addIncompleteImportEdge(tuple._1(), tuple._2(), tuple._3()))
                    .reduce(Boolean::logicalOr)
                    .orElse(false);
           
            return change;            
        }

        @Override
        public boolean reduce(PartialFunction1<V, S> fs, PartialFunction1<V, O> fo) {
            boolean progress = false;
            progress |= reduce(incompleteDirectEdges, fs, this::addDirectEdge);
            progress |= reduce(incompleteImportEdges, fo, this::addImportEdge);
            return progress;
        }

        private <X> boolean reduce(IRelation3.Transient<S, L, V> relation, PartialFunction1<V, X> f,
                Predicate3<S, L, X> add) {
            return relation.stream().flatMap(slv -> {
                return f.apply(slv._3()).map(x -> {
                    add.test(slv._1(), slv._2(), x);
                    return Stream.of(slv);
                }).orElse(Stream.empty());
            }).map(slv -> {
                return relation.remove(slv._1(), slv._2(), slv._3());
            }).findAny().isPresent();
        }
    }

    @Override
    public boolean isOpen(S scope, L label) {
        return incompleteDirectEdges().contains(scope, label) || incompleteImportEdges().contains(scope, label);
    }

    @Override
    public IRelation3.Immutable<S, L, V> incompleteDirectEdges() {
        return incompleteDirectEdges;
    }

    @Override
    public IRelation3.Immutable<S, L, V> incompleteImportEdges() {
        return incompleteImportEdges;
    }

    @Override
    public boolean isComplete() {
        return incompleteDirectEdges().isEmpty() && incompleteImportEdges().isEmpty();
    }
    
    @Override
    public org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph.Transient<S, L, O, V> melt() {       
        return new PersistentScopeGraph.Builder<>(
                this.getAllScopes().asTransient(),
                this.getAllDecls().asTransient(),
                this.getAllRefs().asTransient(),
                this.getDecls().melt(),
                this.getRefs().melt(),
                this.getDirectEdges().melt(),
                this.getExportEdges().melt(),
                this.getImportEdges().melt(),
                this.incompleteDirectEdges.melt(), 
                this.incompleteImportEdges.melt()                
        );
    }
}

class CollectionConverter {

    // TODO: release Capsule and change input type to BinaryRelation.Immutable
    public static final <T, U> IFunction<T, U> relationToHashFunction(BinaryRelation<T, U> input) {
        final IFunction.Transient<T, U> output = HashTrieFunction.Transient.of();
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
        final IRelation3.Transient<T, U, V> output = HashTrieRelation3.Transient.of();
        input.keySet().forEach(key -> output.put(key, intermediate, input.get(key).get()));
        return output;
    }
    
    public static final <T extends IScope, U extends ILabel, V extends IOccurrence> IRelation3.Immutable<T, U, V> union(IRelation3<T, U, V> one, IRelation3<T, U, V> two) {
        final IRelation3.Transient<T, U, V> result = HashTrieRelation3.Transient.of();

        one.stream(ImmutableScopeLabelOccurrence::of).iterator().forEachRemaining(tuple -> result.put(tuple.scope(), tuple.label(), tuple.occurrence()));
        two.stream(ImmutableScopeLabelOccurrence::of).iterator().forEachRemaining(tuple -> result.put(tuple.scope(), tuple.label(), tuple.occurrence()));

        return result.freeze();
    }
    
}
