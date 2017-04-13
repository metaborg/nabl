package org.metaborg.meta.nabl2.scopegraph.fixedpoint;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;
import org.metaborg.meta.nabl2.scopegraph.path.IStep;
import org.metaborg.meta.nabl2.scopegraph.terms.path.Paths;
import org.metaborg.meta.nabl2.util.collections.HashFunction;
import org.metaborg.meta.nabl2.util.collections.HashRelation3;
import org.metaborg.meta.nabl2.util.collections.IFunction;
import org.metaborg.meta.nabl2.util.collections.IRelation3;
import org.metaborg.meta.nabl2.util.functions.Action1;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.Queues;

public class FixedpointScopeGraph<S extends IScope, L extends ILabel, O extends IOccurrence> {

    // scope graph
    private final IFunction.Mutable<O, S> decls;
    private final IFunction.Mutable<O, S> refs;
    private final IRelation3.Mutable<S, L, S> edges;
    private final IRelation3.Mutable<O, L, S> exports;
    private final IRelation3.Mutable<O, L, S> imports;

    private final ScopePaths<S, L, O> paths;

    private final IPathResolver<S, L, O> resolver;

    public FixedpointScopeGraph(IPathResolver<S, L, O> resolver) {
        this.decls = HashFunction.create();
        this.refs = HashFunction.create();
        this.edges = HashRelation3.create();
        this.exports = HashRelation3.create();
        this.imports = HashRelation3.create();
        this.paths = new ScopePaths<>();
        this.resolver = resolver;
    }

    // ------------------------------------------------------------

    public boolean addDecl(S scope, O decl) {
        // forall scope' ->> scope, ref -> scope', if ref ~ decl then ref |-> decl
        if(decls.put(decl, scope)) {
            for(IScopePath<S, L, O> path : resolver.scopePaths().inverse().get(scope)) {
                for(O ref : refs.inverse().get(path.getSource())) {
                    Paths.resolve(ref, path, decl).ifPresent(this::addResolution);
                }
            }
            return true;
        }
        return false;
    }

    public boolean addRef(O ref, S scope) {
        // forall scope ->> scope', forall scope' -> decl, if ref ~ decl then new ref |-> decl
        if(refs.put(ref, scope)) {
            for(IScopePath<S, L, O> path : resolver.scopePaths().get(scope)) {
                for(O decl : decls.inverse().get(path.getTarget())) {
                    Paths.resolve(ref, path, decl).ifPresent(this::addResolution);
                }
            }
            return true;
        }
        return false;
    }

    public boolean addEdge(S source, L label, S target) {
        // new source..target
        if(edges.put(source, label, target)) {
            addStep(Paths.direct(source, label, target));
            return true;
        }
        return false;
    }

    public boolean addImport(S scope, L label, O ref) {
        // forall ref |-> decl, decl =label=> scope', new scope..scope'
        if(imports.put(ref, label, scope)) {
            for(IResolutionPath<S, L, O> path : resolver.resolutionPaths().get(ref)) {
                for(S exp : exports.get(path.getDeclaration(), label)) {
                    addStep(Paths.named(scope, label, path, exp));
                }
            }
            return true;
        }
        return false;
    }

    public boolean addExport(O decl, L label, S scope) {
        // forall ref |-> decl, scope' =label=> ref, new scope'..scope
        if(imports.put(decl, label, scope)) {
            for(IResolutionPath<S, L, O> path : resolver.resolutionPaths().inverse().get(decl)) {
                for(S imp : imports.get(path.getReference(), label)) {
                    addStep(Paths.named(imp, label, path, scope));
                }
            }
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------

    // on new source..target
    private void addStep(IStep<S, L, O> step) {
        work(Queues.newArrayDeque(Iterables2.singleton(step)), Queues.newArrayDeque(), Queues.newArrayDeque());
    }

    // on new ref..decl
    private void addResolution(IResolutionPath<S, L, O> path) {
        work(Queues.newArrayDeque(), Queues.newArrayDeque(), Queues.newArrayDeque(Iterables2.singleton(path)));
    }

    private void work(Queue<IStep<S, L, O>> stepQueue, Queue<IScopePath<S, L, O>> scopePathQueue,
            Queue<IResolutionPath<S, L, O>> resolutionPathQueue) {
        while(exhaust(stepQueue, step -> workStep(step, scopePathQueue))
                || exhaust(scopePathQueue, path -> workScopePath(path, resolutionPathQueue))
                || exhaust(resolutionPathQueue, path -> workResolutionPath(path, stepQueue)))
            ;
    }

    private static <T> boolean exhaust(Queue<T> queue, Action1<T> f) {
        boolean progress = !queue.isEmpty();
        while(!queue.isEmpty()) {
            f.apply(queue.remove());
        }
        return progress;
    }

    private void workStep(IStep<S, L, O> step, Queue<IScopePath<S, L, O>> scopePathQueue) {
        if(paths.add(step)) {
            // forall source' ->> source, target ->> target', new source' ->> target'
            // NB. also take identity cases (scope ->> scope) into account
            for(IScopePath<S, L, O> leftInclusivePath : paths.inverse().get(step.getSource())) {
                final Optional<IScopePath<S, L, O>> maybeLeft = Paths.append(leftInclusivePath, step);
                maybeLeft.ifPresent(scopePathQueue::add);
                for(IScopePath<S, L, O> rightPath : paths.get(step.getTarget())) {
                    Paths.append(step, rightPath).ifPresent(scopePathQueue::add);
                    maybeLeft.ifPresent(left -> Paths.append(left, rightPath).ifPresent(scopePathQueue::add));
                }
            }
        }
    }

    private void workScopePath(IScopePath<S, L, O> path, Queue<IResolutionPath<S, L, O>> resolutionPathQueue) {
        if(resolver.add(path)) {
            // forall ref -> source, target -> decl, if ref ~ decl then new ref |-> decl
            for(O ref : refs.inverse().get(path.getSource())) {
                for(O decl : decls.inverse().get(path.getTarget())) {
                    Paths.resolve(ref, path, decl).ifPresent(resolutionPathQueue::add);
                }
            }
        }
    }

    private void workResolutionPath(IResolutionPath<S, L, O> path, Queue<IStep<S, L, O>> stepQueue) {
        if(resolver.add(path)) {
            for(Entry<L, S> labelAndSource : imports.get(path.getReference())) {
                for(S target : exports.get(path.getDeclaration(), labelAndSource.getKey())) {
                    stepQueue.add(Paths.named(labelAndSource.getValue(), labelAndSource.getKey(), path, target));
                }
            }
        }
    }

}