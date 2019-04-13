package mb.statix.scopegraph.reference;

import mb.statix.taico.module.IModule;

public class IncompleteDataException extends ResolutionException {

    private static final long serialVersionUID = 1L;

    private final Object scope;
    private final Object relation;
    private final IModule module;

    public IncompleteDataException(Object scope, Object relation) {
        this(scope, relation, null);
    }
    
    public IncompleteDataException(Object scope, Object relation, IModule module) {
        super(scope + " incomplete in data " + relation);
        this.scope = scope;
        this.relation = relation;
        this.module = module;
    }

    @SuppressWarnings("unchecked") public <V> V scope() {
        return (V) scope;
    }

    @SuppressWarnings("unchecked") public <L> L relation() {
        return (L) relation;
    }
    
    /**
     * @return
     *      the module that is the cause of the incomplete data, can be null
     */
    public IModule getModule() {
        return module;
    }

}