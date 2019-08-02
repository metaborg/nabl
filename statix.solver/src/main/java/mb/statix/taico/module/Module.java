package mb.statix.taico.module;

import static mb.statix.taico.solver.SolverContext.context;

import java.util.List;

import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.taico.incremental.Flag;
import mb.statix.taico.solver.state.MState;
import mb.statix.taico.util.StablePriorityQueue;

/**
 * Basic implementation of {@link IModule}. The identifiers are not automatically generated.
 */
public class Module implements IModule {
    private static final long serialVersionUID = 1L;
    
    private final String name;                                             //Stateless
    private final String parentId;                                         //Stateless
    private final String id;                                               //Stateless
    private IConstraint initialization;                                    //Stateful, old value unimportant
    private StablePriorityQueue<Flag> flags = new StablePriorityQueue<>(); //Stateful, old value unimportant
    
    /**
     * Creates a new top level module.
     * 
     * @param name
     *      the name of the module
     * @param spec
     *      the spec
     */
    private Module(String name) {
        this.name = name;
        this.parentId = null;
        this.id = (parentId == null ? name : ModulePaths.build(parentId, name));
        context().addModule(this);
    }
    
    /**
     * Constructor for creating child modules.
     * 
     * @param name
     *      the name of the child
     * @param parent
     *      the parent module
     */
    private Module(String name, IModule parent) {
        this.name = name;
        this.parentId = parent == null ? null : parent.getId();
        this.id = (parentId == null ? name : ModulePaths.build(parentId, name));
        context().addModule(this);
    }

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public String getParentId() {
        return parentId;
    }

    @Override
    public Module createChild(String name, List<Scope> canExtend, IConstraint constraint, boolean transferDependencies) {
        System.err.println("Creating child module " + name + " on " + this + ". Transferring dependencies: " + transferDependencies);
        Module child = new Module(name, this);
        child.setInitialization(constraint);
        new MState(child, getScopeGraph().createChild(child, canExtend));
        if (transferDependencies) {
            context().transferDependencies(child.getId());
        } else {
            context().getDependencies(child.getId());
        }
        return child;
    }
    
    @Override
    public StablePriorityQueue<Flag> getFlags() {
        return flags;
    }
    
    // --------------------------------------------------------------------------------------------
    // Initialization
    // --------------------------------------------------------------------------------------------
    
    @Override
    public IConstraint getInitialization() {
        return initialization;
    }
    
    @Override
    public void setInitialization(IConstraint constraint) {
        this.initialization = constraint;
    }
    
    // --------------------------------------------------------------------------------------------
    // Copy
    // --------------------------------------------------------------------------------------------

    @Override
    @Deprecated
    public Module copy() {
        return new Module(this);
    }

    /**
     * Copy constructor. The module is not added to the context.
     * 
     * @param original
     *      the module to copy
     */
    private Module(Module original) {
        this.name = original.name;
        this.parentId = original.parentId;
        this.id = original.id;
        this.flags = new StablePriorityQueue<>(original.flags);
        this.initialization = original.initialization;
    }

    // --------------------------------------------------------------------------------------------
    // Object methods
    // --------------------------------------------------------------------------------------------
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Module)) return false;
        
        assert !id.equals(((Module) obj).getId()) : "Module identifiers are equal but modules are not the same instance! (id: " + id + ")";
        return id.equals(((Module) obj).getId());
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return id;
    }
    
    // --------------------------------------------------------------------------------------------
    // Static initialization
    // --------------------------------------------------------------------------------------------
    
    /**
     * Creates a new top level module and its corresponding state.
     * 
     * @param name
     *      the name of the module
     * 
     * @return
     *      the created module
     */
    public static Module topLevelModule(String name) {
        Module module = new Module(name);
        MState.topLevelState(module);
        return module;
    }
}
