package org.metaborg.meta.nabl2.scopegraph.esop;

import static org.metaborg.meta.nabl2.scopegraph.esop.persistent.CollectionConverter.liftHashFunctionToRelation;
import static org.metaborg.meta.nabl2.scopegraph.esop.persistent.CollectionConverter.union;
import static org.metaborg.meta.nabl2.util.tuples.HasLabel.labelEquals;
import static org.metaborg.meta.nabl2.util.tuples.HasOccurrence.occurrenceEquals;

import java.util.stream.Stream;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.IScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.esop.persistent.AllShortestPathsNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.persistent.BiSimulationNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.persistent.BiSimulationScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.esop.persistent.PersistentNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.persistent.PersistentScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.esop.reference.EsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.reference.EsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.util.collections.HashTrieRelation3;
import org.metaborg.meta.nabl2.util.collections.IRelation3;
import org.metaborg.meta.nabl2.util.tuples.ImmutableOccurrenceLabelScope;
import org.metaborg.meta.nabl2.util.tuples.ImmutableScopeLabelOccurrence;
import org.metaborg.meta.nabl2.util.tuples.ImmutableScopeLabelScope;
import org.metaborg.meta.nabl2.util.tuples.OccurrenceLabelScope;
import org.metaborg.meta.nabl2.util.tuples.ScopeLabelOccurrence;
import org.metaborg.meta.nabl2.util.tuples.ScopeLabelScope;
import org.metaborg.util.functions.PartialFunction1;

import com.google.common.annotations.Beta;

import io.usethesource.capsule.BinaryRelation;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;
import io.usethesource.capsule.util.stream.CapsuleCollectors;

