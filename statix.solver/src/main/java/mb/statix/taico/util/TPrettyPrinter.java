package mb.statix.taico.util;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.spoofax.StatixTerms;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModulePaths;
import mb.statix.taico.solver.SolverContext;

public class TPrettyPrinter {
    private static Map<Scope, Integer> scopeNumbers = new HashMap<>();
    private static SolverContext context;
    private static int scopeCounter;
    private static final IUnifier NULL_UNIFIER = PersistentUnifier.Immutable.of();
    
    /**
     * Prints the given scope with the module it belongs to and a unique id.
     * 
     * @param scope
     *      the scope to print
     * 
     * @return
     *      the formatted string
     * 
     * @see #printScope(Scope, boolean, boolean) {@code printScope(scope, true, true)}
     */
    public static String printScope(Scope scope) {
        return printScope(scope, true, true);
    }
    
    /**
     * @param scope
     *      the scope to print
     * @param module
     *      if the module should be printed
     * @param uid
     *      if an unique id for the scope should be printed
     * 
     * @return
     *      the formatted string
     */
    public static String printScope(Scope scope, boolean module, boolean uid) {
        StringBuilder sb = new StringBuilder();
        if (uid) sb.append(scopeNumber(scope)).append('-');
        if (module) sb.append(printModule(scope.getResource())).append('-');
        sb.append(printScopeName(scope.getName()));
        return sb.toString();
    }
    
    public static String printScopeFancy(Scope scope) {
        return printScopeName(scope.getName()) + " (" + scopeNumber(scope) + ")";
    }
    
    public static String printScopeName(String name) {
        int index = name.indexOf('.');
        if (index == -1) return name;
        return name.substring(0, index);
    }
    
    /**
     * Gets the unique number for the given scope. If the given scope does not have a unique
     * number yet, a new one is created.
     * 
     * @param scope
     *      the scope
     * 
     * @return
     *      the unique number associated with the given scope
     */
    public static int scopeNumber(Scope scope) {
        //Reset
        if (context != SolverContext.context()) {
            scopeNumbers.clear();
            context = SolverContext.context();
            scopeCounter = 0;
        }
        
        return scopeNumbers.computeIfAbsent(scope, s -> scopeCounter++);
    }
    
    public static String printModule(IModule module) {
        if (moduleUniqueness()) {
            return module.getName();
        } else {
            return printModule(module.getId());
        }
    }
    
    public static String printModule(String module) {
        if (moduleUniqueness()) {
            return ModulePaths.getName(module);
        }

        //We want to cut off the root string, and replace it with ~
        String root = SolverContext.context().getRootModule().getId();
        if (root.length() == module.length()) {
            //For the root module itself, just print ~
            return "~";
            //sb.append(base.replace("eclipse:///", ""));
        }
        
        return new StringBuilder("~%").append(module, root.length(), module.length()).toString();
    }
    
    public static String printLabel(ITerm term) {
        return label().match(term).orElse(term.toString());
    }
    
    public static String printTerm(ITerm term) {
        return printTerm(term, NULL_UNIFIER);
    }
    
    public static String printTerm(ITerm term, IUnifier unifier) {
        return term(unifier).match(term, unifier).get();
    }
    
    /**
     * Prints the given list in compact format.
     * If the list is empty, the empty string is returned. If the list contains a single element,
     * that element is printed. Otherwise, the list is printed as normal.
     * 
     * @param list
     *      the list
     * @param unifier
     *      the unifier
     * 
     * @return
     *      the compact string representation of the list
     */
    public static String printListCompact(List<? extends ITerm> list, IUnifier unifier) {
        if (list.isEmpty()) return "";
        if (list.size() == 1) return printTerm(list.get(0), unifier);
        return formatList(convertList(list, unifier));
    }
    
    /**
     * Prints the given list.
     * 
     * @param list
     *      the list
     * @param unifier
     *      the unifier
     * 
     * @return
     *      the string representation of the list
     */
    public static String printList(List<? extends ITerm> list, IUnifier unifier) {
        return formatList(convertList(list, unifier));
    }
    
    //---------------------------------------------------------------------------------------------
    //Matchers
    //---------------------------------------------------------------------------------------------
    
    /**
     * @return
     *      a matcher which converts any statix term to a string
     */
    public static IMatcher<String> term(IUnifier unifier) {
        //@formatter:off
        return M.cases(
                M.stringValue(),
                var(),
                scope(),
                occurrence(unifier),
                list(unifier),
                tuple(unifier),
                label(),
                appl(unifier),            //Fallback for an application
                M.term(t -> t.toString()) //Fallback for any other term
        );
        //@formatter:on
    }
    
    public static IMatcher<String> occurrence(IUnifier unifier) {
        //This has a recursive definition
        return M.appl3(StatixTerms.OCCURRENCE_OP, M.stringValue(), M.listElems(), M.term(),
                (t, ns, name, ast) -> ns + "{" + printListCompact(name, unifier) + "}");
    }
    
    public static IMatcher<String> label() {
        return M.appl1("Label", M.stringValue(), (t, s) -> s);
    }
    
    public static IMatcher<String> tuple(IUnifier unifier) {
        return M.tuple(t -> "(" + String.join(", ", convertList(t.getArgs(), unifier)) + ")");
    }
    
    public static IMatcher<String> list(IUnifier unifier) {
        return M.listElems().map(l -> formatList(convertList(l, unifier)));
    }
    
    public static IMatcher<String> appl(IUnifier unifier) {
        return M.appl(t -> t.getOp() + "(" + convertList(t.getArgs(), unifier) + ")");
    }
    
    public static IMatcher<String> var() {
        return M.var(v -> "?" + printModule(v.getResource()) + "-" + v.getName());
    }
    
    public static IMatcher<String> scope() {
        return Scope.matcher().map(TPrettyPrinter::printScopeFancy);
    }
    
    //---------------------------------------------------------------------------------------------
    //Helper methods
    //---------------------------------------------------------------------------------------------
    
    private static boolean moduleUniqueness() {
        return SolverContext.context().getModuleManager().areModuleNamesUnique();
    }
    
    public static List<String> convertList(List<? extends ITerm> list, IUnifier unifier) {
        List<String> tbr = new ArrayList<>(list.size());
        for (ITerm term : list) {
            tbr.add(term(unifier).match(term, unifier).get());
        }
        return tbr;
    }

    private static String formatList(List<String> l) {
        return "[" + String.join(", ", l) + "]";
    }
}
