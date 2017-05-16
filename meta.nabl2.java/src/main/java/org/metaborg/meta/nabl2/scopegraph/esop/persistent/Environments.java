package org.metaborg.meta.nabl2.scopegraph.esop.persistent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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

// TODO: Support garbage collection of sub-envs
// * lambda's must be separate classes not to capture arguments
// * inline paths and 'null' references (or drop elements from list)

public class Environments {
    
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

        private final Function0<IPersistentEnvironment<S, L, O, P>> supplier;
        private IPersistentEnvironment<S, L, O, P> result = null;
        
        private LazyEnvironment(final Function0<IPersistentEnvironment<S, L, O, P>> supplier) {
            this.supplier = supplier;
        }
        
        private IPersistentEnvironment<S, L, O, P> env() {
            if (result == null) {
                result = supplier.apply();
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
            Function0<IPersistentEnvironment<S, L, O, P>> supplier) {
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
        return new EagerEnvironment<>(paths);
    }

    /*
     * TODO ensure that environment can be used as set and is not required to be a sequence???
     */
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

        return new StatefulLazyShadowEnvironment<>(filter, environments);
    }   

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
        
        return new StatefulLazyUnionEnvironment<>(environments);
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
    };

    @SuppressWarnings("rawtypes")
    private static final IPersistentEnvironment.Filter IDENTITY_FILTER = new IdentityFilter<>();
    
    @SuppressWarnings("unchecked")
    public static <S extends IScope, L extends ILabel, O extends IOccurrence> IPersistentEnvironment.Filter<S, L, O, IDeclPath<S, L, O>> identityFilter() {
        return IDENTITY_FILTER;
    }

}
