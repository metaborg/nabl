package org.metaborg.meta.nabl2.scopegraph.esop.persistent;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.metaborg.meta.nabl2.regexp.IRegExpMatcher;
import org.metaborg.meta.nabl2.relations.IRelation;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.path.IPath;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;
import org.metaborg.meta.nabl2.util.functions.Function0;

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
                PersistentNameResolution<S, L, O> nameResolution) {
            final IPersistentEnvironment<S, L, O, P> env_l = Environments
                    .lazy((Function0<IPersistentEnvironment<S, L, O, P>> & Serializable) () -> {
                        
                        // TODO: reconsider static call to evn_l
                        return env_lCache.computeIfAbsent(label,
                                ll -> PersistentNameResolution.env_l(seenImports, lt, re, label, path, filter, nameResolution));
                    });

            if (shadowingBuilder.isPresent()) {
                final IPersistentEnvironment<S, L, O, P> shadowing = shadowingBuilder.get().build(seenImports, re, path,
                        filter, env_lCache, lt, nameResolution);

                return Environments.shadow(filter, Arrays.asList(shadowing, env_l));
            } else {
                return env_l;
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
                PersistentNameResolution<S, L, O> nameResolution) {

            // @formatter:off
            final Set.Immutable<IPersistentEnvironment<S, L, O, P>> environments = builders.stream()
                    .map(b -> b.build(seenImports, re, path, filter, env_lCache, lt, nameResolution))
                    .collect(CapsuleCollectors.toSet());
            // @formatter:on

            return Environments.union(environments);
        }

        @Override
        public String toString() {
            return builders.toString();
        }
    }

    static final <S extends IScope, L extends ILabel, O extends IOccurrence> EnvironmentBuilder<S, L, O> stageEnvironments(
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
                    environmentBuilder = Optional.of(stageEnvironments(lt, smallerLabels));
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

}