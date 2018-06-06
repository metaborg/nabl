package mb.statix.scopegraph.path;

public interface IResolutionPath<V, L, R> extends IPath<V, L> {

    IScopePath<V, L> getPath();

    R getRelation();

    V getDeclaration();

}