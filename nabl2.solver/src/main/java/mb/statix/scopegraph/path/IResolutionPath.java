package mb.statix.scopegraph.path;

public interface IResolutionPath<S, L, R, O> extends IPath<S, L, O> {

    IScopePath<S, L, O> getPath();

    R getRelation();
    
    O getDeclaration();

}