package org.metaborg.meta.nabl2.scopegraph.esop.persistent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.path.IDeclPath;
import org.metaborg.meta.nabl2.scopegraph.path.IPath;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.terms.SpacedName;
import org.metaborg.meta.nabl2.scopegraph.terms.path.Paths;
import org.metaborg.meta.nabl2.util.functions.Function0;
import org.metaborg.meta.nabl2.util.functions.PartialFunction0;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.Set.Immutable;
import io.usethesource.capsule.util.stream.CapsuleCollectors;

// TODO: Support garbage collection of sub-envs
// * lambda's must be separate classes not to capture arguments
// * inline paths and 'null' references (or drop elements from list)

public class Environments {
    
    private static final boolean USE_STATELESS_FILTERS = true;
    
    private static class LazyGuardedEnvironment<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> implements IPersistentEnvironment<S, L, O, P> {
        private static final long serialVersionUID = 42L;

        private final PartialFunction0<IPersistentEnvironment<S, L, O, P>> supplier;
        private IPersistentEnvironment<S, L, O, P> result = null;
        
        private LazyGuardedEnvironment(final PartialFunction0<IPersistentEnvironment<S, L, O, P>> supplier) {
            this.supplier = supplier;
        }
               
        private Optional<IPersistentEnvironment<S, L, O, P>> env() {
            if (result == null) {
                result = supplier.apply().orElse(null);
            }
            return Optional.ofNullable(result);
        }

        @Override
        public Optional<Set.Immutable<P>> solution() {
            return env().flatMap(IPersistentEnvironment::solution);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("#");
            sb.append(env().map(Object::toString).orElse("?"));
            return sb.toString();
        }

        @Override
        public int hashCode() {
            // TODO Auto-generated method stub
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            // TODO Auto-generated method stub
            return super.equals(obj);
        }
    };
    
    // guarded delegation
    public static <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> guarded(
            PartialFunction0<IPersistentEnvironment<S, L, O, P>> supplier) {
        return new LazyGuardedEnvironment<>(supplier);                
    }

    private static class LazyEnvironment<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> implements IPersistentEnvironment<S, L, O, P> {
        private static final long serialVersionUID = 42L;

        private final Supplier<IPersistentEnvironment<S, L, O, P>> supplier;
        private IPersistentEnvironment<S, L, O, P> result = null;
        
        private LazyEnvironment(final Supplier<IPersistentEnvironment<S, L, O, P>> supplier) {
            this.supplier = supplier;
        }
        
        private IPersistentEnvironment<S, L, O, P> env() {
            if (result == null) {
                result = supplier.get();
            }
            return result;
        }

        @Override
        public Optional<Set.Immutable<P>> solution() {
            return env().solution();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("~");
            sb.append(env());
            return sb.toString();
        }
        
        @Override
        public int hashCode() {
            // TODO Auto-generated method stub
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            // TODO Auto-generated method stub
            return super.equals(obj);
        }        
    };    
    
    // lazy delegation
    public static <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> lazy(
            Supplier<IPersistentEnvironment<S, L, O, P>> supplier) {
        return new LazyEnvironment<>(supplier);
    }

    private static class EagerEnvironment<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> implements IPersistentEnvironment<S, L, O, P> {
        private static final long serialVersionUID = 42L;

        private final Set.Immutable<P> paths;
        
        private EagerEnvironment(final Set.Immutable<P> paths) {
            this.paths = paths;
        }
        
        @Override
        public Optional<Set.Immutable<P>> solution() {
            return Optional.of(paths);
        }

        @Override
        public String toString() {
            return paths.toString();
        }
        
        @Override
        public int hashCode() {
            // TODO Auto-generated method stub
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            // TODO Auto-generated method stub
            return super.equals(obj);
        }        
    };
    
    public static <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> eager(
            Set.Immutable<P> paths) {
        if (paths.isEmpty()) {
            return empty();
        } else {
            return new EagerEnvironment<>(paths);
        }
    }

    private static class LazyShadowEnvironment<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> implements IPersistentEnvironment<S, L, O, P> {
        private static final long serialVersionUID = 42L;

        private final IPersistentEnvironment.Filter<S, L, O, P> filter;
        private final List<IPersistentEnvironment<S, L, O, P>> environments;
        
        transient private Set.Immutable<P> result;
        
        private LazyShadowEnvironment(final IPersistentEnvironment.Filter<S, L, O, P> filter,
                final List<IPersistentEnvironment<S, L, O, P>> environments) {
            this.filter = filter;
            this.environments = environments;
        }
        
