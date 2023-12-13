package mb.statix.generator.scopegraph;

import java.util.Optional;
import java.util.Set;

import jakarta.annotation.Nullable;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Predicate0;
import org.metaborg.util.functions.Predicate2;

import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.path.IScopePath;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.oopsla20.reference.IncompleteException;
import mb.scopegraph.oopsla20.reference.LabelOrder;
import mb.scopegraph.oopsla20.reference.LabelWF;
import mb.scopegraph.oopsla20.reference.ResolutionException;
import mb.scopegraph.oopsla20.terms.path.Paths;
import mb.statix.spec.Spec;

public class NameResolution<S extends D, L, D, X> {

    private final Spec spec;
    private final IScopeGraph<S, L, D> scopeGraph;

    private final EdgeOrData<L> dataLabel;
    private final Set<EdgeOrData<L>> allLabels;

    private final LabelWF<L> labelWF; // default: true
    private final LabelOrder<L> labelOrder; // default: false

    private final DataWF<D, X> dataWF; // default: true
    /** Whether the data equivalence constraint is always true. */
    private final boolean dataEquiv; // default: false

    private final Predicate2<S, EdgeOrData<L>> isComplete; // default: true

    private Predicate0 select;

    public NameResolution(Spec spec, IScopeGraph<S, L, D> scopeGraph, Set<L> edgeLabels, LabelWF<L> labelWF,
            LabelOrder<L> labelOrder, DataWF<D, X> dataWF, boolean dataEquiv, Predicate2<S, EdgeOrData<L>> isComplete) {
        this.spec = spec;
        this.scopeGraph = scopeGraph;
        this.dataLabel = EdgeOrData.data();
        this.allLabels =
            edgeLabels.stream().map(EdgeOrData::edge).collect(CapsuleCollectors.toSet())
                .__insert(dataLabel);
        this.labelWF = labelWF;
        this.labelOrder = labelOrder;
        this.dataWF = dataWF;
        this.dataEquiv = dataEquiv;
        this.isComplete = isComplete;
    }


    /**
     * Returns the environment with the declarations to which this name resolution resolves.
     *
     * @param scope the starting scope
     * @param select a predicate that selects whether a match is accepted
     * @return the resulting environment
     */
    public Env<S, L, D, X> resolve(S scope, Predicate0 select) throws ResolutionException, InterruptedException {
        this.select = select;
        final Env<S, L, D, X> env = env(labelWF, Paths.empty(scope));
        return env;
    }

    /**
     * Returns the environment with the declarations that are reachable from the specified scope graph path,
     * with a minimal path satisfying the well-formedness criteria.
     *
     * @param re the path well-formedness criteria
     * @param path the scope graph path
     * @return the resulting environment
     */
    private Env<S, L, D, X> env(LabelWF<L> re, IScopePath<S, L> path) throws ResolutionException, InterruptedException {
        return env_L(allLabels, re, path);
    }

    /**
     * Returns the environment with the declarations that are visible from the specified scope graph path
     * through labels in set L after applying the shadowing policy. Using the label order, declarations
     * accessible through smaller labels shadow declarations accessible through larger ones.
     *
     * @param L the set of labels
     * @param re the path well-formedness criteria
     * @param path the scope graph path
     * @return the resulting environment
     */
    private Env<S, L, D, X> env_L(Set<EdgeOrData<L>> L, LabelWF<L> re, IScopePath<S, L> path)
            throws ResolutionException, InterruptedException {
        if(Thread.interrupted()) {
            throw new InterruptedException();
        }
        final Env.Builder<S, L, D, X> env = Env.builder();
        // For each label that may end a path:
        final Set<EdgeOrData<L>> max_L = max(L);
        for(EdgeOrData<L> l : max_L) {
            // Check if any of the smaller labels resolve.
            // If so, it may shadow this resolution.
            final Env<S, L, D, X> env1 = env_L(smaller(L, l), re, path);
            // env1 <| env2 ?
            final boolean matchEnv2;
            if(env1.isEmpty()) {
                // It didn't resolve. This resolution is not shadowed.
                env.reject(env1);
                matchEnv2 = true;
            } else if(!dataEquiv) {
                env.match(env1);
                matchEnv2 = true;
            } else if(env1.isNullable() && !select.test()) {
                // It did resolve, is conditional, but not selected
                env.reject(env1);
                matchEnv2 = true;
            } else {
                // It did resolve and shadows this resolution
                env.match(env1);
                matchEnv2 = false;
            }
            if(matchEnv2) {
                // This resolution is still valid
                env.match(env_l(l, re, path));
            }
        }
        return env.build();
    }

