package org.metaborg.meta.nabl2.scopegraph.fixedpoint.generalized;

import java.util.Set;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.fixedpoint.generalized.ImportsAndExports.Entry;
import org.metaborg.meta.nabl2.util.collections.IRelation3;

import com.google.common.collect.Sets;

public class FixedpointScopeGraphAndNameResolution<S extends IScope, L extends ILabel> {

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
     * - By parameterizing over the domain of refs and decls, we can model
     *   all kind of scopes collections, not just arbitrary occurrences.
     *   We do want to allow mixing them in one model, though.
     *  
     * - For incremental analysis, we want
     *   (1) summarize the graph exports, starting from the interfacing scopes.
     *   (2) collect open resolution. We need to be careful here, if the resolution
     *       does not, in the end, resolve via the interface scope, it might again
     *       resolve in the local graph. This part might not be part of the interface
     *       though. We do however know all possible resolution paths in that part of
     *       the graph, so instead of making that part of the local graph, we might want
     *       to keep the local (possible less specific) resolution paths as backup.
     * 
     * </pre>
     */

    // scope graph
    private final RefsAndDecls.Mutable<S, L> refsAndDecls = null;
    private final IRelation3.Mutable<S, L, S> edges = null;
    private final ImportsAndExports.Mutable<S, L> importsAndExports = null;

    // name resolution
    private final Reachability.Mutable<S, L> reachability = null;
    private final Resolution.Mutable<S, L> resolution = null;

    public <R, D> Set<IPath> addDecl(S scope, Decl<R, D> decl) {
        // forall scope' ->> scope, ref -> scope', if ref ~ decl then ref |-> decl
        Set<IPath> paths = Sets.newHashSet();
        if(refsAndDecls.putDecl(scope, decl)) {
            for(ScopePath<S, L> path : reachability.to(scope)) {
                for(Ref<R, D> ref : refsAndDecls.getRefs(path.getSource(), decl.getNamespace())) {
                    try {
                        paths.addAll(newResolution(IPath.of(ref, path, decl)));
                    } catch(PathException e) {
                    }
                }
            }

        }
        return paths;
    }

    public <R, D> Set<IPath> addRef(Ref<R, D> ref, S scope) {
        // forall scope ->> scope', forall scope' -> decl, if ref ~ decl then new ref |-> decl
        Set<IPath> paths = Sets.newHashSet();
        if(refsAndDecls.putRef(scope, ref)) {
            for(ScopePath<S, L> path : reachability.from(scope)) {
                for(Decl<R, D> decl : refsAndDecls.getDecls(path.getTarget(), ref.getNamespace())) {
                    try {
                        paths.addAll(newResolution(IPath.of(ref, path, decl)));
                    } catch(PathException e) {
                    }
                }
            }

        }
        return paths;
    }

    public Set<IPath> addEdge(S source, L label, S target) {
        // new source..target
        Set<IPath> paths = Sets.newHashSet();
        if(edges.put(source, label, target)) {
            try {
                paths.addAll(newStep(IStep.of(source, label, target)));
            } catch(PathException e) {
            }
        }
        return paths;
    }

    public <R, D> Set<IPath> addImport(S scope, L label, Ref<R, D> ref) {
        // forall ref |-> decl, decl =label=> scope', new scope..scope'
        Set<IPath> paths = Sets.newHashSet();
        if(importsAndExports.putImport(scope, ref)) {
            for(ResolutionPath<S, L, R, D> path : resolution.from(ref)) {
                for(S target : importsAndExports.getExportScopes(path.getDecl(), label)) {
                    try {
                        paths.addAll(newStep(IStep.of(scope, label, target)));
                    } catch(PathException e) {
                    }
                }

            }
        }
        return paths;
    }

    public <R, D> Set<IPath> addExport(Decl<R, D> decl, L label, S scope) {
        // forall ref |-> decl, scope' =label=> ref, new scope'..scope
        Set<IPath> paths = Sets.newHashSet();
        if(importsAndExports.putExport(scope, decl)) {
            for(ResolutionPath<S, L, R, D> path : resolution.to(decl)) {
                for(S source : importsAndExports.getImportScopes(path.getRef(), label)) {
                    try {
                        paths.addAll(newStep(IStep.of(source, label, scope)));
                    } catch(PathException e) {
                    }
                }
            }
        }
        return paths;
    }

    // on new source..target
    private Set<IPath> newStep(IStep<S, L> step) {
        // forall source' ->> source, target ->> target', new source' ->> target'
        // NB. also take identity cases (scope ->> scope) into account
        Set<IPath> paths = Sets.newHashSet();
        try {
            paths.addAll(newPath(IPath.of(step)));
        } catch(PathException e) {
        }
        for(ScopePath<S, L> pre : reachability.to(step.getSource())) {
            try {
                paths.addAll(newPath(IPath.of(pre, step)));
            } catch(PathException e) {
            }
            for(ScopePath<S, L> post : reachability.from(step.getTarget())) {
                try {
                    paths.addAll(newPath(IPath.of(pre, step, post)));
                } catch(PathException e) {
                }
            }
        }
        for(ScopePath<S, L> post : reachability.from(step.getTarget())) {
            try {
                paths.addAll(newPath(IPath.of(step, post)));
            } catch(PathException e) {
            }
        }
        return paths;
    }

    // on new source ->> target
    private Set<IPath> newPath(ScopePath<S, L> path) {
        // forall ref -> source, target -> decl, if ref ~ decl then new ref |-> decl
        Set<IPath> paths = Sets.newHashSet();
        if(reachability.add(path)) {
            for(Ref<?, ?> ref : refsAndDecls.getRefs(path.getSource())) {
                paths.addAll(newPath(ref, path));
            }
        }
        return paths;
    }

    private <R, D> Set<IPath> newPath(Ref<R, D> ref, ScopePath<S, L> path) {
        Set<IPath> paths = Sets.newHashSet();
        Namespace<R, D> ns = ref.getNamespace();
        for(Decl<?, ?> decl : refsAndDecls.getDecls(path.getTarget(), ns)) {
            decl.cast(ns).ifPresent(mdecl -> {
                try {
                    paths.addAll(newResolution(IPath.of(ref, path, mdecl)));
                } catch(PathException e) {
                }
            });
        }
        return paths;
    }

    // on new ref..decl
    private <R, D> Set<IPath> newResolution(ResolutionPath<S, L, R, D> path) {
        // forall source =label=> ref, decl =label=> target, new source ->> target
        Set<IPath> paths = Sets.newHashSet();
        if(resolution.add(path)) {
            for(Entry<L, S> imp : importsAndExports.getImportScopes(path.getRef())) {
                for(S exp : importsAndExports.getExportScopes(path.getDecl(), imp.getLabel())) {
                    try {
                        paths.addAll(newStep(IStep.of(imp.getValue(), imp.getLabel(), path, exp)));
                    } catch(PathException e) {
                    }
                }
            }
        }
        return paths;
    }

}