        private @Nullable Set.Immutable<P> paths() {
            if (result == null) {
                boolean aborted = false;
                boolean solutionFound = false;
                                
                final Set.Transient<Object> shadowTokens = Set.Transient.of();                
                final Set.Transient<P> partialResult = Set.Transient.of();                
                
                final Iterator<IPersistentEnvironment<S, L, O, P>> it = environments.iterator();
                                
                while (!aborted && !solutionFound && it.hasNext()) {
                    final IPersistentEnvironment<S, L, O, P> environment = it.next();

                    if (environment.solution().isPresent()) {
                        final Set.Immutable<P> paths = environment.solution().get();

                        // To avoid self-shadowing, we first add not yet
                        // shadowed paths.
                        paths.stream().filter(path -> !shadowTokens.contains(filter.matchToken(path)))
                                .forEach(partialResult::__insert);

                        // Then we update the shadow list.
                        paths.stream().map(filter::matchToken).forEach(shadowTokens::__insert);

                        if (filter.shortCircuit() && !partialResult.isEmpty()) {
                            solutionFound = true;
                        }
                    } else {
                        aborted = true;
                    }
                }       
               
                if (!aborted) {
                    result = partialResult.freeze();
                }
            }
            
            return result;
        }

        @Override
        public Optional<Set.Immutable<P>> solution() {
            return Optional.ofNullable(paths());
        }

        @Override
        public String toString() {
            String body = environments.stream().map(Object::toString).collect(Collectors.joining(" <| "));
            return String.format("( %s )", body);           
        }
        
        @Override
        public int hashCode() {
            // TODO Auto-generated method stub
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            // TODO Auto-generated method stub
            return super.equals(obj);
        }        
    };
    
    private static class StatefulLazyShadowEnvironment<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> implements IPersistentEnvironment<S, L, O, P> {
        private static final long serialVersionUID = 42L;

        private final IPersistentEnvironment.Filter<S, L, O, P> filter;
        // private final List<IPersistentEnvironment<S, L, O, P>> environments;

        private final Deque<IPersistentEnvironment<S, L, O, P>> worklist;
        
        private final Set.Transient<Object> shadowTokens = Set.Transient.of();
        
        private final Set.Transient<P> partialResult;
        transient private Set.Immutable<P> result;
        
        private StatefulLazyShadowEnvironment(final IPersistentEnvironment.Filter<S, L, O, P> filter,
                final List<IPersistentEnvironment<S, L, O, P>> environments) {
            this.filter = filter;
            // this.environments = environments;
            
            // state for incrementally calculated result
            this.worklist = new ArrayDeque<>(environments); 
            this.partialResult = Set.Transient.of();
            this.result = null;            
        }
        
        private @Nullable Set.Immutable<P> paths() {
            if (result == null) {
                boolean aborted = false;
                boolean solutionFound = false;
                
                final Iterator<IPersistentEnvironment<S, L, O, P>> it = worklist.iterator();
                                
                while (!aborted && !solutionFound && it.hasNext()) {
                    final IPersistentEnvironment<S, L, O, P> environment = it.next();

                    if (environment.solution().isPresent()) {
                        final Set.Immutable<P> paths = environment.solution().get();

                        // environment has a solution -> remove from work list
                        it.remove();

                        // To avoid self-shadowing, we first add not yet
                        // shadowed paths.
                        paths.stream().filter(path -> !shadowTokens.contains(filter.matchToken(path)))
                                .forEach(partialResult::__insert);

                        // Then we update the shadow list.
                        paths.stream().map(filter::matchToken).forEach(shadowTokens::__insert);

                        if (filter.shortCircuit() && !partialResult.isEmpty()) {
                            solutionFound = true;
                        }
                    } else {
                        aborted = true;
                    }
                }       

                if (solutionFound) {
                    worklist.clear();
                }
                
                if (worklist.isEmpty()) {
                    result = partialResult.freeze();
                }
            }
            
            return result;
        }

        @Override
        public Optional<Set.Immutable<P>> solution() {
            return Optional.ofNullable(paths());
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("(");
            sb.append(result != null ? result : partialResult);
            worklist.stream().forEach(e -> {
                sb.append(" <| ");
                sb.append(e);
            });
            sb.append(")");
            return sb.toString();
        }
        
        @Override
        public int hashCode() {
            // TODO Auto-generated method stub
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            // TODO Auto-generated method stub
            return super.equals(obj);
        }        
    };
    
