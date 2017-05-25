package org.metaborg.meta.nabl2.scopegraph.esop.persistent;

import static org.metaborg.meta.nabl2.util.tuples.ScopeLabelOccurrence.occurrenceEquals;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.metaborg.meta.nabl2.regexp.IRegExpMatcher;
import org.metaborg.meta.nabl2.regexp.RegExpMatcher;
import org.metaborg.meta.nabl2.relations.IRelation;
import org.metaborg.meta.nabl2.relations.RelationDescription;
import org.metaborg.meta.nabl2.relations.terms.Relation;
import org.metaborg.meta.nabl2.scopegraph.IActiveScopes;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.path.IDeclPath;
import org.metaborg.meta.nabl2.scopegraph.path.IPath;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;
import org.metaborg.meta.nabl2.scopegraph.terms.path.Paths;
import org.metaborg.meta.nabl2.util.tuples.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;

import com.google.common.annotations.Beta;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.Set.Immutable;

public class PersistentNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements IEsopNameResolution<S, L, O>, Serializable {

    private static final long serialVersionUID = 42L;

    private final PersistentScopeGraph<S, L, O> scopeGraph;

    private final Set.Immutable<L> labels;
    private final L labelD;
    private final IRegExpMatcher<L> wf;

    private final IRelation.Immutable<L> ordered;
    private final IRelation.Immutable<L> unordered;

    private final IActiveScopes<S, L> scopeCounter;

    transient private Map<O, IPersistentEnvironment<S, L, O, IResolutionPath<S, L, O>>> resolutionCache;

    transient private Map<S, IPersistentEnvironment<S, L, O, IDeclPath<S, L, O>>> visibilityCache;
    transient private Map<S, IPersistentEnvironment<S, L, O, IDeclPath<S, L, O>>> reachabilityCache;

    transient private Map<IRelation<L>, EnvironmentBuilder<S, L, O>> environmentBuilderCache;

    public PersistentNameResolution(PersistentScopeGraph<S, L, O> scopeGraph, IResolutionParameters<L> params,
            IActiveScopes<S, L> scopeCounter) {
        this.scopeGraph = scopeGraph;

        this.labels = Set.Immutable.<L>of().__insertAll(Sets.newHashSet(params.getLabels()));
        this.labelD = params.getLabelD();
        this.wf = RegExpMatcher.create(params.getPathWf());
        this.ordered = params.getSpecificityOrder();
        assert ordered.getDescription().equals(
                RelationDescription.STRICT_PARTIAL_ORDER) : "Label specificity order must be a strict partial order";
        this.unordered = Relation.Immutable.of(RelationDescription.STRICT_PARTIAL_ORDER);
        this.scopeCounter = scopeCounter;

        initTransients();
        
        // stage and cache environment builders
        getEnvironmentBuilder(ordered);
        getEnvironmentBuilder(unordered);
    }

    private void initTransients() {
        this.resolutionCache = Maps.newHashMap();
        this.visibilityCache = Maps.newHashMap();
        this.reachabilityCache = Maps.newHashMap();                
        this.environmentBuilderCache = Maps.newHashMap();
    }

    @Beta
    public final Set.Immutable<L> getLabels() {
        return labels;
    }    
    
    @Beta
    public final PersistentScopeGraph<S, L, O> getScopeGraph() {
        return scopeGraph;
    }

    @Beta
    public final L getLabelD() {
        return labelD;
    }

    @Beta
    public final IActiveScopes<S, L> getScopeCounter() {
        return scopeCounter;
    }
    
    @Beta
    public final IRelation<L> getOrdered() {
        return ordered;
    }

    @Beta
    public final IRelation<L> getUnordered() {
        return unordered;
    }

    @Beta
    public final IRegExpMatcher<L> getWf() {
        return wf;
    }

    // NOTE: never used in project
    @Deprecated
    @Override
    public Set.Immutable<S> getAllScopes() {
        return scopeGraph.getAllScopes();
    }

    // NOTE: all references could be duplicated to get rid of scope graph
    // reference
    @Override
    public Set.Immutable<O> getAllRefs() {
        return scopeGraph.getAllRefs();
    }

