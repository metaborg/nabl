package org.metaborg.meta.nabl2.scopegraph.esop.persistent;

import static org.metaborg.meta.nabl2.util.tuples.HasLabel.labelEquals;
import static org.metaborg.meta.nabl2.util.tuples.ScopeLabelOccurrence.occurrenceEquals;
import static org.metaborg.meta.nabl2.util.tuples.ScopeLabelOccurrence.scopeEquals;
import static org.metaborg.meta.nabl2.util.tuples.ScopeLabelScope.sourceScopeEquals;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.metaborg.meta.nabl2.regexp.IRegExpMatcher;
import org.metaborg.meta.nabl2.relations.IRelation;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.path.IPath;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;
import org.metaborg.meta.nabl2.scopegraph.terms.path.Paths;

import com.google.common.collect.Maps;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;

public class EnvironmentBuilders<S extends IScope, L extends ILabel, O extends IOccurrence> {

    /**
     * Staged computation builder for name resolution that optionally allows
     * refinement by use of shadowing.
     */
    static class RefinementBuilder<S extends IScope, L extends ILabel, O extends IOccurrence>
            implements EnvironmentBuilder<S, L, O> {

        private final L label;
        private final Optional<EnvironmentBuilder<S, L, O>> shadowingBuilder;

        RefinementBuilder(final L label, final Optional<EnvironmentBuilder<S, L, O>> shadowingBuilder) {
            this.label = label;
            this.shadowingBuilder = shadowingBuilder;
        }

        @Override
        public <P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> build(Set.Immutable<O> seenImports,
                IRegExpMatcher<L> re, IScopePath<S, L, O> path, IPersistentEnvironment.Filter<S, L, O, P> filter,
                Map<L, IPersistentEnvironment<S, L, O, P>> env_lCache, IRelation<L> lt,
                Optional<O> resolutionReference, PersistentNameResolution<S, L, O> nameResolution, boolean eagerEvaluation) {
            
            /*
             * NOTE: caching currently does not work because.
             */
            assert !env_lCache.containsKey(label);
                       
            final IPersistentEnvironment<S, L, O, P> labelEnvironment;
                        
            // NOTE: using nameResolution.{scopeGraph, labelD}
            if (nameResolution.getScopeCounter().isOpen(path.getTarget(), label)) {
                labelEnvironment = Environments.unresolvable();
            } else if (label.equals(nameResolution.getLabelD()) && !re.isAccepting()) {
                labelEnvironment = Environments.empty();
            } else if (label.equals(nameResolution.getLabelD()) && re.isAccepting()) {
                // @formatter:off
                final Set.Immutable<P> paths = nameResolution.getScopeGraph().localDeclarationsStream()
                    .filter(scopeEquals(path.getTarget()))
                    .map(tuple -> tuple.occurrence())
                    .flatMap(declaration -> OptionalStream.of(filter.test(Paths.decl(path, declaration))))
                    .collect(CapsuleCollectors.toSet());
                // @formatter:on

                labelEnvironment = Environments.eager(paths);
            } else {
                final IRegExpMatcher<L> nextRe = re.match(label);

                if (nextRe.isEmpty()) {
                    // TODO check if importScopes calculation can be pruned as
                    // well 
                    labelEnvironment = Environments.empty();
                } else {                
                    // NOTE: label is captured in instance variable.

                    final Supplier<IPersistentEnvironment<S, L, O, P>> environmentSupplier = () -> {
                        // NOTE: using nextRe
                        final IPersistentEnvironment<S, L, O, P> result = env_lCache.computeIfAbsent(label,
                                label -> EnvironmentBuilders.env_l(seenImports, lt, nextRe, label, path, filter, env_lCache,
                                        resolutionReference, nameResolution, eagerEvaluation));
                        return result;
                    };
                    
                    if (eagerEvaluation) {
                        labelEnvironment = environmentSupplier.get();
                    } else {
                        // TODO: decompose in more specialized steps
                        labelEnvironment = Environments.lazy(environmentSupplier);
                    }
                }
            }

            if (shadowingBuilder.isPresent()) {
                final IPersistentEnvironment<S, L, O, P> shadowEnvironment = shadowingBuilder.get().build(seenImports,
                        re, path, filter, env_lCache, lt, resolutionReference, nameResolution, eagerEvaluation);
             
                assert filter != Environments.identityFilter() || resolutionReference.isPresent();
                
                return Environments.shadow(filter, Arrays.asList(shadowEnvironment, labelEnvironment));
                
                // return Environments.shadow(resolutionReference.isPresent(),
                // Arrays.asList(shadowEnvironment, labelEnvironment));
            } else {
                return labelEnvironment;
            }
        }

        @Override
        public String toString() {
            if (shadowingBuilder.isPresent()) {
                return String.format("%s < %s", shadowingBuilder.get(), label);
            } else {
                return String.format("~%s", label);
            }
        }
    }