    private static class LazyReferenceShadowEnvironment<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>, RP extends IResolutionPath<S, L, O>>
            implements IPersistentEnvironment<S, L, O, P> {
        private static final long serialVersionUID = 42L;

        private final boolean shortCircuit;
        private final List<IPersistentEnvironment<S, L, O, P>> environments;
        
        transient private Set.Immutable<P> result;
        
        private LazyReferenceShadowEnvironment(final boolean shortCircuit, final List<IPersistentEnvironment<S, L, O, P>> environments) {
            assert environments.size() > 1;
            this.shortCircuit = shortCircuit;
            this.environments = environments;
        }
        
        @SuppressWarnings("unchecked")
        private @Nullable Set.Immutable<P> paths() {
            if (result == null) {
                                               
                // path.getDeclaration()
                final Set.Transient<O> shadowTokens = Set.Transient.of();

                final Predicate<RP> statefulFilter = path -> {
                    if (shadowTokens.contains(path.getDeclaration())) {
                        return false;
                    } else {
                        shadowTokens.__insert(path.getDeclaration());
                        return true;
                    }
                };
                
                @SuppressWarnings("unchecked")
                final Function<P, RP> castPathToResolutionPath = path -> ((RP) IResolutionPath.class.cast(path));
                
                // @formatter:off
                final Stream<Optional<Set.Immutable<RP>>> solutionStream = 
                        environments.stream()
                        .map(IPersistentEnvironment::solution)
                        .flatMap(solution -> {
                            if (solution.isPresent()) {
                                final Set.Immutable<RP> partialResult = solution.get().stream()
                                          .map(castPathToResolutionPath)
                                          .filter(statefulFilter)
                                          .collect(CapsuleCollectors.toSet());
                                
                                if (partialResult.isEmpty()) {
                                    return Stream.empty();                                    
                                } else {
                                    return Stream.of(Optional.of(partialResult));     
                                }
                               
                            } else {
                                return Stream.of(Optional.<Set.Immutable<RP>>empty());
                            }
                        });
                // @formatter:on

                final Optional<Optional<Set.Immutable<RP>>> shadowedPaths;
                
                if (shortCircuit) {
                    shadowedPaths = solutionStream.findFirst();
                } else {
                    shadowedPaths = solutionStream.reduce((left, right) -> {
                        if (left.isPresent() && right.isPresent()) {
                            return Optional.of(left.get().__insertAll(right.get()));
                        } else {
                            return Optional.empty();
                        }
                    });
                }         
                
                if (!shadowedPaths.isPresent()) {
                    // empty set of paths
                    result = Set.Immutable.of();
                } else {
                    // either set of paths or null if not solvable
                    result = (Set.Immutable<P>) shadowedPaths.get().orElse(null); 
                }
            }

            return result;
        }

        @Override
        public Optional<Set.Immutable<P>> solution() {
            return Optional.ofNullable(paths());
        }

        @Override
        public String toString() {
            String body = environments.stream().map(Object::toString).collect(Collectors.joining(" <| "));
            return String.format("( %s )", body);           
        }
        
        @Override
        public int hashCode() {
            // TODO Auto-generated method stub
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            // TODO Auto-generated method stub
            return super.equals(obj);
        }        
    };
    
//    // NOTE: order of environments is relevant for algorithm to work correctly 
//    public static <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> shadow(
//            boolean shortCircuit, List<IPersistentEnvironment<S, L, O, P>> candidates) {
//
//        final List<IPersistentEnvironment<S, L, O, P>> environments = candidates.stream()
//                .filter(environment -> !Objects.equals(environment, empty())).collect(Collectors.toList());
//
//        if (environments.isEmpty()) {
//            return Environments.empty();
//        }
//
//        if (environments.size() == 1) {
//            return environments.stream().findFirst().get();
//        }
//       
//        return new LazyReferenceShadowEnvironment<>(shortCircuit, environments);
//        
////        if (USE_STATELESS_FILTERS) {
////            return new LazyShadowEnvironment<>(filter, environments);
////        } else {
////            return new StatefulLazyShadowEnvironment<>(filter, environments);
////        }
//    }
      