    @Override
    public Set.Immutable<IResolutionPath<S, L, O>> resolve(O ref) {
        return tryResolve(ref).map(Tuple2::_1).orElse(Set.Immutable.of());
    }

    @Override
    public Set.Immutable<IDeclPath<S, L, O>> visible(S scope) {
        return tryVisible(scope).map(Tuple2::_1).orElse(Set.Immutable.of());
    }

    @Override
    public Set.Immutable<IDeclPath<S, L, O>> reachable(S scope) {
        return tryReachable(scope).map(Tuple2::_1).orElse(Set.Immutable.of());
    }
    
    public Optional<Tuple2<Set.Immutable<IResolutionPath<S, L, O>>, Set.Immutable<String>>> tryResolve(O reference) {
        final IPersistentEnvironment<S, L, O, IResolutionPath<S, L, O>> environment = resolutionCache
                .computeIfAbsent(reference, r -> resolveEnvironment(Set.Immutable.of(), r, this));

        return environment.solution().map(paths -> ImmutableTuple2.of(paths, Set.Immutable.of()));
    }

    public Optional<Tuple2<Immutable<IDeclPath<S, L, O>>, Set.Immutable<String>>> tryVisible(S scope) {
        final IPersistentEnvironment<S, L, O, IDeclPath<S, L, O>> environment = visibilityCache.computeIfAbsent(scope,
                s -> visibleEnvironment(s, this));

        return environment.solution().map(paths -> ImmutableTuple2.of(paths, Set.Immutable.of()));
    }

    public Optional<Tuple2<Set.Immutable<IDeclPath<S, L, O>>, Set.Immutable<String>>> tryReachable(S scope) {
        final IPersistentEnvironment<S, L, O, IDeclPath<S, L, O>> env = reachabilityCache.computeIfAbsent(scope,
                s -> reachableEnvironment(s, this));

        return env.solution().map(paths -> ImmutableTuple2.of(paths, Set.Immutable.of()));
    }

    private static final <S extends IScope, L extends ILabel, O extends IOccurrence> IPersistentEnvironment<S, L, O, IDeclPath<S, L, O>> visibleEnvironment(final S scope, final PersistentNameResolution<S, L, O> nameResolution) {
        return buildEnvironment(
                Set.Immutable.of(), 
                nameResolution.getOrdered(), 
                nameResolution.getWf(), 
                Paths.empty(scope),
                Environments.identityFilter(), 
                Optional.empty(), nameResolution, false);
    }

    private static final <S extends IScope, L extends ILabel, O extends IOccurrence> IPersistentEnvironment<S, L, O, IDeclPath<S, L, O>> reachableEnvironment(final S scope, final PersistentNameResolution<S, L, O> nameResolution) {   
        return buildEnvironment(
                Set.Immutable.of(), 
                nameResolution.getUnordered(), 
                nameResolution.getWf(), 
                Paths.empty(scope),
                Environments.identityFilter(), 
                Optional.empty(), nameResolution, false);
    }
    
    static final <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, IResolutionPath<S, L, O>> resolveEnvironment(
            final Set.Immutable<O> seenImports, final O reference,
            /***/
            PersistentNameResolution<S, L, O> nameResolution) {
        
        final PersistentScopeGraph<S, L, O> scopeGraph = nameResolution.getScopeGraph();
        
        // TODO: use hash lookup on occurrence instead of filter

        final Set.Immutable<O> nextSeenImports = seenImports.__insert(reference);
        final IPersistentEnvironment.Filter<S, L, O, IResolutionPath<S, L, O>> nextFilter = Environments
                .resolutionFilter(reference);
        final Optional<O> nextReference = Optional.of(reference);
        
        // EXPERIMENTAL
        boolean eagerEvaluation = false;
        
        // @formatter:off
        IPersistentEnvironment<S, L, O, IResolutionPath<S, L, O>> environment = scopeGraph.localReferencesStream()
            .filter(occurrenceEquals(reference))
            .findAny() // must be unique (TODO ensure this)
            .map(tuple -> tuple.scope())
            .map(scope -> buildEnvironment(nextSeenImports, nameResolution.getOrdered(), nameResolution.getWf(), Paths.empty(scope), nextFilter, nextReference, nameResolution, eagerEvaluation))
            .orElse(Environments.empty());
        // @formatter:on 
        
        return environment;
    }

