package org.metaborg.meta.nabl2.scopegraph.esop.persistent;

import static org.metaborg.meta.nabl2.util.tuples.HasLabel.labelEquals;
import static org.metaborg.meta.nabl2.util.tuples.ScopeLabelOccurrence.occurrenceEquals;
import static org.metaborg.meta.nabl2.util.tuples.ScopeLabelOccurrence.scopeEquals;
import static org.metaborg.meta.nabl2.util.tuples.ScopeLabelScope.sourceScopeEquals;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.metaborg.meta.nabl2.regexp.IRegExpMatcher;
import org.metaborg.meta.nabl2.regexp.RegExpMatcher;
import org.metaborg.meta.nabl2.relations.IRelation;
import org.metaborg.meta.nabl2.relations.RelationDescription;
import org.metaborg.meta.nabl2.relations.terms.Relation;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.OpenCounter;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.path.IDeclPath;
import org.metaborg.meta.nabl2.scopegraph.path.IPath;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;
import org.metaborg.meta.nabl2.scopegraph.terms.path.Paths;
import org.metaborg.meta.nabl2.util.functions.Function0;
import org.metaborg.meta.nabl2.util.functions.PartialFunction0;
import org.metaborg.meta.nabl2.util.tuples.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;

import com.google.common.annotations.Beta;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.Set.Immutable;
import io.usethesource.capsule.util.stream.CapsuleCollectors;

public class PersistentNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements IEsopNameResolution<S, L, O>, Serializable {

    private static final long serialVersionUID = 42L;

    private final PersistentScopeGraph<S, L, O> scopeGraph;

    private final Set.Immutable<L> labels;
    private final L labelD;
    private final IRegExpMatcher<L> wf;

    private final IRelation<L> ordered;
    private final IRelation<L> unordered;

    private final OpenCounter<S, L> scopeCounter;

    transient private Map<O, IPersistentEnvironment<S, L, O, IResolutionPath<S, L, O>>> resolutionCache;

    transient private Map<S, IPersistentEnvironment<S, L, O, IDeclPath<S, L, O>>> visibilityCache;
    transient private Map<S, IPersistentEnvironment<S, L, O, IDeclPath<S, L, O>>> reachabilityCache;

    transient private Map<IRelation<L>, EnvironmentBuilder<S, L, O>> stagedEnv_L;

    public PersistentNameResolution(PersistentScopeGraph<S, L, O> scopeGraph, IResolutionParameters<L> params,
            OpenCounter<S, L> scopeCounter) {
        this.scopeGraph = scopeGraph;

        this.labels = Set.Immutable.<L>of().__insertAll(Sets.newHashSet(params.getLabels()));
        this.labelD = params.getLabelD();
        this.wf = RegExpMatcher.create(params.getPathWf());
        this.ordered = params.getSpecificityOrder();
        assert ordered.getDescription().equals(
                RelationDescription.STRICT_PARTIAL_ORDER) : "Label specificity order must be a strict partial order";
        this.unordered = new Relation<>(RelationDescription.STRICT_PARTIAL_ORDER);
        this.scopeCounter = scopeCounter;

        initTransients();
    }

