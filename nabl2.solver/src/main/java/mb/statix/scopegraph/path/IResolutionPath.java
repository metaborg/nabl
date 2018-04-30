package mb.statix.scopegraph.path;

public interface IResolutionPath<S, L, O> extends IPath<S, L, O> {

    IScopePath<S, L, O> getPath();

    Object getLabel();
    
    O getDeclaration();

}