    /**
     * Gets the set of maximal elements of L.
     *
     * This returns those labels in L that are not smaller than any other label in L.
     *
     * @param L the set of labels
     * @return the maximal elements of the set of labels
     */
    private Set<EdgeOrData<L>> max(Set<EdgeOrData<L>> L) throws ResolutionException, InterruptedException {
        final io.usethesource.capsule.Set.Transient<EdgeOrData<L>> max = CapsuleUtil.transientSet();
        outer: for(EdgeOrData<L> l1 : L) {
            for(EdgeOrData<L> l2 : L) {
                if(labelOrder.lt(l1, l2)) {
                    continue outer;
                }
            }
            max.__insert(l1);
        }
        return max.freeze();
    }

    /**
     * Gets the set of labels in L that are smaller than l1.
     *
     * @param L the set of labels
     * @param l1 the label to test against
     * @return the set of labels in L that are smaller than l1
     */
    private Set<EdgeOrData<L>> smaller(Set<EdgeOrData<L>> L, EdgeOrData<L> l1)
            throws ResolutionException, InterruptedException {
        final io.usethesource.capsule.Set.Transient<EdgeOrData<L>> smaller = CapsuleUtil.transientSet();
        for(EdgeOrData<L> l2 : L) {
            if(labelOrder.lt(l2, l1)) {
                smaller.__insert(l2);
            }
        }
        return smaller.freeze();
    }

    /**
     * Returns the environment with the declarations accessible from the specified scope graph path
     * through an l-labeled step.
     *
     * @param l the label of the step
     * @param re the path well-formedness criteria
     * @param path the scope graph path
     * @return the resulting environment
     */
    private Env<S, L, D, X> env_l(EdgeOrData<L> l, LabelWF<L> re, IScopePath<S, L> path)
            throws ResolutionException, InterruptedException {
        return l.matchInResolution(
            () -> env_data(re, path),
            lbl -> env_edges(lbl, re, path)
        );
    }

    /**
     * Returns the environment with the declarations accessible from the specified scope graph path
     * through a data step, i.e., the set of declarations in the path.
     *
     * @param re the path well-formedness criteria
     * @param path the scope graph path
     * @return the resulting environment
     */
    private Env<S, L, D, X> env_data(LabelWF<L> re, IScopePath<S, L> path)
            throws ResolutionException, InterruptedException {
        if(!re.accepting()) {
            return Env.empty();
        }
        // Ensure completeness of the path.
        if(!isComplete.test(path.getTarget(), dataLabel)) {
            throw new IncompleteException(path.getTarget(), dataLabel);
        }
        // Get the data at the path
        @Nullable final D datum;
        if((datum = getData(path).orElse(null)) == null) {
            // No data found.
            return Env.empty();
        }
        // Find whether there is a condition on the data (e.g, a constraint)
        final Optional<Optional<X>> x = dataWF.wf(spec, datum);
        if(!x.isPresent()) {
            // Match is unconditional.
            return Env.empty();
        }
        // Return an environment matching the path and the data, with the specified condition
        return Env.match(Paths.resolve(path, datum), x.get());
    }

    /**
     * Returns the environment with the declarations accessible from the specified scope graph path
     * through a label step.
     *
     * @param l the label
     * @param re the path well-formedness criteria
     * @param path the scope graph path
     * @return the resulting environment
     */
    private Env<S, L, D, X> env_edges(L l, LabelWF<L> re, IScopePath<S, L> path)
            throws ResolutionException, InterruptedException {
        // Perform a step in the well-formedness relation
        final Optional<LabelWF<L>> newRe = re.step(l);
        if(!newRe.isPresent()) {
            // Step was invalid, return nothing
            return Env.empty();
        } else {
            // Step was valid
            re = newRe.get();
        }
        // Assert that the path from the last scope and the given edge is complete
        final EdgeOrData<L> edgeLabel = EdgeOrData.edge(l);
        if(!isComplete.test(path.getTarget(), edgeLabel)) {
            throw new IncompleteException(path.getTarget(), edgeLabel);
        }

        final Env.Builder<S, L, D, X> env = Env.builder();
        for(S nextScope : getEdges(path, l)) {
            // Extend the path with the label and the next scope
            final Optional<IScopePath<S, L>> p = Paths.append(path, Paths.edge(path.getTarget(), l, nextScope));
            if(p.isPresent()) {
                // The path is valid (i.e., properly connected and not cyclic)
                // Find the resolutions through the new path and add each of them to this environment
                env.match(env(re, p.get()));
            }
        }
        return env.build();
    }


    ///////////////////////////////////////////////////////////////////////////
    // edges and data                                                        //
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Gets the data that is reachable from the given path.
     * @param path the path
     * @return the data reachable from the given path; or nothing if none found
     */
    protected Optional<D> getData(IScopePath<S, L> path) {
        return scopeGraph.getData(path.getTarget());
    }

    /**
     * Gets all scopes from the given path that are reachable through the specified label.
     *
     * @param path the path
     * @param l the label
     * @return an iterable of scopes reachable from the specified path through the specified label
     */
    protected Iterable<S> getEdges(IScopePath<S, L> path, L l) {
        return scopeGraph.getEdges(path.getTarget(), l);
    }

}