    // NOTE: order of environments is relevant for algorithm to work correctly
    public static <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> shadow(
            IPersistentEnvironment.Filter<S, L, O, P> filter, List<IPersistentEnvironment<S, L, O, P>> candidates) {
        
        final List<IPersistentEnvironment<S, L, O, P>> environments = candidates.stream()
                .filter(environment -> !Objects.equals(environment, empty())).collect(Collectors.toList());

        if (environments.isEmpty()) {
            return Environments.empty();
        }
        
        if (environments.size() == 1) {
            return environments.stream().findFirst().get();
        }       
        
        if (USE_STATELESS_FILTERS) {
            // return new LazyShadowEnvironment<>(filter, environments);
            
            if (filter == IDENTITY_FILTER) {
                return new LazyReferenceShadowEnvironment<>(false, environments);
            } else {
                return new LazyReferenceShadowEnvironment<>(true, environments);
            }            
        } else {
            return new StatefulLazyShadowEnvironment<>(filter, environments);
        }
    }   

    private static class LazyUnionEnvironment<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> implements IPersistentEnvironment<S, L, O, P> {
        private static final long serialVersionUID = 42L;

        private final Set.Immutable<IPersistentEnvironment<S, L, O, P>> environments;       
        transient private Set.Immutable<P> result;        
        
        private LazyUnionEnvironment(final Set.Immutable<IPersistentEnvironment<S, L, O, P>> environments) {
            this.environments = environments;            
        }

        private @Nullable Set.Immutable<P> paths() {
            if (result == null) {             
                boolean aborted = false;               
                
                final Set.Transient<P> partialResult = Set.Transient.of();
                
                final Iterator<IPersistentEnvironment<S, L, O, P>> it = environments.iterator();
                while (!aborted && it.hasNext()) {
                    final IPersistentEnvironment<S, L, O, P> environment = it.next();

                    environment.solution().ifPresent(paths -> {
                        partialResult.__insertAll(paths);
                    });
                }

                if (!aborted) {
                    result = partialResult.freeze();
                }
            }
            
            return result;
        }

        @Override
        public Optional<Set.Immutable<P>> solution() {
            return Optional.ofNullable(paths());
        }

        @Override
        public String toString() {
            return environments.toString();
        }
        
        @Override
        public int hashCode() {
            // TODO Auto-generated method stub
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            // TODO Auto-generated method stub
            return super.equals(obj);
        }        
    };
    
    private static class StatefulLazyUnionEnvironment<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> implements IPersistentEnvironment<S, L, O, P> {
        private static final long serialVersionUID = 42L;

        // private final Set.Immutable<IPersistentEnvironment<S, L, O, P>> environments;
        
        private final Deque<IPersistentEnvironment<S, L, O, P>> worklist;
        private final Set.Transient<P> partialResult;
        transient private Set.Immutable<P> result;        
        
        private StatefulLazyUnionEnvironment(final Set.Immutable<IPersistentEnvironment<S, L, O, P>> environments) {
            // this.environments = environments;
            
            // state for incrementally calculated result
            this.worklist = new ArrayDeque<>(environments); 
            this.partialResult = Set.Transient.of();
            this.result = null;
        }

        private @Nullable Set.Immutable<P> paths() {
            if (result == null) {                
                final Iterator<IPersistentEnvironment<S, L, O, P>> it = worklist.iterator();
                while (it.hasNext()) {
                    final IPersistentEnvironment<S, L, O, P> environment = it.next();

                    environment.solution().ifPresent(paths -> {
                        partialResult.__insertAll(paths);
                        it.remove();
                    });
                }

                if (worklist.isEmpty()) {
                    result = partialResult.freeze();
                }
            }
            
            return result;
        }

        @Override
        public Optional<Set.Immutable<P>> solution() {
            return Optional.ofNullable(paths());
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("(");
            sb.append(result != null ? result : partialResult);
            worklist.stream().forEach(e -> {
                sb.append(" U ");
                sb.append(e);
            });
            sb.append(")");
            return sb.toString();
        }
        
        @Override
        public int hashCode() {
            // TODO Auto-generated method stub
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            // TODO Auto-generated method stub
            return super.equals(obj);
        }        
    };
    
    
    public static <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> union(
            Set.Immutable<IPersistentEnvironment<S, L, O, P>> candidates) {
        
        final Set.Immutable<IPersistentEnvironment<S, L, O, P>> environments;

        if (candidates.contains(empty())) {
            environments = candidates.__remove(empty());
        } else {
            environments = candidates;
        }
               
        if (environments.isEmpty()) {
            return Environments.empty();
        }

        if (environments.size() == 1) {
            return environments.findFirst().get();
        }
        
        if (USE_STATELESS_FILTERS) {
            return new LazyUnionEnvironment<>(environments);
        } else {
            return new StatefulLazyUnionEnvironment<>(environments);
        }
    }

