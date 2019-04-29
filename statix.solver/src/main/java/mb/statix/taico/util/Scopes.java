package mb.statix.taico.util;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.Delay;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleManager;
import mb.statix.taico.solver.SolverContext;

public class Scopes {
    /**
     * Gets the scope represented by the given term.
     * 
     * @param term
     *      the term
     * 
     * @return
     *      the scope represented by the term
     * 
     * @throws IllegalArgumentException
     *      If the given term does not represent a scope.
     */
    public static Scope getScope(ITerm term) {
        return Scope.matcher().match(term)
                .orElseThrow(() -> new IllegalArgumentException("The given scope is not a scope!"));
    }
    
    /**
     * Gets the owner of the given term if it is a scope.
     * 
     * @param term
     *      the term
     * @param manager
     *      the module manager to lookup modules in
     * 
     * @return
     *      the owner of the given scope
     * 
     * @throws IllegalArgumentException
     *      If the given term is not a scope.
     */
    public static IModule getOwner(ITerm term, ModuleManager manager) {
        Scope scope = getScope(term);
        return manager.getModule(scope.getResource());
    }
    
    /**
     * Gets the owner of the given term if it is a scope.
     * 
     * @param term
     *      the scope
     * @param manager
     *      the module manager to lookup modules in
     * 
     * @return
     *      the owner of the given scope
     * 
     * @throws IllegalArgumentException
     *      If the given term is not a scope.
     */
    public static IModule getOwner(Scope term, ModuleManager manager) {
        return manager.getModule(term.getResource());
    }
    
    /**
     * Gets the owner of the given term if it is a scope.
     * 
     * @param term
     *      the scope
     * @param manager
     *      the module manager to lookup modules in
     * 
     * @return
     *      the owner of the given scope
     *      
     * @throws Delay
     *      If this request is not allowed.
     * 
     * @throws IllegalArgumentException
     *      If the given term is not a scope.
     */
    public static IModule getOwner(Scope term, IModule requester, SolverContext context) throws Delay {
        return context.getModule(requester, term.getResource());
    }
}
