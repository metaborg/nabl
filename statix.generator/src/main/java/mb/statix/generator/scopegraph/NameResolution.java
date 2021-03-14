package mb.statix.generator.scopegraph;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.metaborg.util.functions.Predicate0;
import org.metaborg.util.functions.Predicate2;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;

import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.scopegraph.reference.EdgeOrData;
import mb.statix.scopegraph.reference.IncompleteException;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.scopegraph.terms.path.Paths;
import mb.statix.spec.Spec;

public class NameResolution<S extends D, L, D, X> {

    private final Spec spec;
    private final IScopeGraph<S, L, D> scopeGraph;

    private final EdgeOrData<L> dataLabel;
    private final Set<EdgeOrData<L>> allLabels;

    private final LabelWF<L> labelWF; // default: true
    private final LabelOrder<L> labelOrder; // default: false

    private final DataWF<D, X> dataWF; // default: true
    private final boolean dataEquiv; // default: false

    private final Predicate2<S, EdgeOrData<L>> isComplete; // default: true

    private Predicate0 select;

    public NameResolution(Spec spec, IScopeGraph<S, L, D> scopeGraph, Set<L> edgeLabels, LabelWF<L> labelWF,
            LabelOrder<L> labelOrder, DataWF<D, X> dataWF, boolean dataEquiv, Predicate2<S, EdgeOrData<L>> isComplete) {
        this.spec = spec;
        this.scopeGraph = scopeGraph;
        this.dataLabel = EdgeOrData.data();
        this.allLabels = Streams.concat(Stream.of(dataLabel), edgeLabels.stream().map(EdgeOrData::edge))
                .collect(Collectors.toSet());
        this.labelWF = labelWF;
        this.labelOrder = labelOrder;
        this.dataWF = dataWF;
        this.dataEquiv = dataEquiv;
        this.isComplete = isComplete;
    }

    public Env<S, L, D, X> resolve(S scope, Predicate0 select) throws ResolutionException, InterruptedException {
        this.select = select;
        final Env<S, L, D, X> env = env(labelWF, Paths.empty(scope));
        return env;
    }

    private Env<S, L, D, X> env(LabelWF<L> re, IScopePath<S, L> path) throws ResolutionException, InterruptedException {
        return env_L(allLabels, re, path);
    }

    private Env<S, L, D, X> env_L(Set<EdgeOrData<L>> L, LabelWF<L> re, IScopePath<S, L> path)
            throws ResolutionException, InterruptedException {
        if(Thread.interrupted()) {
            throw new InterruptedException();
        }
        final Env.Builder<S, L, D, X> env = Env.builder();
        final Set<EdgeOrData<L>> max_L = max(L);
        for(EdgeOrData<L> l : max_L) {
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

    private Set<EdgeOrData<L>> max(Set<EdgeOrData<L>> L) throws ResolutionException, InterruptedException {
        final ImmutableSet.Builder<EdgeOrData<L>> max = ImmutableSet.builder();
        outer: for(EdgeOrData<L> l1 : L) {
            for(EdgeOrData<L> l2 : L) {
                if(labelOrder.lt(l1, l2)) {
                    continue outer;
                }
            }
            max.add(l1);
        }
        return max.build();
    }

    private Set<EdgeOrData<L>> smaller(Set<EdgeOrData<L>> L, EdgeOrData<L> l1)
            throws ResolutionException, InterruptedException {
        final ImmutableSet.Builder<EdgeOrData<L>> smaller = ImmutableSet.builder();
        for(EdgeOrData<L> l2 : L) {
            if(labelOrder.lt(l2, l1)) {
                smaller.add(l2);
            }
        }
        return smaller.build();
    }

    private Env<S, L, D, X> env_l(EdgeOrData<L> l, LabelWF<L> re, IScopePath<S, L> path)
            throws ResolutionException, InterruptedException {
        return l.matchInResolution(() -> env_data(re, path), lbl -> env_edges(lbl, re, path));
    }

    private Env<S, L, D, X> env_data(LabelWF<L> re, IScopePath<S, L> path)
            throws ResolutionException, InterruptedException {
        if(!re.accepting()) {
            return Env.empty();
        }
        if(!isComplete.test(path.getTarget(), dataLabel)) {
            throw new IncompleteException(path.getTarget(), dataLabel);
        }
        final D datum;
        if((datum = getData(re, path).orElse(null)) == null) {
            return Env.empty();
        }
        final Optional<Optional<X>> x = dataWF.wf(spec, datum);
        if(!x.isPresent()) {
            return Env.empty();
        }
        return Env.match(Paths.resolve(path, datum), x.get());
    }

    private Env<S, L, D, X> env_edges(L l, LabelWF<L> re, IScopePath<S, L> path)
            throws ResolutionException, InterruptedException {
        final Optional<LabelWF<L>> newRe = re.step(l);
        if(!newRe.isPresent()) {
            return Env.empty();
        } else {
            re = newRe.get();
        }
        final EdgeOrData<L> edgeLabel = EdgeOrData.edge(l);
        if(!isComplete.test(path.getTarget(), edgeLabel)) {
            throw new IncompleteException(path.getTarget(), edgeLabel);
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

    protected Optional<D> getData(LabelWF<L> re, IScopePath<S, L> path) {
        return scopeGraph.getData(path.getTarget());
    }

    protected Iterable<S> getEdges(LabelWF<L> re, IScopePath<S, L> path, L l) {
        return scopeGraph.getEdges(path.getTarget(), l);
    }

}