    /**
     * Staged computation builder for name resolution that represents the union
     * of a set of nested computations.
     */
    static class AggregationBuilder<S extends IScope, L extends ILabel, O extends IOccurrence>
            implements EnvironmentBuilder<S, L, O> {

        private final Set.Immutable<EnvironmentBuilder<S, L, O>> builders;

        AggregationBuilder(final Set.Immutable<EnvironmentBuilder<S, L, O>> builders) {
            this.builders = builders;
        }

        @Override
        public <P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> build(Set.Immutable<O> seenImports,
                IRegExpMatcher<L> re, IScopePath<S, L, O> path, IPersistentEnvironment.Filter<S, L, O, P> filter,
                Map<L, IPersistentEnvironment<S, L, O, P>> env_lCache, IRelation<L> lt, 
                Optional<O> resolutionReference,
                PersistentNameResolution<S, L, O> nameResolution, boolean eagerEvaluation) {

            // @formatter:off
            final Set.Immutable<IPersistentEnvironment<S, L, O, P>> environments = builders.stream()
                    .map(b -> b.build(seenImports, re, path, filter, env_lCache, lt, resolutionReference, nameResolution, eagerEvaluation))
                    .collect(CapsuleCollectors.toSet());
            // @formatter:on

            return Environments.union(environments);
        }

        @Override
        public String toString() {
            return builders.toString();
        }
    }

    static final <S extends IScope, L extends ILabel, O extends IOccurrence> EnvironmentBuilder<S, L, O> stage(
            IRelation<L> lt, final Set.Immutable<L> labels) {
        
        // @formatter:off
        final Set.Immutable<L> max = max(lt, labels);
        
        final Set.Immutable<EnvironmentBuilder<S, L, O>> builders = max.stream()
            .map(l -> {
                final Set.Immutable<L> smallerLabels = smaller(lt, labels, l);
                
                final Optional<EnvironmentBuilder<S, L, O>> environmentBuilder;
                
                if (smallerLabels.isEmpty()) {
                    environmentBuilder = Optional.empty();
                } else {
                    environmentBuilder = Optional.of(stage(lt, smallerLabels));
                }
                
                return new RefinementBuilder<>(l, environmentBuilder);
            })
            .collect(CapsuleCollectors.toSet());
        // @formatter:on
    
        // if (builders.size() == 0) {
        // System.out.println("TODO");
        // }
        
        if (builders.size() == 1) {
            return builders.findFirst().get();
        }
        
        return new AggregationBuilder<>(builders);
    }
        
    static <L extends ILabel> Set.Immutable<L> max(IRelation<L> lt, Set.Immutable<L> labels) {
        // @formatter:off
        final Set.Immutable<L> result = labels.stream()
            .filter(l -> lt.larger(l).stream().noneMatch(labels::contains))
            .collect(CapsuleCollectors.toSet());
        // @formatter:on       

        return result;
    }

    static <L extends ILabel> Set.Immutable<L> smaller(IRelation<L> lt, Set.Immutable<L> labels, L l) {
        // @formatter:off
        final Set.Immutable<L> result = lt.smaller(l).stream()
            .filter(labels::contains)
            .collect(CapsuleCollectors.toSet());
        // @formatter:on              

        return result;
    }

