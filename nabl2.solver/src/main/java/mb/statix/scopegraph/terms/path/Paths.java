package mb.statix.scopegraph.terms.path;

import java.util.Optional;

import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.scopegraph.path.IStep;

public final class Paths {

    public static final String PATH_SEPARATOR = " ";

    public static <S, L, O> IStep<S, L, O> edge(S source, L label, S target) {
        return ImmutableStep.of(source, label, target);
    }

    public static <S, L, O> IScopePath<S, L, O> empty(S scope) {
        return ImmutableEmptyScopePath.of(scope);
    }

    public static <S, L, O> IResolutionPath<S, L, O> resolve(IScopePath<S, L, O> path, O decl) {
        return ImmutableResolutionPath.of(path, decl);
    }

    public static <S, L, O> Optional<IScopePath<S, L, O>> append(IScopePath<S, L, O> left, IScopePath<S, L, O> right) {
        return Optional.ofNullable(ImmutableComposedScopePath.of(left, right));
    }

    public static <S, L, O> Optional<IResolutionPath<S, L, O>> append(IScopePath<S, L, O> left,
            IResolutionPath<S, L, O> right) {
        return Optional.ofNullable(ImmutableComposedScopePath.of(left, right.getPath()))
                .map(p -> ImmutableResolutionPath.of(p, right.getDeclaration()));
    }

}