    /**
     * Calculate new environment if path is well-formed, otherwise return an
     * empty environment.
     */
    static final <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> buildEnvironment(
            Set.Immutable<O> seenImports,
            IRelation<L> lt, IRegExpMatcher<L> re, IScopePath<S, L, O> path,
            IPersistentEnvironment.Filter<S, L, O, P> filter,
            Optional<O> resolutionReference,
            PersistentNameResolution<S, L, O> nameResolution,
            boolean eagerEvaluation) {
        if (re.isEmpty()) {
            return Environments.empty();
        } else {
            final EnvironmentBuilder<S, L, O> builder = nameResolution.getEnvironmentBuilder(lt);

            final IPersistentEnvironment<S, L, O, P> environment = builder.build(builder, seenImports, re, path, filter,
                    Maps.newHashMap(), resolutionReference, nameResolution, eagerEvaluation);
            
            return environment;
        }
    }

    /**
     * Returns the set of declarations that are reachable from S with a
     * l-labeled step.
     */
    public static <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> env_l(
            Set.Immutable<O> seenImports,
            IRelation<L> lt, IRegExpMatcher<L> re, L l, IScopePath<S, L, O> path,
            IPersistentEnvironment.Filter<S, L, O, P> filter,
            /***/
            PersistentNameResolution<S, L, O> nameResolution) {

        return Environments.guarded((PartialFunction0<IPersistentEnvironment<S, L, O, P>> & Serializable) () -> {
            // NOTE: capturing immutable state: scopeGraph, labelD
            // NOTE: capturing mutable state: scopeCounter

            final PersistentScopeGraph<S, L, O> scopeGraph = nameResolution.getScopeGraph();
            final IActiveScopes<S, L> scopeCounter = nameResolution.getScopeCounter();
            final L labelD = nameResolution.getLabelD();
            
            if (scopeCounter.isOpen(path.getTarget(), l)) {
                return Optional.empty(); // no solution available currently
            }

            final IPersistentEnvironment<S, L, O, P> result;

            if (l.equals(labelD)) {
                // case: env_D

                if (!re.isAccepting()) {
                    result = Environments.empty();
                } else {
                    // @formatter:off
                    final Set.Immutable<P> paths = scopeGraph.localDeclarationsStream()
                        .filter(scopeEquals(path.getTarget()))
                        .map(tuple -> tuple.occurrence())
                        .flatMap(declaration -> OptionalStream.of(filter.test(Paths.decl(path, declaration))))
                        .collect(CapsuleCollectors.toSet());
                    // @formatter:on

                    result = Environments.eager(paths);
                }

            } else {
                // case: env_nonD

                final IRegExpMatcher<L> nextRe = re.match(l);

                if (nextRe.isEmpty()) {
                    // TODO check if importScopes calculation can be pruned as
                    // well
                    result = Environments.empty();
                } else {
                    final Function<IScopePath<S, L, O>, IPersistentEnvironment<S, L, O, P>> getter = p -> env(
                            seenImports, lt, nextRe, p, filter, nameResolution);

                    final Set.Immutable<IPersistentEnvironment<S, L, O, P>> directScopes = directScopes(seenImports, l,
                            path, filter, getter, nameResolution);
                    final Set.Immutable<IPersistentEnvironment<S, L, O, P>> importScopes = importScopes(seenImports, l,
                            path, filter, getter, nameResolution);

                    // TODO: add union to Capsule
                    final Set.Immutable<IPersistentEnvironment<S, L, O, P>> scopes = directScopes
                            .__insertAll(importScopes);
                    result = Environments.union(scopes);
                }
            }

            return Optional.of(result);
        });
    }
        
    /**
     * Retrieves an environment builder for for a relation of labels. 
     */
    public EnvironmentBuilder<S, L, O> getEnvironmentBuilder(final IRelation<L> lt) {
        return environmentBuilderCache.computeIfAbsent(lt, key -> EnvironmentBuilders.stage(key, labels));
    }
    
    // serialization

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        initTransients();
    }

}

class OptionalStream {

    public static final <T> Stream<T> of(Optional<T> optional) {
        if (optional.isPresent()) {
            return Stream.of(optional.get());
        } else {
            return Stream.empty();
        }
    }

}