    private void initTransients() {
        this.resolutionCache = Maps.newHashMap();
        this.visibilityCache = Maps.newHashMap();
        this.reachabilityCache = Maps.newHashMap();
        this.stagedEnv_L = Maps.newHashMap();
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
    public final OpenCounter<S, L> getScopeCounter() {
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
        return env(
                Set.Immutable.of(), 
                nameResolution.getOrdered(), 
                nameResolution.getWf(), 
                Paths.empty(scope),
                Environments.identityFilter(), 
                nameResolution);
    }

    private static final <S extends IScope, L extends ILabel, O extends IOccurrence> IPersistentEnvironment<S, L, O, IDeclPath<S, L, O>> reachableEnvironment(final S scope, final PersistentNameResolution<S, L, O> nameResolution) {   
        return env(
                Set.Immutable.of(), 
                nameResolution.getUnordered(), 
                nameResolution.getWf(), 
                Paths.empty(scope),
                Environments.identityFilter(), 
                nameResolution);
    }

    private static final <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, IResolutionPath<S, L, O>> resolveEnvironment(
            final Set.Immutable<O> seenImports, final O reference,
            /***/
            PersistentNameResolution<S, L, O> nameResolution) {

        final PersistentScopeGraph<S, L, O> scopeGraph = nameResolution.getScopeGraph();
        
        // TODO: use hash lookup on occurrence instead of filter

        final Set.Immutable<O> nextSeenImports = seenImports.__insert(reference);
        final IPersistentEnvironment.Filter<S, L, O, IResolutionPath<S, L, O>> nextFilter = Environments
                .resolutionFilter(reference);

        // @formatter:off
        IPersistentEnvironment<S, L, O, IResolutionPath<S, L, O>> environment = scopeGraph.localReferencesStream()
            .filter(occurrenceEquals(reference))
            .findAny() // must be unique (TODO ensure this)
            .map(tuple -> tuple.scope())
            .map(scope -> env(nextSeenImports, nameResolution.getOrdered(), nameResolution.getWf(), Paths.empty(scope), nextFilter, nameResolution))
            .orElse(Environments.empty());
        // @formatter:on 
        
        return environment;
    }

    /**
     * Calculate new environment if path is well-formed, otherwise return an
     * empty environment.
     */
    private static final <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> env(
            Set.Immutable<O> seenImports,
            IRelation<L> lt, IRegExpMatcher<L> re, IScopePath<S, L, O> path,
            IPersistentEnvironment.Filter<S, L, O, P> filter,
            /***/
            PersistentNameResolution<S, L, O> nameResolution) {
        if (re.isEmpty()) {
            return Environments.empty();
        } else {          
            final Supplier<EnvironmentBuilder<S, L, O>> lazyEnvironment = () -> stageEnvironments(nameResolution, lt);
            
            final EnvironmentBuilder<S, L, O> stagedEnvironment = nameResolution.getOrCacheStagedEnvironment(lt, lazyEnvironment);
            
            return stagedEnvironment.build(seenImports, re, path, filter, Maps.newHashMap(), nameResolution);
        }
    }

    /**
     * Returns the set of declarations that are reachable from S with a
     * l-labeled step.
     */
    private static <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> env_l(
            Set.Immutable<O> seenImports,
            IRelation<L> lt, IRegExpMatcher<L> re, L l, IScopePath<S, L, O> path,
            IPersistentEnvironment.Filter<S, L, O, P> filter,
            /***/
            PersistentNameResolution<S, L, O> nameResolution) {

        return Environments.guarded((PartialFunction0<IPersistentEnvironment<S, L, O, P>> & Serializable) () -> {
            // NOTE: capturing immutable state: scopeGraph, labelD
            // NOTE: capturing mutable state: scopeCounter

            final PersistentScopeGraph<S, L, O> scopeGraph = nameResolution.getScopeGraph();
            final OpenCounter<S, L> scopeCounter = nameResolution.getScopeCounter();
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

    private static final <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> Set.Immutable<IPersistentEnvironment<S, L, O, P>> directScopes(
            Set.Immutable<O> seenImports, L l, IScopePath<S, L, O> path,
            IPersistentEnvironment.Filter<S, L, O, P> filter,
            Function<IScopePath<S, L, O>, IPersistentEnvironment<S, L, O, P>> getter,
            /***/
            PersistentNameResolution<S, L, O> nameResolution) {

        final PersistentScopeGraph<S, L, O> scopeGraph = nameResolution.getScopeGraph();
       
        final Function<S, Optional<IScopePath<S, L, O>>> extendPathToNextScopeAndValidate = nextScope -> Paths
                .append(path, Paths.direct(path.getTarget(), l, nextScope));

        // @formatter:off
        final Set.Immutable<IPersistentEnvironment<S, L, O, P>> environments = scopeGraph.directEdgesStream()
            .filter(labelEquals(l))
            .filter(sourceScopeEquals(path.getTarget()))
            .map(tuple -> tuple.targetScope())
            .map(extendPathToNextScopeAndValidate)
            .flatMap(OptionalStream::of)
            .map(getter::apply)
            //.flatMap(nextScope -> OptionalStream.of(Paths.append(path, Paths.direct(path.getTarget(), l, nextScope)).map(getter::apply)))
            .collect(CapsuleCollectors.toSet());
        // @formatter:on

        return environments;
    }

    private static final <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> Set.Immutable<IPersistentEnvironment<S, L, O, P>> importScopes(
            Set.Immutable<O> seenImports, L l, IScopePath<S, L, O> path,
            IPersistentEnvironment.Filter<S, L, O, P> filter,
            Function<IScopePath<S, L, O>, IPersistentEnvironment<S, L, O, P>> getter,
            /***/
            PersistentNameResolution<S, L, O> nameResolution) {

        final PersistentScopeGraph<S, L, O> scopeGraph = nameResolution.getScopeGraph();
       
        final Function<IResolutionPath<S, L, O>, IPersistentEnvironment<S, L, O, P>> importPathToUnionEnvironment = importPath -> {
            // @formatter:off        
            final Set.Immutable<IPersistentEnvironment<S, L, O, P>> importEnvironments = scopeGraph.exportDeclarationsStream()
                    .filter(labelEquals(l))
                    .filter(occurrenceEquals(importPath.getDeclaration()))
                    .map(tuple -> tuple.scope())
                    .flatMap(nextScope -> OptionalStream.of(Paths.append(path, Paths.named(path.getTarget(), l, importPath, nextScope)).map(getter::apply)))
                    .collect(CapsuleCollectors.toSet());
            // @formatter:on

            return Environments.union(importEnvironments);
        };

        final Function<IPersistentEnvironment<S, L, O, IResolutionPath<S, L, O>>, IPersistentEnvironment<S, L, O, P>> intermediateToFinal = environment -> {
            final Set.Immutable<IPersistentEnvironment<S, L, O, P>> importEnvironments = environment.solution()
                    .orElse(Set.Immutable.of()).stream().map(importPathToUnionEnvironment)
                    .collect(CapsuleCollectors.toSet());

            return Environments.union(importEnvironments);
        };

        // @formatter:off        
        final Set.Immutable<IPersistentEnvironment<S, L, O, P>> environments = scopeGraph.importReferencesStream()
            .filter(scopeEquals(path.getTarget()))
            .filter(tuple -> !seenImports.contains(tuple.occurrence()))
            .map(tuple -> tuple.occurrence())
            .map(reference -> resolveEnvironment(seenImports, reference, nameResolution))
            .map(intermediateToFinal)            
            .collect(CapsuleCollectors.toSet());
        // @formatter:on       
        
        return environments;           
    }

    private EnvironmentBuilder<S, L, O> getOrCacheStagedEnvironment(final IRelation<L> lt, final Supplier<EnvironmentBuilder<S, L, O>> lazyValue) {
        return stagedEnv_L.computeIfAbsent(lt, key -> lazyValue.get());
    }
    
//    private static final <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> env_L(Set.Immutable<L> labels,
//            Set.Immutable<O> seenImports, IRelation<L> lt, IRegExpMatcher<L> re, IScopePath<S, L, O> path,
//            IPersistentEnvironment.Filter<S, L, O, P> filter) {
//
//        return stagedEnv_L.computeIfAbsent(lt, lo -> stageEnv_L(labels, lo, this)).apply(seenImports, re, path, filter,
//                Maps.newHashMap());
//    }

    private static final <S extends IScope, L extends ILabel, O extends IOccurrence> EnvironmentBuilder<S, L, O> stageEnvironments(
            PersistentNameResolution<S, L, O> nameResolution, IRelation<L> lt) {
        return stageEnvironments0(nameResolution, lt, nameResolution.getLabels());      
    }
    
    private static final <S extends IScope, L extends ILabel, O extends IOccurrence> EnvironmentBuilder<S, L, O> stageEnvironments0(
            PersistentNameResolution<S, L, O> nameResolution, IRelation<L> lt, final Set.Immutable<L> labels) {
        final List<EnvironmentBuilder<S, L, O>> stagedEnvs = Lists.newArrayList();

        final Set.Immutable<L> max = max(lt, labels);
        for (L l : max) {
            final Set.Immutable<L> smaller = smaller(lt, labels, l);
            EnvironmentBuilder<S, L, O> smallerEnv = stageEnvironments0(nameResolution, lt, smaller);

            stagedEnvs.add(new EnvironmentLShadow<>(l, lt, smallerEnv));
        }
        
        // TODO: if stagedEnvs -> return emptyEnvironment
        
//        Environments.union(stagedEnvs.stream().map(se -> se.apply(seenImports, re, path, filter, env_lCache))
//                .collect(CapsuleCollectors.toSet()));
        
        return new EnvironmentLUnion<>(stagedEnvs);
    }

    private static class EnvironmentLShadow<S extends IScope, L extends ILabel, O extends IOccurrence>
    implements EnvironmentBuilder<S, L, O> {

        private final L l;
        private final IRelation<L> lt;        
        private final EnvironmentBuilder<S, L, O> smallerEnv;
        // private final PersistentNameResolution<S, L, O> nameResolution;

        private EnvironmentLShadow(final L l, final IRelation<L> lt, final EnvironmentBuilder<S, L, O> smallerEnv) {
            this.l = l;
            this.lt = lt;
            this.smallerEnv = smallerEnv;
            // this.nameResolution = nameResolution;
        }

        @Override
        public <P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> build(Set.Immutable<O> seenImports,
                IRegExpMatcher<L> re, IScopePath<S, L, O> path, IPersistentEnvironment.Filter<S, L, O, P> filter,
                Map<L, IPersistentEnvironment<S, L, O, P>> env_lCache, final PersistentNameResolution<S, L, O> nameResolution) {
            final IPersistentEnvironment<S, L, O, P> env_l = Environments
                    .lazy((Function0<IPersistentEnvironment<S, L, O, P>> & Serializable) () -> {
                        return env_lCache.computeIfAbsent(l, ll -> env_l(seenImports, lt, re, l, path, filter, nameResolution));
                    });

            return Environments.shadow(filter,
                    Arrays.asList(smallerEnv.build(seenImports, re, path, filter, env_lCache, nameResolution), env_l));
        }
    };    
    
    private static class EnvironmentLUnion<S extends IScope, L extends ILabel, O extends IOccurrence>
            implements EnvironmentBuilder<S, L, O> {

        private final List<EnvironmentBuilder<S, L, O>> stagedEnvs;

        private EnvironmentLUnion(final List<EnvironmentBuilder<S, L, O>> stagedEnvs) {
            this.stagedEnvs = stagedEnvs;
        }

        @Override
        public <P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> build(Set.Immutable<O> seenImports,
                IRegExpMatcher<L> re, IScopePath<S, L, O> path, IPersistentEnvironment.Filter<S, L, O, P> filter,
                Map<L, IPersistentEnvironment<S, L, O, P>> env_lCache, PersistentNameResolution<S, L, O> nameResolution) {

            return Environments.union(stagedEnvs.stream().map(se -> se.build(seenImports, re, path, filter, env_lCache, nameResolution))
                    .collect(CapsuleCollectors.toSet()));
        }
    };
    
    private static <L extends ILabel> Set.Immutable<L> max(IRelation<L> lt, Set.Immutable<L> labels) {
        // @formatter:off
        final Set.Immutable<L> result = labels.stream()
            .filter(l -> lt.larger(l).stream().noneMatch(labels::contains))
            .collect(CapsuleCollectors.toSet());
        // @formatter:on       

        return result;
    }

    private static <L extends ILabel> Set.Immutable<L> smaller(IRelation<L> lt, Set.Immutable<L> labels, L l) {
        // @formatter:off
        final Set.Immutable<L> result = lt.smaller(l).stream()
            .filter(labels::contains)
            .collect(CapsuleCollectors.toSet());
        // @formatter:on              

        return result;
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
