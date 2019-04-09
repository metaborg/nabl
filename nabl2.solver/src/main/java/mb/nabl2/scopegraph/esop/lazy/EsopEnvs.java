package mb.nabl2.scopegraph.esop.lazy;

import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.Optional;

import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.PartialFunction0;

import com.google.common.collect.Queues;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.Set.Immutable;
import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;
import mb.nabl2.scopegraph.path.IDeclPath;
import mb.nabl2.scopegraph.path.IPath;
import mb.nabl2.scopegraph.path.IResolutionPath;
import mb.nabl2.scopegraph.terms.SpacedName;
import mb.nabl2.scopegraph.terms.path.Paths;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;

// TODO: Support garbage collection of sub-envs
// * lambda's must be separate classes not to capture arguments
// * inline paths and 'null' references (or drop elements from list)

public class EsopEnvs {

    // guarded delegation
    public static <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>>
            IEsopEnv<S, L, O, P> guarded(PartialFunction0<IEsopEnv<S, L, O, P>> provider) {
        return new IEsopEnv<S, L, O, P>() {
            private static final long serialVersionUID = 42L;

            private IEsopEnv<S, L, O, P> env = null;

            private Optional<IEsopEnv<S, L, O, P>> env() {
                if(env == null) {
                    env = provider.apply().orElse(null);
                }
                return Optional.ofNullable(env);
            }

            @Override public Optional<Tuple2<Set.Immutable<P>, Set.Immutable<String>>> get() {
                return env().flatMap(IEsopEnv::get);
            }

            @Override public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append("#");
                sb.append(env().map(Object::toString).orElse("?"));
                return sb.toString();
            }

        };
    }

    // lazy delegation
    public static <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>>
            IEsopEnv<S, L, O, P> lazy(Function0<IEsopEnv<S, L, O, P>> provider) {
        return new IEsopEnv<S, L, O, P>() {
            private static final long serialVersionUID = 42L;

            private IEsopEnv<S, L, O, P> env = null;

            private IEsopEnv<S, L, O, P> env() {
                if(env == null) {
                    env = provider.apply();
                }
                return env;
            }

            @Override public Optional<Tuple2<Immutable<P>, Immutable<String>>> get() {
                return env().get();
            }

            @Override public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append("~");
                sb.append(env());
                return sb.toString();
            }

        };
    }

    // initialize with paths
    public static <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>>
            IEsopEnv<S, L, O, P> init(Iterable<P> paths) {
        return new IEsopEnv<S, L, O, P>() {
            private static final long serialVersionUID = 42L;

            private Set.Immutable<P> _paths = Set.Immutable.<P>of().__insertAll(Sets.newHashSet(paths));

            @Override public Optional<Tuple2<Immutable<P>, Immutable<String>>> get() {
                return Optional.of(ImmutableTuple2.of(_paths, Set.Immutable.of()));
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

            private final Deque<IEsopEnv<S, L, O, P>> _envs = Queues.newArrayDeque(Arrays.asList(envs));
            private final Set.Transient<Object> _shadowed = Set.Transient.of();
            private final Set.Transient<P> _paths = Set.Transient.of();
            private final Set.Transient<String> _trace = Set.Transient.of();
            private Set.Immutable<P> paths = null;
            private Set.Immutable<String> trace = null;

            private boolean done() {
                if(paths != null) {
                    return true;
                }
                Iterator<IEsopEnv<S, L, O, P>> it = _envs.iterator();
                while(paths == null && it.hasNext()) {
                    IEsopEnv<S, L, O, P> env = it.next();
                    if(env.get().map(pts -> {
                        // be careful not to self-shadow, therefore first add paths, then add shadow tokens
                        pts._1().stream().filter(p -> !_shadowed.contains(filter.matchToken(p)))
                                .forEach(_paths::__insert);
                        pts._1().stream().map(p -> filter.matchToken(p)).forEach(_shadowed::__insert);
                        _trace.__insertAll(pts._2());
                        it.remove();
                        if(filter.shortCircuit() && !_paths.isEmpty()) {
                            paths = _paths.freeze();
                            trace = _trace.freeze();
                            return true;
                        }
                        return false;
                    }).orElse(true)) {
                        break;
                    }
                }
                if(paths == null && _envs.isEmpty()) {
                    paths = _paths.freeze();
                    trace = _trace.freeze();
                }
                return paths != null;
            }

            @Override public Optional<Tuple2<Immutable<P>, Immutable<String>>> get() {
                return done() ? Optional.of(ImmutableTuple2.of(paths, trace)) : Optional.empty();
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

            private final java.util.Set<IEsopEnv<S, L, O, P>> _envs = Sets.newHashSet(envs);
            private final Set.Transient<P> _paths = Set.Transient.of();
            private final Set.Transient<String> _trace = Set.Transient.of();
            private Set.Immutable<P> paths = null;
            private Set.Immutable<String> trace = null;

            private boolean done() {
                if(paths != null) {
                    return true;
                }
                Iterator<IEsopEnv<S, L, O, P>> it = _envs.iterator();
                while(it.hasNext()) {
                    final IEsopEnv<S, L, O, P> env = it.next();
                    env.get().ifPresent(pts -> {
                        _paths.__insertAll(pts._1());
                        _trace.__insertAll(pts._2());
                        it.remove();
                    });
                }
                if(_envs.isEmpty()) {
                    paths = _paths.freeze();
                    trace = _trace.freeze();
                }
                return paths != null;
            }

            @Override public Optional<Tuple2<Immutable<P>, Immutable<String>>> get() {
                return done() ? Optional.of(ImmutableTuple2.of(paths, trace)) : Optional.empty();
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

    // trace environment
    public static <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>>
            IEsopEnv<S, L, O, P> trace(String step, IEsopEnv<S, L, O, P> env) {
        return new IEsopEnv<S, L, O, P>() {
            private static final long serialVersionUID = 42L;

            @Override public Optional<Tuple2<Immutable<P>, Immutable<String>>> get() {
                return env.get().map(pts -> ImmutableTuple2.of(pts._1(), pts._2().__insert(step)));
            }

            @Override public String toString() {
                return step + " @ " + env.toString();
            }

        };
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>>
            IEsopEnv<S, L, O, P> trace(IEsopEnv<S, L, O, P> env, Immutable<String> steps) {
        return new IEsopEnv<S, L, O, P>() {
            private static final long serialVersionUID = 42L;

            @Override public Optional<Tuple2<Immutable<P>, Immutable<String>>> get() {
                return env.get().map(pts -> ImmutableTuple2.of(pts._1(), pts._2().__insertAll(steps)));
            }

            @Override public String toString() {
                return steps + " @ " + env.toString();
            }

        };
    }

    // empty environment
    public static <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>>
            IEsopEnv<S, L, O, P> empty() {
        return new IEsopEnv<S, L, O, P>() {
            private static final long serialVersionUID = 42L;

            @Override public Optional<Tuple2<Immutable<P>, Immutable<String>>> get() {
                return Optional.of(ImmutableTuple2.of(Set.Immutable.of(), Set.Immutable.of()));
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
                return SpacedName.of(p.getDeclaration());
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
                return SpacedName.of(p.getDeclaration());
            }

            @Override public boolean shortCircuit() {
                return false;
            }

        };
    }

}