    private static class EmptyEnvironment<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> implements IPersistentEnvironment<S, L, O, P> {
        private static final long serialVersionUID = 42L;

        @Override
        public Optional<Set.Immutable<P>> solution() {
            return Optional.of(Set.Immutable.of());
        }

        @Override
        public String toString() {
            return Set.Immutable.of().toString();
        }
        
        @Override
        public int hashCode() {
            return -1260680698;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other)
                return true;
            if (other == null)
                return false;
            if (getClass() != other.getClass())
                return false;

            return true;
        }
    };
    
    @SuppressWarnings("rawtypes")
    private static final IPersistentEnvironment EMPTY_ENVIRONMENT = new EmptyEnvironment<>();
    
    @SuppressWarnings("unchecked")
    public static final <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> empty() {
        return EMPTY_ENVIRONMENT;
    }
    
    private static class UnresolvableEnvironment<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> implements IPersistentEnvironment<S, L, O, P> {
        private static final long serialVersionUID = 42L;

        @Override
        public Optional<Set.Immutable<P>> solution() {
            return Optional.empty();
        }

        @Override
        public String toString() {
            return "???";
        }
        
        @Override
        public int hashCode() {
            return 1012020627;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other)
                return true;
            if (other == null)
                return false;
            if (getClass() != other.getClass())
                return false;

            return true;
        }
    };
    
    @SuppressWarnings("rawtypes")
    private static final IPersistentEnvironment UNRESOLVABLE_ENVIRONMENT = new UnresolvableEnvironment<>();
    
    @SuppressWarnings("unchecked")
    public static final <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> IPersistentEnvironment<S, L, O, P> unresolvable() {
        return UNRESOLVABLE_ENVIRONMENT;
    }    
    
    // TODO: rename to RedirectFilter?
    private static class ResolutionFilter<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IResolutionPath<S, L, O>>
            implements IPersistentEnvironment.Filter<S, L, O, P> {
        private static final long serialVersionUID = 42L;

        private final O reference;

        private ResolutionFilter(O reference) {
            this.reference = Objects.requireNonNull(reference);
        }

        @Override
        public Optional<P> test(IDeclPath<S, L, O> path) {
            return (Optional<P>) Paths.resolve(reference, path);
        }

        @Override
        public Object matchToken(P path) {
            return SpacedName.of(path.getDeclaration());
        }

        @Override
        public boolean shortCircuit() {
            return true;
        }

        @Override
        public int hashCode() {
            return -1263774330 + reference.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other)
                return true;
            if (other == null)
                return false;
            if (getClass() != other.getClass())
                return false;
            
            @SuppressWarnings("rawtypes")
            ResolutionFilter that = (ResolutionFilter) other;
            
            return Objects.equals(reference, that.reference);
        }
        
        @Override
        public String toString() {
            return String.format("ResolutionFilter(%s)", reference);            
        }
    };    
    
    // TODO: rename to RedirectFilter?
    public static <S extends IScope, L extends ILabel, O extends IOccurrence> IPersistentEnvironment.Filter<S, L, O, IResolutionPath<S, L, O>> resolutionFilter(
            O reference) { 
        return new ResolutionFilter<>(reference);
    }
    
    private static class IdentityFilter<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IDeclPath<S, L, O>>
            implements IPersistentEnvironment.Filter<S, L, O, P> {
        private static final long serialVersionUID = 42L;

        private IdentityFilter() {
        }

        @Override
        public Optional<P> test(IDeclPath<S, L, O> path) {
            return (Optional<P>) Optional.of(path);
        }

        @Override
        public Object matchToken(P path) {
            return SpacedName.of(path.getDeclaration());
        }

        @Override
        public boolean shortCircuit() {
            return false;
        }
        
        @Override
        public int hashCode() {
            return 993372364;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other)
                return true;
            if (other == null)
                return false;
            if (getClass() != other.getClass())
                return false;
            
            return true;
        }
        
        @Override
        public String toString() {
            return String.format("IdentityFilter()");            
        }        
    };

    @SuppressWarnings("rawtypes")
    private static final IPersistentEnvironment.Filter IDENTITY_FILTER = new IdentityFilter<>();
    
    @SuppressWarnings("unchecked")
    public static <S extends IScope, L extends ILabel, O extends IOccurrence> IPersistentEnvironment.Filter<S, L, O, IDeclPath<S, L, O>> identityFilter() {
        return IDENTITY_FILTER;
    }

}