    /**
     * Returns the set of declarations that are reachable from S with a
     * l-labeled step.
     */
    public static <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> env_l(
            Set.Immutable<O> seenImports,
            IRelation<L> lt, IRegExpMatcher<L> nextRe, L label, IScopePath<S, L, O> path,
            IPersistentEnvironment.Filter<S, L, O, P> filter,
            /***/
            Map<L, IPersistentEnvironment<S, L, O, P>> env_lCache,
            Optional<O> resolutionReference,
            PersistentNameResolution<S, L, O> nameResolution, boolean eagerEvaluation) {
    
        // TODO WIP factoring out facts
        assert !nameResolution.getScopeCounter().isOpen(path.getTarget(), label);
        assert !label.equals(nameResolution.getLabelD());
        assert !nextRe.isEmpty();
           
        // case: env_nonD       
        final Function<IScopePath<S, L, O>, IPersistentEnvironment<S, L, O, P>> nestedPathToEnvironment = 
                p -> {
                    final EnvironmentBuilder<S, L, O> builder = nameResolution.getEnvironmentBuilder(lt);

                    // NOTE uses 'nextRe' and 'p'
                    // TODO should I use env_lCache instead of
                    // Maps.newHashMap()?
                    final IPersistentEnvironment<S, L, O, P> environment = builder.build(seenImports, nextRe, p, filter,
                            Maps.newHashMap(), lt, resolutionReference, nameResolution, eagerEvaluation);

                    return environment;
                };

        final Set.Immutable<IPersistentEnvironment<S, L, O, P>> directScopes = directScopes(seenImports, label, path,
                filter, nestedPathToEnvironment, nameResolution);
        final Set.Immutable<IPersistentEnvironment<S, L, O, P>> importScopes = importScopes(seenImports, label, path,
                filter, nestedPathToEnvironment, nameResolution);

        // TODO: add union to Capsule
        final Set.Immutable<IPersistentEnvironment<S, L, O, P>> scopes = directScopes.__insertAll(importScopes);
        return Environments.union(scopes);
    }    
    
    static final <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> Set.Immutable<IPersistentEnvironment<S, L, O, P>> directScopes(
            Set.Immutable<O> seenImports, L label, IScopePath<S, L, O> path,
            IPersistentEnvironment.Filter<S, L, O, P> filter,
            Function<IScopePath<S, L, O>, IPersistentEnvironment<S, L, O, P>> nestedPathToEnvironment,
            /***/
            PersistentNameResolution<S, L, O> nameResolution) {

        final PersistentScopeGraph<S, L, O> scopeGraph = nameResolution.getScopeGraph();
       
        final Function<S, Optional<IScopePath<S, L, O>>> extendPathToNextScopeAndValidate = 
                nextScope -> Paths.append(path, Paths.direct(path.getTarget(), label, nextScope));

        // @formatter:off
        final Set.Immutable<IPersistentEnvironment<S, L, O, P>> environments = scopeGraph.directEdgesStream()
            .filter(labelEquals(label))
            .filter(sourceScopeEquals(path.getTarget()))
            .map(tuple -> tuple.targetScope())
            .map(extendPathToNextScopeAndValidate)
            .flatMap(OptionalStream::of)
            .map(nestedPathToEnvironment::apply)
            //.flatMap(nextScope -> OptionalStream.of(Paths.append(path, Paths.direct(path.getTarget(), l, nextScope)).map(getter::apply)))
            .collect(CapsuleCollectors.toSet());
        // @formatter:on

        return environments;
    }

    static final <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> Set.Immutable<IPersistentEnvironment<S, L, O, P>> importScopes(
            Set.Immutable<O> seenImports, L label, IScopePath<S, L, O> path,
            IPersistentEnvironment.Filter<S, L, O, P> filter,
            Function<IScopePath<S, L, O>, IPersistentEnvironment<S, L, O, P>> nestedPathToEnvironment,
            /***/
            PersistentNameResolution<S, L, O> nameResolution) {

        final PersistentScopeGraph<S, L, O> scopeGraph = nameResolution.getScopeGraph();
       
        final Function<IResolutionPath<S, L, O>, IPersistentEnvironment<S, L, O, P>> importPathToUnionEnvironment = importPath -> {
            // @formatter:off        
            final Set.Immutable<IPersistentEnvironment<S, L, O, P>> importEnvironments = scopeGraph.exportDeclarationsStream()
                    .filter(labelEquals(label))
                    .filter(occurrenceEquals(importPath.getDeclaration()))
                    .map(tuple -> tuple.scope())
                    .flatMap(nextScope -> OptionalStream.of(Paths.append(path, Paths.named(path.getTarget(), label, importPath, nextScope)).map(nestedPathToEnvironment::apply)))
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
            .map(reference -> PersistentNameResolution.resolveEnvironment(seenImports, reference, nameResolution))
            .map(intermediateToFinal)            
            .collect(CapsuleCollectors.toSet());
        // @formatter:on               
        
        return environments;        
    }    

}