@Beta
public interface IEsopScopeGraph<S extends IScope, L extends ILabel, O extends IOccurrence, V>
        extends IScopeGraph<S, L, O> {

    public static final boolean USE_PERSISTENT_SCOPE_GRAPH = Boolean.getBoolean("usePersistentScopeGraph");

    /*
     * Factory method to switch between different scope graph implementations.
     */
    static <S extends IScope, L extends ILabel, O extends IOccurrence, V> IEsopScopeGraph.Transient<S, L, O, V> builder() {
        if (USE_PERSISTENT_SCOPE_GRAPH) {
            // return new PersistentScopeGraph.Builder<>();
            
            IEsopScopeGraph.Immutable<S, L, O, V> one = EsopScopeGraph.Immutable.of();
            IEsopScopeGraph.Immutable<S, L, O, V> two = new PersistentScopeGraph<>();
            
            // return new BiSimulationScopeGraph<>(one, two).melt();
            
            return two.melt();
        } else {
            return EsopScopeGraph.Transient.of();
        }
    }

    boolean isOpen(S scope, L label);

    IRelation3<S, L, V> incompleteDirectEdges();

    IRelation3<S, L, V> incompleteImportEdges();

    boolean isComplete();

    // default Stream<OccurrenceLabelScope<O, L, S>> referenceEdgeStream() {
    // return liftHashFunctionToRelation(this.getRefs(), (L)
    // Label.R).stream(ImmutableOccurrenceLabelScope::of);
    // }

    default IRelation3.Immutable<O, L, S> sourceEdges() {
        return (IRelation3.Immutable<O, L, S>) union(this.getExportEdges().inverse(),
                liftHashFunctionToRelation(this.getRefs(), (L) Label.R).inverse()).inverse();
    }

    default IRelation3.Immutable<S, L, S> middleEdges() {
        final IRelation3.Transient<S, L, S> result = HashTrieRelation3.Transient.of();    
        this.getDirectEdges().stream().iterator().forEachRemaining(tuple -> result.put(tuple._1(), tuple._2(), tuple._3()));
        return result.freeze();
    }

    default IRelation3.Immutable<S, L, O> targetEdges() {
        return union(this.getImportEdges(), liftHashFunctionToRelation(this.getDecls(), (L) Label.D).inverse());
    }

    default Stream<OccurrenceLabelScope<O, L, S>> sourceEdgeStream() {
        return sourceEdges().stream(ImmutableOccurrenceLabelScope::of);
    }

    default Stream<OccurrenceLabelScope<O, L, S>> referenceEdgeStream() {
        // TODO: use hash lookup on label instead of filter
        return sourceEdgeStream().filter(labelEquals(Label.R));
    }

    default Stream<OccurrenceLabelScope<O, L, S>> associatedScopeEdgeStream() {
        // TODO: use hash lookup on label instead of filter
        return sourceEdgeStream().filter(labelEquals(Label.R).negate());
    }

    default Stream<ScopeLabelScope<S, L, O>> middleEdgeStream() {
        return middleEdges().stream(ImmutableScopeLabelScope::of);
    }

    default Stream<ScopeLabelOccurrence<S, L, O>> targetEdgeStream() {
        return targetEdges().stream(ImmutableScopeLabelOccurrence::of);
    }

    default Stream<ScopeLabelOccurrence<S, L, O>> declarationEdgeStream() {
        // TODO: use hash lookup on label instead of filter
        return targetEdgeStream().filter(labelEquals(Label.D));
    }

    default Stream<ScopeLabelOccurrence<S, L, O>> requireImportEdgeStream() {
        // TODO: use hash lookup on label instead of filter
        return targetEdgeStream().filter(labelEquals(Label.D).negate());
    }
    
    default SetMultimap.Immutable<S, L> importSourceScopes(O importReference) {
        final SetMultimap.Transient<S, L> builder = SetMultimap.Transient.of(); 
        
        requireImportEdgeStream()
                .filter(occurrenceEquals(importReference))
                .forEach(importEdge -> builder.__insert(importEdge.scope(), importEdge.label()));
                
        return builder.freeze();
    }
    
    default Set.Immutable<S> importSourceScopes(O importReference, L importLabel) {
        return requireImportEdgeStream()
                .filter(occurrenceEquals(importReference))
                .filter(labelEquals(importLabel))
                .map(ScopeLabelOccurrence::scope)
                .collect(CapsuleCollectors.toSet());
    }

    default SetMultimap.Immutable<L, S> importTargetScopes(O importDeclaration) {
        final SetMultimap.Transient<L, S> builder = SetMultimap.Transient.of(); 
        
        associatedScopeEdgeStream()
                .filter(occurrenceEquals(importDeclaration))
                .forEach(importEdge -> builder.__insert(importEdge.label(), importEdge.scope()));
                
        return builder.freeze();
    }
    
    default Set.Immutable<S> importTargetScopes(O importDeclaration, L importLabel) {
        return associatedScopeEdgeStream()
                .filter(occurrenceEquals(importDeclaration))
                .filter(labelEquals(importLabel))
                .map(OccurrenceLabelScope::scope)
                .collect(CapsuleCollectors.toSet());
    }

    interface Immutable<S extends IScope, L extends ILabel, O extends IOccurrence, V>
            extends IEsopScopeGraph<S, L, O, V>, IScopeGraph.Immutable<S, L, O> {

        IRelation3.Immutable<S, L, V> incompleteDirectEdges();

        IRelation3.Immutable<S, L, V> incompleteImportEdges();

        IEsopScopeGraph.Transient<S, L, O, V> melt();

    }

    interface Transient<S extends IScope, L extends ILabel, O extends IOccurrence, V>
            extends IEsopScopeGraph<S, L, O, V> {

        boolean addDecl(S scope, O decl);

        boolean addRef(O ref, S scope);

        boolean addDirectEdge(S sourceScope, L label, S targetScope);

        boolean addIncompleteDirectEdge(S scope, L label, V var);

        boolean addExportEdge(O decl, L label, S scope);

        boolean addImportEdge(S scope, L label, O ref);

        boolean addIncompleteImportEdge(S scope, L label, V var);

        boolean addAll(IEsopScopeGraph<S, L, O, V> other);

        boolean reduce(PartialFunction1<V, S> fs, PartialFunction1<V, O> fo);

        // -----------------------

        IEsopScopeGraph.Immutable<S, L, O, V> freeze();

    }

}
