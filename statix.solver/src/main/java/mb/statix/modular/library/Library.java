package mb.statix.modular.library;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.input.ClassLoaderObjectInputStream;

import mb.statix.constraints.CTrue;
import mb.statix.modular.incremental.Flag;
import mb.statix.modular.module.IModule;
import mb.statix.modular.module.ModulePaths;
import mb.statix.modular.solver.Context;
import mb.statix.modular.solver.MSolverResult;
import mb.statix.modular.solver.ModuleSolver;
import mb.statix.modular.solver.state.IMState;
import mb.statix.scopegraph.terms.Scope;

public class Library implements Serializable {
    private static final long serialVersionUID = 1L;

    private Set<IModule> modules = new HashSet<>();
    private Map<IModule, IMState> states = new HashMap<>();
    
    public Library(Iterable<IModule> modules, Context context) {
        CTrue ctrue = new CTrue();
        for (IModule module : modules) {
            MSolverResult result = context.getResult(module);
            if (result != null && (result.hasErrors() || result.hasDelays())) {
                throw new IllegalStateException("Cannot create library: module " + module + " has errors.");
            }
            
            IModule copy = module.copy();
            copy.setFlag(Flag.CLEAN);
            copy.setInitialization(ctrue);
            copy.setLibraryModule();
            this.modules.add(copy);
            
            IMState state = context.getState(module);
            this.states.put(copy, state.toLibraryState(copy));
        }
    }
    
    /**
     * Adds this library to the given context.
     * 
     * @param context
     *      the context
     */
    public void addToContext(Context context, IMState rootState) {
        Map<IModule, IMState> libRoots = new HashMap<>();
        
        for (Entry<IModule, IMState> entry : states.entrySet()) {
            IModule module = entry.getKey();
            module.setLibraryModule();
            
            IMState state = entry.getValue();
            context.addModule(module);
            context.setState(module, state);
            ModuleSolver.librarySolver(state);
            
            //Add the library as a child of the original
            if (ModulePaths.pathLength(module.getId()) == 1) {
                libRoots.put(module, state);
            }
        }
        
        for (Entry<IModule, IMState> entry : libRoots.entrySet()) {
            IModule libRoot = entry.getKey();
            IMState libRootState = entry.getValue();
            
            //Replace the global scope of the library with the one from the root
            Scope globalScope = rootState.scopes().stream().findFirst().get();
            libRootState.scopeGraph().substituteForLibrary(null, globalScope);
            
            //Add the library as child of the root
            rootState.scopeGraph().addChild(libRoot);
        }
    }
    
    // --------------------------------------------------------------------------------------------
    // Object methods
    // --------------------------------------------------------------------------------------------
    
    @Override
    public int hashCode() {
        return modules.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Library)) return false;
        return modules.equals(((Library) obj).modules);
    }
    
    // --------------------------------------------------------------------------------------------
    // Serialization
    // --------------------------------------------------------------------------------------------
    
    /**
     * Saves this context as a library to the given file.
     * 
     * @param file
     *      the file to save to
     * 
     * @throws IOException
     *      If an I/O error occurs.
     */
    public void saveAsLibrary(File file) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(
                        new FileOutputStream(file)))) {
            oos.writeObject(this);
        }
    }
    
    /**
     * @param file
     *      the file to read from
     * 
     * @return
     *      the read library
     * 
     * @throws IOException
     *      If the given file does not exist or an I/O error occurred.
     */
    public static Library readLibrary(File file) throws IOException {
        try (ObjectInputStream ois = new ClassLoaderObjectInputStream(
                Library.class.getClassLoader(),
                new BufferedInputStream(new FileInputStream(file)))) {
            return (Library) ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to load library from " + file +
                    ": library is incompatible. Is it written with a different version of spoofax?");
        }
    }
}
