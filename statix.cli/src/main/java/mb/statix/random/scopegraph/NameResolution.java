package mb.statix.random.scopegraph;

import java.util.Optional;
import java.util.Set;

import org.metaborg.util.functions.Predicate0;
import org.metaborg.util.functions.Predicate2;

import com.google.common.collect.ImmutableSet;

import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.scopegraph.reference.IncompleteDataException;
import mb.statix.scopegraph.reference.IncompleteEdgeException;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.scopegraph.terms.path.Paths;

public class NameResolution<S extends D, L, D, X> {

    private final IScopeGraph<S, L, D> scopeGraph;
    private final L relation;
    private final Set<L> labels;

    private final LabelWF<L> labelWF; // default: true
    private final LabelOrder<L> labelOrder; // default: false
    private final Predicate2<S, L> isEdgeComplete; // default: true

    private final DataWF<D, X> dataWF; // default: true
    private final boolean dataEquiv; // default: false
    private final Predicate2<S, L> isDataComplete; // default: true

    private Predicate0 select;

    public NameResolution(IScopeGraph<S, L, D> scopeGraph, L relation, LabelWF<L> labelWF, LabelOrder<L> labelOrder,
            Predicate2<S, L> isEdgeComplete, DataWF<D, X> dataWF, boolean dataEquiv, Predicate2<S, L> isDataComplete) {
        this.scopeGraph = scopeGraph;
        this.relation = relation;
        this.labels =
                ImmutableSet.<L>builder().addAll(scopeGraph.getEdgeLabels()).add(scopeGraph.getNoDataLabel()).build();
        this.labelWF = labelWF;
        this.labelOrder = labelOrder;
        this.isEdgeComplete = isEdgeComplete;
        this.dataWF = dataWF;
        this.dataEquiv = dataEquiv;
        this.isDataComplete = isDataComplete;
    }

    public Env<S, L, D, X> resolve(S scope, Predicate0 select) throws ResolutionException, InterruptedException {
        this.select = select;
        final Env<S, L, D, X> env = env(labelWF, Paths.empty(scope));
        return env;
    }

    private Env<S, L, D, X> env(LabelWF<L> re, IScopePath<S, L> path) throws ResolutionException, InterruptedException {
        return env_L(labels, re, path);
    }

    private Env<S, L, D, X> env_L(Set<L> L, LabelWF<L> re, IScopePath<S, L> path)
            throws ResolutionException, InterruptedException {
        if(Thread.interrupted()) {
            throw new InterruptedException();
        }
        final Env.Builder<S, L, D, X> env = Env.builder();
        final Set<L> max_L = max(L);
        for(L l : max_L) {
            final Env<S, L, D, X> env1 = env_L(smaller(L, l), re, path);
            final boolean matchEnv2;
            if(env1.isEmpty()) {
                env.reject(env1);
                matchEnv2 = true;
            } else if(!dataEquiv) {
                env.match(env1);
                matchEnv2 = true;
            } else if(env1.isNullable() && !select.test()) {
                env.reject(env1);
                matchEnv2 = true;
            } else {
                env.match(env1);
                matchEnv2 = false;
            }
            if(matchEnv2) {
                env.match(env_l(l, re, path));
            }
        }
        return env.build();
    }

    private Set<L> max(Set<L> L) throws ResolutionException, InterruptedException {
        final ImmutableSet.Builder<L> max = ImmutableSet.builder();
        outer: for(L l1 : L) {
            for(L l2 : L) {
                if(labelOrder.lt(l1, l2)) {
                    continue outer;
                }
            }
            max.add(l1);
        }
        return max.build();
    }

    private Set<L> smaller(Set<L> L, L l1) throws ResolutionException, InterruptedException {
        final ImmutableSet.Builder<L> smaller = ImmutableSet.builder();
        for(L l2 : L) {
            if(labelOrder.lt(l2, l1)) {
                smaller.add(l2);
            }
        }
        return smaller.build();
    }

    private Env<S, L, D, X> env_l(L l, LabelWF<L> re, IScopePath<S, L> path)
            throws ResolutionException, InterruptedException {
        if(scopeGraph.getEdgeLabels().contains(l)) {
            return env_nonEOP(l, re, path);
        } else if(scopeGraph.getNoDataLabel().equals(l)) {
            return env_EOP(re, path);
        } else {
            throw new IllegalStateException("Encountered unknown label " + l);
        }
    }

    private Env<S, L, D, X> env_EOP(LabelWF<L> re, IScopePath<S, L> path)
            throws ResolutionException, InterruptedException {
        if(!re.accepting()) {
            return Env.of();
        }
        final S scope = path.getTarget();
        if(!isDataComplete.test(scope, relation)) {
            throw new IncompleteDataException(scope, relation);
        }
        final Env.Builder<S, L, D, X> env = Env.builder();
        if(relation.equals(scopeGraph.getNoDataLabel())) {
            final D datum = scope;
            final Optional<Optional<X>> x = dataWF.wf(datum);
            if(x.isPresent()) {
                env.match(Paths.resolve(path, relation, datum), x.get());
            }
        } else {
            for(D datum : getData(re, path, relation)) {
                final Optional<Optional<X>> x = dataWF.wf(datum);
                if(x.isPresent()) {
                    env.match(Paths.resolve(path, relation, datum), x.get());
                }
            }
        }
        return env.build();
    }

    private Env<S, L, D, X> env_nonEOP(L l, LabelWF<L> re, IScopePath<S, L> path)
            throws ResolutionException, InterruptedException {
        final Optional<LabelWF<L>> newRe = re.step(l);
        if(!newRe.isPresent()) {
            return Env.of();
        } else {
            re = newRe.get();
        }
        if(!isEdgeComplete.test(path.getTarget(), l)) {
            throw new IncompleteEdgeException(path.getTarget(), l);
        }
        final Env.Builder<S, L, D, X> env = Env.builder();
        for(S nextScope : getEdges(re, path, l)) {
            final Optional<IScopePath<S, L>> p = Paths.append(path, Paths.edge(path.getTarget(), l, nextScope));
            if(p.isPresent()) {
                env.match(env(re, p.get()));
            }
        }
        return env.build();
    }

    ///////////////////////////////////////////////////////////////////////////
    // edges and data                                                        //
    ///////////////////////////////////////////////////////////////////////////

    protected java.util.Set<D> getData(LabelWF<L> re, IScopePath<S, L> path, L l) {
        return scopeGraph.getData(path.getTarget(), l);
    }

    protected java.util.Set<S> getEdges(LabelWF<L> re, IScopePath<S, L> path, L l) {
        return scopeGraph.getEdges(path.getTarget(), l);
    }

}
