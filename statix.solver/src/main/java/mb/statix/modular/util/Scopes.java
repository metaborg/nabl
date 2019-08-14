package mb.statix.modular.util;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.optionals.Optionals;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.modular.module.IModule;
import mb.statix.modular.module.ModuleManager;
import mb.statix.modular.scopegraph.reference.ModuleDelayException;
import mb.statix.modular.solver.Context;
import mb.statix.modular.solver.state.IMState;
import mb.statix.scopegraph.terms.IScope;
import mb.statix.scopegraph.terms.Scope;

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
        if (term instanceof Scope) return (Scope) term;
        
        return Scope.matcher().match(term)
                .orElseThrow(() -> new IllegalArgumentException("The given scope is not a scope!"));
    }
    
    /**
     * Gets the scope represented by the given term.
     * 
     * @param term
     *      the term
     * @param unifier
     *      the unifier
     * 
     * @return
     *      the scope represented by the term
     * 
     * @throws IllegalArgumentException
     *      If the given term does not represent a scope.
     */
    public static Scope getScope(ITerm term, IUnifier unifier) {
        if (term instanceof Scope) return (Scope) term;
        
        return Scope.matcher().match(term, unifier)
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
     * Gets the owner of the given scope without checking the access.
     * 
     * @param context
     *      the context
     * @param term
     *      the scope
     * 
     * @return
     *      the owner of the given scope
     */
    public static IModule getOwnerUnchecked(Context context, IScope term) {
        return context.getModuleUnchecked(term.getResource());
    }
    
    /**
     * Gets the owner of the given scope without checking the access.
     * 
     * @param term
     *      the scope
     * 
     * @return
     *      the owner of the given scope
     */
    public static IModule getOwnerUnchecked(ITerm term) {
        return Context.context().getModuleUnchecked(getScope(term).getResource());
    }
    
    /**
     * Gets the owner of the given scope without checking the access.
     * 
     * @param term
     *      the scope
     * 
     * @return
     *      the owner of the given scope
     */
    public static IModule getOwnerUnchecked(ITerm term, IUnifier unifier) {
        return Context.context().getModuleUnchecked(getScope(term, unifier).getResource());
    }
    
    /**
     * Gets the state of the owner of the given term if it is a scope.
     * 
     * @param term
     *      the scope
     * 
     * @return
     *      the state of the owner of the given scope
     * 
     * @throws IllegalArgumentException
     *      If the given term is not a scope.
     */
    public static IMState getStateUnchecked(ITerm term) {
        return Context.context().getState(getScope(term).getResource());
    }
    
    /**
     * Gets the owner of the given term if it is a scope.
     * 
     * @param term
     *      the scope
     * @param requester
     *      the requester of the owner
     * 
     * @return
     *      the owner of the given scope
     *      
     * @throws ModuleDelayException
     *      If this request is not allowed.
     * 
     * @throws IllegalArgumentException
     *      If the given term is not a scope.
     */
    public static IModule getOwner(IScope scope, IModule requester) throws ModuleDelayException {
        return Context.context().getModule(requester, scope.getResource());
    }
    
    /**
     * Gets the owner of the given term if it is a scope.
     * 
     * @param term
     *      the scope
     * @param requester
     *      the requester of the owner
     * 
     * @return
     *      the owner of the given scope
     *      
     * @throws ModuleDelayException
     *      If this request is not allowed.
     * 
     * @throws IllegalArgumentException
     *      If the given term is not a scope.
     */
    public static IModule getOwner(IScope scope, String requester) throws ModuleDelayException {
        return Context.context().getModule(requester, scope.getResource());
    }
    
    /**
     * Returns a list of the terms in the given list that represent scopes.
     * The order of the returned scopes corresponds to the original order.
     * 
     * @param terms
     *      the terms
     * 
     * @return
     *      the terms in the given list that represent scopes
     */
    public static List<Scope> getScopeTerms(List<ITerm> terms) {
        List<Scope> scopes = new ArrayList<>();
        for (ITerm term : terms) {
            Scope scope = Scope.matcher().match(term).orElse(null);
            if (scope != null) scopes.add(scope);
        }
        return scopes;
    }
    
    /**
     * @param term
     *      the term
     * @param unifier
     *      the unifier
     * 
     * @return
     *      a set of all the scopes that are in the given term
     */
    public static Set<Scope> getScopesInTerm(ITerm term, IUnifier unifier) {
        return M.<Set<Scope>>casesFix(m -> Iterables2.from(
                Scope.matcher().map(s -> Collections.singleton(s)),
                M.listElems(m).map(l -> l.stream().flatMap(s -> s.stream()).collect(Collectors.toSet())),
                M.appl(t -> t.getArgs()).map(l -> l.stream().flatMap(i -> Optionals.stream(m.match(i, unifier)).flatMap(c -> c.stream())).collect(Collectors.toSet())),
                M.var(v -> m.match(unifier.findRecursive(v), unifier).orElseGet(Collections::emptySet)),
                M.term(t -> Collections.emptySet())
        )).match(term, unifier).orElse(Collections.emptySet());
    }
    
    /**
     * The given consumer is called for each scope encountered in the given term.
     * <p>
     * Please note that the consumer can be called with the same scope multiple times.
     * 
     * @param term
     *      the term
     * @param unifier
     *      the unifier
     * @param consumer
     *      the consumer
     */
    public static void getScopesInTerm(ITerm term, IUnifier unifier, Consumer<Scope> consumer) {
        ITerm instantiatedTerm = unifier.findRecursive(term);
        _getScopesInTerm(instantiatedTerm, consumer);
    }
    
    private static void _getScopesInTerm(ITerm term, Consumer<Scope> consumer) {
        Scope scope = Scope.matcher().match(term).orElse(null);
        if (scope != null) {
            consumer.accept(scope);
            return;
        }
        
        List<? extends ITerm> list;
        try {
            list = M.cases(
                    M.listElems(),
                    M.appl(t -> t.getArgs()))
                .match(term).orElse(null);
        } catch (IllegalStateException ex) {
            System.err.println("Unable to find scopes in term " + TPrettyPrinter.prettyPrint(term) + ": " + ex.getMessage());
            return;
        }
        
        if (list == null) return;
        for (ITerm listTerm : list) {
            _getScopesInTerm(listTerm, consumer);
        }
    }
}
