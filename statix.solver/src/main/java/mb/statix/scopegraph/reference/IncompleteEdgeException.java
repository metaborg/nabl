package mb.statix.scopegraph.reference;

import mb.statix.taico.module.IModule;

public class IncompleteEdgeException extends ResolutionException {

    private static final long serialVersionUID = 1L;

    private final Object scope;
    private final Object label;
    private final IModule module;

    public IncompleteEdgeException(Object scope, Object label) {
        this(scope, label, null);
    }
    
    public IncompleteEdgeException(Object scope, Object label, IModule module) {
        super(scope + " incomplete in edge " + label);
        this.scope = scope;
        this.label = label;
        this.module = module;
    }

    @SuppressWarnings("unchecked") public <V> V scope() {
        return (V) scope;
    }

    @SuppressWarnings("unchecked") public <L> L label() {
        return (L) label;
    }
    
    /**
     * @return
     *      the module that is the cause for the incomplete edge, can be null
     */
    public IModule getModule() {
        return module;
    }

}