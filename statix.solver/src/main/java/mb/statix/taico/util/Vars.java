package mb.statix.taico.util;

import mb.nabl2.terms.ITermVar;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleManager;
import mb.statix.taico.scopegraph.reference.ModuleDelayException;
import mb.statix.taico.solver.SolverContext;

public class Vars {
    /**
     * Gets the owner of the given term variable.
     * 
     * @param termVar
     *      the termVar
     * @param manager
     *      the module manager to lookup modules in
     * 
     * @return
     *      the owner of the given term variable
     * 
     * @throws IllegalArgumentException
     *      If the given term is not a scope.
     */
    public static IModule getOwner(ITermVar termVar, ModuleManager manager) {
        return manager.getModule(termVar.getResource());
    }
    
    /**
     * Gets the owner of the given term variable without checking the access.
     * 
     * @param context
     *      the context
     * @param termVar
     *      the variable
     * 
     * @return
     *      the owner of the given term variable
     */
    public static IModule getOwnerUnchecked(SolverContext context, ITermVar termVar) {
        return context.getModuleUnchecked(termVar.getResource());
    }
    
    /**
     * Gets the owner of the given term variable without checking the access.
     * 
     * @param termVar
     *      the variable
     * 
     * @return
     *      the owner of the given term variable
     */
    public static IModule getOwnerUnchecked(ITermVar termVar) {
        return SolverContext.context().getModuleUnchecked(termVar.getResource());
    }
    
    /**
     * Gets the owner of the given term variable.
     * 
     * @param termVar
     *      the term variable
     * @param requester
     *      the requester of the owner
     * 
     * @return
     *      the owner of the given term variable
     *      
     * @throws ModuleDelayException
     *      If this request is not allowed.
     */
    public static IModule getOwner(ITermVar termVar, IModule requester) throws ModuleDelayException {
        return SolverContext.context().getModule(requester, termVar.getResource());
    }
    
    /**
     * Gets the owner of the given term variable.
     * 
     * @param termVar
     *      the term variable
     * @param requester
     *      the requester of the owner
     * 
     * @return
     *      the owner of the given term variable
     *      
     * @throws ModuleDelayException
     *      If this request is not allowed.
     */
    public static IModule getOwner(ITermVar termVar, String requesterId) throws ModuleDelayException {
        return SolverContext.context().getModule(requesterId, termVar.getResource());
    }
}
