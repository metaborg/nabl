package mb.nabl2.scopegraph.esop.lazy;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.NullCancel;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Set;
import mb.nabl2.scopegraph.CriticalEdgeException;
import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;
import mb.nabl2.scopegraph.path.IDeclPath;
import mb.nabl2.scopegraph.path.IPath;
import mb.nabl2.scopegraph.path.IResolutionPath;
import mb.nabl2.scopegraph.terms.path.Paths;
import mb.nabl2.util.CapsuleUtil;

// TODO: Support garbage collection of sub-envs
// * lambda's must be separate classes not to capture arguments
// * inline paths and 'null' references (or drop elements from list)

public class EsopEnvs {

    // guarded delegation
    public static <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>>
            IEsopEnv<S, L, O, P> guarded(EnvProvider<S, L, O, P> provider) {
        return new IEsopEnv<S, L, O, P>() {
            private static final long serialVersionUID = 42L;

            private IEsopEnv<S, L, O, P> env = null;

            private IEsopEnv<S, L, O, P> env() throws CriticalEdgeException, InterruptedException {
                if(env == null) {
                    env = provider.apply();
                }
                return env;
            }

            @Override public Collection<P> get(ICancel cancel) throws CriticalEdgeException, InterruptedException {
                return env().get(cancel);
            }

            @Override public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append("#");
                try {
                    sb.append(env().toString());
                } catch(CriticalEdgeException | InterruptedException e) {
                    sb.append("?");
                }
                return sb.toString();
            }

        };
    }

    interface EnvProvider<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> {
        IEsopEnv<S, L, O, P> apply() throws CriticalEdgeException, InterruptedException;
    }

    // lazy delegation
    public static <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>>
            IEsopEnv<S, L, O, P> lazy(LazyEnv<S, L, O, P> provider) {
        return new IEsopEnv<S, L, O, P>() {
            private static final long serialVersionUID = 42L;

            private IEsopEnv<S, L, O, P> env = null;

            private IEsopEnv<S, L, O, P> env(ICancel cancel) throws InterruptedException, CriticalEdgeException {
                if(env == null) {
                    env = provider.apply(cancel);
                }
                return env;
            }

            @Override public Collection<P> get(ICancel cancel) throws CriticalEdgeException, InterruptedException {
                return env(cancel).get(cancel);
            }

            @Override public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append("~");
                try {
                    sb.append(env(new NullCancel()));
                } catch(CriticalEdgeException | InterruptedException e) {
                    sb.append("?");
                }
                return sb.toString();
            }

        };
    }

    interface LazyEnv<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>> {
        IEsopEnv<S, L, O, P> apply(ICancel cancel) throws CriticalEdgeException, InterruptedException;

    }

    // initialize with paths
    public static <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>>
            IEsopEnv<S, L, O, P> init(Iterable<P> paths) {
        return new IEsopEnv<S, L, O, P>() {
            private static final long serialVersionUID = 42L;

            private Collection<P> _paths = CapsuleUtil.toSet(paths);

            @Override public Collection<P> get(ICancel cancel) throws InterruptedException {
                return _paths;
            }

            @Override public String toString() {
                return _paths.toString();
            }

        };
    }

    @SafeVarargs public static <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>>
            IEsopEnv<S, L, O, P> shadow(IEsopEnv.Filter<S, L, O, P> filter, IEsopEnv<S, L, O, P>... envs) {
        return new IEsopEnv<S, L, O, P>() {
            private static final long serialVersionUID = 42L;

            private final Deque<IEsopEnv<S, L, O, P>> _envs = Lists.newLinkedList(Arrays.asList(envs));
            private final Collection<Object> _shadowed = Sets.newHashSet();
            private final Set.Transient<P> _paths = Set.Transient.of();
            private Collection<P> paths = null;

            private Collection<P> env(ICancel cancel) throws CriticalEdgeException, InterruptedException {
                if(paths != null) {
                    return paths;
                }
                Iterator<IEsopEnv<S, L, O, P>> it = _envs.iterator();
                while(it.hasNext()) {
                    cancel.throwIfCancelled();
                    IEsopEnv<S, L, O, P> env = it.next();
                    Collection<P> pts = env.get(cancel);
                    // be careful not to self-shadow, therefore first add paths, then add shadow tokens
                    for(P p : pts) {
                        cancel.throwIfCancelled();
                        if(!_shadowed.contains(filter.matchToken(p))) {
                            _paths.__insert(p);
                        }
                    }
                    for(P p : pts) {
                        cancel.throwIfCancelled();
                        _shadowed.add(filter.matchToken(p));
                    }
                    it.remove();
                    if(filter.shortCircuit() && !_paths.isEmpty()) {
                        break;
                    }
                }
                paths = _paths.freeze();
                return paths;
            }

            @Override public Collection<P> get(ICancel cancel) throws CriticalEdgeException, InterruptedException {
                return env(cancel);
            }

            @Override public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append("(");
                sb.append(paths != null ? paths : _paths);
                _envs.stream().forEach(e -> {
                    sb.append(" <| ");
                    sb.append(e);
                });
                sb.append(")");
                return sb.toString();
            }

        };
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>>
            IEsopEnv<S, L, O, P> union(Iterable<IEsopEnv<S, L, O, P>> envs) {
        return new IEsopEnv<S, L, O, P>() {
            private static final long serialVersionUID = 42L;

            private final java.util.LinkedList<IEsopEnv<S, L, O, P>> _envs = Lists.newLinkedList(envs);
            private final Set.Transient<P> _paths = Set.Transient.of();
            private Collection<P> paths = null;

            private Collection<P> env(ICancel cancel) throws CriticalEdgeException, InterruptedException {
                if(paths != null) {
                    return paths;
                }
                List<CriticalEdgeException> es = Lists.newArrayList();
                Iterator<IEsopEnv<S, L, O, P>> it = _envs.iterator();
                while(it.hasNext()) {
                    final IEsopEnv<S, L, O, P> env = it.next();
                    try {
                        Collection<P> pts = env.get(cancel);
                        for(P p : pts) {
                            _paths.__insert(p);
                        }
                        it.remove();
                    } catch(CriticalEdgeException e) {
                        es.add(e);
                    }
                }
                if(!es.isEmpty()) {
                    throw CriticalEdgeException.of(es);
                }
                paths = _paths.freeze();
                return paths;
            }

            @Override public Collection<P> get(ICancel cancel) throws CriticalEdgeException, InterruptedException {
                return env(cancel);
            }

            @Override public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append("(");
                sb.append(paths != null ? paths : _paths);
                _envs.stream().forEach(e -> {
                    sb.append(" U ");
                    sb.append(e);
                });
                sb.append(")");
                return sb.toString();
            }

        };
    }

    // empty environment
    public static <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>>
            IEsopEnv<S, L, O, P> empty() {
        return new IEsopEnv<S, L, O, P>() {
            private static final long serialVersionUID = 42L;

            @Override public Collection<P> get(ICancel cancel) throws InterruptedException {
                return Collections.emptyList();
            }

            @Override public String toString() {
                return "{}";
            }

        };
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence>
            IEsopEnv.Filter<S, L, O, IResolutionPath<S, L, O>> resolutionFilter(O ref) {
        return new IEsopEnv.Filter<S, L, O, IResolutionPath<S, L, O>>() {
            private static final long serialVersionUID = 42L;

            @Override public Optional<IResolutionPath<S, L, O>> test(IDeclPath<S, L, O> path) {
                return Paths.resolve(ref, path);
            }

            @Override public Object matchToken(IResolutionPath<S, L, O> p) {
                return p.getDeclaration().getSpacedName();
            }

            @Override public boolean shortCircuit() {
                return true;
            }

        };
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence>
            IEsopEnv.Filter<S, L, O, IDeclPath<S, L, O>> envFilter() {
        return new IEsopEnv.Filter<S, L, O, IDeclPath<S, L, O>>() {
            private static final long serialVersionUID = 42L;

            @Override public Optional<IDeclPath<S, L, O>> test(IDeclPath<S, L, O> path) {
                return Optional.of(path);
            }

            @Override public Object matchToken(IDeclPath<S, L, O> p) {
                return p.getDeclaration().getSpacedName();
            }

            @Override public boolean shortCircuit() {
                return false;
            }

        };
    }

}