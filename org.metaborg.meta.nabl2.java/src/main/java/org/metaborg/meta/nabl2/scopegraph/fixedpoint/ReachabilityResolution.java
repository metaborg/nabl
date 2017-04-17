package org.metaborg.meta.nabl2.scopegraph.fixedpoint;

import org.metaborg.meta.nabl2.regexp.IRegExpMatcher;
import org.metaborg.meta.nabl2.regexp.RegExpMatcher;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;
import org.metaborg.meta.nabl2.util.collections.HashRelation3;
import org.metaborg.meta.nabl2.util.collections.IRelation3;

public class ReachabilityResolution<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements IPathResolver<S, L, O> {

    /**
     * Fixed-point name resolution algorithms
     * 
     * <pre>
     * Reachability calculation happens while edges are being added.
     * - When composing paths, cycles and bogus imports are checked
     * - We could interleave well-formedness checking
     *   (1) rejecting paths that cannot be extended
     *   (2) create only well-formed resolution paths
     * - We could interleave specificity checking
     *   (1) drop resolution paths that are less specific than a new path
     *   (2) drop any dependencies of a dropped path
     * - We could also implement well-formedness and specificity as a
     *   separate filter on the calculated reachability paths.
     * 
     * </pre>
     */

    private final IRegExpMatcher<L> wf;
    private final IRelation3.Mutable<S, IScopePath<S, L, O>, S> reachability;
    private final IRelation3.Mutable<O, IResolutionPath<S, L, O>, O> resolution;

    public ReachabilityResolution(IResolutionParameters<L> params) {
        this.wf = RegExpMatcher.create(params.getPathWf());
        this.reachability = HashRelation3.create();
        this.resolution = HashRelation3.create();
    }

    @Override public boolean add(IResolutionPath<S, L, O> path) {
        if(!resolution.contains(path.getReference(), path, path.getDeclaration())
                && !wf.match(path.getLabels()).isEmpty()) {
            resolution.put(path.getReference(), path, path.getDeclaration());
            return true;
        }
        return false;
    }

    @Override public boolean add(IScopePath<S, L, O> path) {
        if(!reachability.contains(path.getSource(), path, path.getTarget())
                && wf.match(path.getLabels()).isAccepting()) {
            reachability.put(path.getSource(), path, path.getTarget());
            return true;
        }
        return false;
    }

    @Override public IRelation3<S, IScopePath<S, L, O>, S> scopePaths() {
        return reachability;
    }

    @Override public IRelation3<O, IResolutionPath<S, L, O>, O> resolutionPaths() {
        return resolution;
    }

}