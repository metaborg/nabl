package mb.statix.modular.module;

import static mb.statix.modular.solver.Context.context;

import java.util.List;

import mb.statix.modular.incremental.Flag;
import mb.statix.modular.solver.state.MState;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.util.collection.StablePriorityQueue;

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
    private transient boolean libraryModule;
    
    /**
     * Creates a new top level module.
     * 
     * @param name
     *      the name of the module
     * @param spec
     *      the spec
     */
    protected Module(String name) {
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
    protected Module(String name, IModule parent) {
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
    public IModule createChild(String name, List<Scope> canExtend, IConstraint constraint, boolean transferDependencies) {
//        TDebug.DEV_OUT.info("Creating child module " + name + " on " + this + ". Transferring dependencies: " + transferDependencies);
        
        //Reuse an existing child module if possible
        String childId = ModulePaths.build(this.id, name);
        IModule child = context().getModuleUnchecked(childId);
        if (child == null) {
            child = new Module(name, this);
        } else if (!context().getModuleManager().hasModule(childId)) {
            context().addModule(child);
        }
        child.setFlag(Flag.NEW);
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
    
    @Override
    public boolean isLibraryModule() {
        return libraryModule;
    }
    
    @Override
    public void setLibraryModule() {
        libraryModule = true;
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
    protected Module(Module original) {
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
    
    // --------------------------------------------------------------------------------------------
    // Serialization
    // --------------------------------------------------------------------------------------------
    
    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
        out.defaultWriteObject();
        out.writeBoolean(libraryModule);
    }
    
    private void readObject(java.io.ObjectInputStream in)
        throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        try {
            this.libraryModule = in.readBoolean();
        } catch (java.io.OptionalDataException | java.io.EOFException ex) {
            //Ignore, old version
        }
    }
}
