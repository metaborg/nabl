package mb.statix.scopegraph.terms.path;

import java.util.Optional;

import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.scopegraph.path.IStep;

public final class Paths {

    public static final String PATH_SEPARATOR = " ";

    public static <V, L> IStep<V, L> edge(V source, L label, V target) {
        return Step.of(source, label, target);
    }

    public static <V, L> IScopePath<V, L> empty(V scope) {
        return EmptyScopePath.of(scope);
    }

    public static <V, L, R> IResolutionPath<V, L, R> resolve(IScopePath<V, L> path, R relation, V decl) {
        return ResolutionPath.of(path, relation, decl);
    }

    public static <V, L> Optional<IScopePath<V, L>> append(IScopePath<V, L> left, IScopePath<V, L> right) {
        return Optional.ofNullable(ComposedScopePath.of(left, right));
    }

    public static <V, L, R> Optional<IResolutionPath<V, L, R>> append(IScopePath<V, L> left,
            IResolutionPath<V, L, R> right) {
        return Optional.ofNullable(ComposedScopePath.of(left, right.getPath()))
                .map(p -> ResolutionPath.of(p, right.getRelation(), right.getDeclaration()));
    }

}