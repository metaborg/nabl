package mb.statix.modular.util;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.checkerframework.checker.nullness.qual.Nullable;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.nabl2.util.TermFormatter;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.modular.module.IModule;
import mb.statix.modular.module.ModulePaths;
import mb.statix.modular.solver.Context;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.spoofax.StatixTerms;

public class TPrettyPrinter {
    private static Map<Scope, Integer> scopeNumbers = new HashMap<>();
    private static Context context;
    private static int scopeCounter;
    private static boolean fixScopeNumbers;
    private static final IUnifier NULL_UNIFIER = PersistentUnifier.Immutable.of();
    
    //Matchers
    public static final IMatcher<String> SCOPE = Scope.matcher().map(TPrettyPrinter::printScope);
    public static final IMatcher<String> SCOPE_FANCY = Scope.matcher().map(TPrettyPrinter::printScopeFancy);
    public static final IMatcher<String> VAR = M.var(v -> "?" + printModule(v.getResource()) + "-" + v.getName());
    public static final IMatcher<String> LABEL = M.cases(
            M.appl1("Label", M.stringValue(), (t, s) -> s),
            M.appl0("Decl", t -> "decl"));
    
    public static void fixScopeNumbers() {
        fixScopeNumbers = true;
        context = Context.context();
    }
    
    public static void unfixScopeNumbers() {
        fixScopeNumbers = false;
        if (context != Context.context()) {
            scopeNumbers.clear();
            scopeCounter = 0;
            context = Context.context();
        }
    }
    
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
        if (!fixScopeNumbers && context != Context.context()) {
            scopeNumbers.clear();
            context = Context.context();
            scopeCounter = 0;
        }
        
        return scopeNumbers.computeIfAbsent(scope, s -> scopeCounter++);
    }
    
    public static String printModule(IModule module, boolean longFormat) {
        //Root modules       -> ~
        //Module for project -> <project> / ~%<project>
        //Unique modules     -> module name
        //Other              -> short path
        if (module.getParentId() == null) {
            return "~";
        } else if (!longFormat && moduleUniqueness()) {
            if (module.getName().equals("||")) {
                return "<project>";
            } else {
                return module.getName();
            }
        } else {
            return printModule(module.getId());
        }
    }
    
    public static String printModule(String module, boolean longFormat) {
        //Root modules       -> ~
        //Module for project -> <project> / ~%<project>
        //Unique modules     -> module name
        //Other              -> short path
        String root = Context.context().getRootModule().getId();
        if (root.length() == module.length()) {
            //For the root module itself, just print ~
            return "~";
        }
        
        String module2 = module.replace("||", "<project>");
        if (!longFormat && moduleUniqueness()) {
            return ModulePaths.getName(module2);
        }
        
        return new StringBuilder("~").append(module2, root.length(), module2.length()).toString();
    }
    
    public static String printModule(IModule module) {
        return printModule(module, false);
    }
    
    public static String printModule(String module) {
        return printModule(module, false);
    }
    
    public static String printLabel(ITerm term) {
        return label().match(term).orElse(term.toString());
    }
    
    public static String printTerm(ITerm term) {
        return printTerm(term, NULL_UNIFIER);
    }
    
    public static String printTerm(ITerm term, IUnifier unifier) {
        try {
            return term(unifier).match(term, unifier).get();
        } catch (IllegalStateException ex) {
            return term.toString();
        }
    }
    
    public static String printTerm(ITerm term, IUnifier unifier, boolean fancyScopes) {
        try {
            return term(unifier, fancyScopes).match(term, unifier).get();
        } catch (IllegalStateException ex) {
            return term.toString();
        }
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
    
    /**
     * Prints the given edge in the form {@code scope -label->}.
     * 
     * @param scope
     *      the scope of the edge
     * @param label
     *      the label of the edge
     * 
     * @return
     *      the string representation of the edge
     */
    public static String printEdge(Scope scope, ITerm label) {
        return printScopeFancy(scope) + " -" + printLabel(label) + "->";
    }
    
    // --------------------------------------------------------------------------------------------
    // Printing objects
    // --------------------------------------------------------------------------------------------
    
    /**
     * Pretty prints the given object using with an empty unifier within the current context.
     */
    public static String prettyPrint(Object object) {
        return prettyPrint(object, NULL_UNIFIER);
    }
    
    /**
     * Pretty prints the given object using the given unifier within the current context.
     */
    public static String prettyPrint(Object object, IUnifier unifier) {
        if (object instanceof IModule) return printModule((IModule) object);
        if (object instanceof Scope) return printScope((Scope) object);
        if (object instanceof ITerm) return printTerm((ITerm) object, unifier);
        if (object instanceof Entry) {
            Entry<?, ?> entry = (Entry<?, ?>) object;
            return prettyPrint(entry.getKey(), unifier) + "=" + prettyPrint(entry.getValue(), unifier);
        }
        if (object instanceof Iterable) {
            StringBuilder sb = new StringBuilder("[");
            for (Object obj : (Iterable<?>) object) {
                sb.append(prettyPrint(obj, unifier));
                sb.append(", ");
            }
            if (sb.length() > 2) sb.setLength(sb.length() - 2);
            sb.append(']');
            return sb.toString();
        }
        if (object instanceof Map) {
            StringBuilder sb = new StringBuilder("{");
            for (Entry<?, ?> obj : ((Map<?, ?>) object).entrySet()) {
                sb.append(prettyPrint(obj.getKey(), unifier));
                sb.append('=');
                sb.append(prettyPrint(obj.getValue(), unifier));
                sb.append(", ");
            }
            if (sb.length() > 2) sb.setLength(sb.length() - 2);
            sb.append('}');
            return sb.toString();
        }
        if (object instanceof IRelation3) {
            @SuppressWarnings("unchecked")
            IRelation3<Object, ?, ?> rel = (IRelation3<Object, ?, ?>) object;
            StringBuilder sb = new StringBuilder("{");
            for (Object key : rel.keySet()) {
                int len = sb.length();
                for (Entry<?, ?> obj : rel.get(key)) {
                    sb.append('(');
                    sb.append(prettyPrint(key, unifier));
                    sb.append(", ");
                    sb.append(prettyPrint(obj.getKey(), unifier));
                    sb.append(")=");
                    sb.append(prettyPrint(obj.getValue(), unifier));
                    sb.append(", ");
                }
                if (sb.length() == len) sb.append(", ");
            }
            if (sb.length() > 2) sb.setLength(sb.length() - 2);
            sb.append('}');
            return sb.toString();
        }
        
        return object.toString();
    }
    
    /**
     * Pretty prints the given object using the given unifier within the given context.
     */
    public static String prettyPrint(Object object, IUnifier unifier, @Nullable Context context) {
        if (context == null) return prettyPrint(object, unifier);
        return Context.executeInContext(context, () -> prettyPrint(object, unifier));
    }
    
    /**
     * Prints the given iterable by applying the given function to each element.
     * 
     * @param iterable
     *      the iterable
     * @param function
     *      the function
     * 
     * @return
     *      the string [a, b, ..., n]
     */
    public static <T> String print(Iterable<T> iterable, Function<T, String> function) {
        StringBuilder sb = new StringBuilder("[");
        for (T t : iterable) {
            sb.append(function.apply(t));
            sb.append(", ");
        }
        if (sb.length() > 1) sb.setLength(sb.length() - 2);
        sb.append(']');
        return sb.toString();
    }
    
    // --------------------------------------------------------------------------------------------
    // Matchers
    // --------------------------------------------------------------------------------------------
    
    /**
     * @param unifier
     *      the unifier to use for variables
     * 
     * @return
     *      a matcher which converts any statix term to a string
     */
    public static IMatcher<String> term(IUnifier unifier) {
        return term(unifier, false);
    }
    
    /**
     * @param unifier
     *      the unifier to use for variables
     * @param fancyScopes
     *      if true, scopes are printed fancily
     * 
     * @return
     *      a matcher which converts any statix term to a string
     */
    public static IMatcher<String> term(IUnifier unifier, boolean fancyScopes) {
        //@formatter:off
        return M.cases(
                M.stringValue(),
                LABEL,
                fancyScopes ? SCOPE_FANCY : SCOPE,
                occurrence(unifier),
                list(unifier),
                tuple(unifier),
                appl(unifier),
                VAR,
                M.term(ITerm::toString) //Fallback for any other term
        );
        //@formatter:on
    }
    
    public static IMatcher<String> occurrence(IUnifier unifier) {
        //This has a recursive definition
        return M.appl3(StatixTerms.OCCURRENCE_OP, M.stringValue(), M.listElems(), M.term(),
                (t, ns, name, ast) -> ns + "{" + printListCompact(name, unifier) + "}");
    }
    
    public static IMatcher<String> label() {
        return LABEL;
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
        return VAR;
    }
    
    public static IMatcher<String> scope() {
        return SCOPE;
    }
    
    public static IMatcher<String> scopeFancy() {
        return SCOPE_FANCY;
    }
    
    // --------------------------------------------------------------------------------------------
    // Helper methods
    // --------------------------------------------------------------------------------------------
    
    private static boolean moduleUniqueness() {
        return Context.context().getModuleManager().areModuleNamesUnique();
    }
    
    public static List<String> convertList(List<? extends ITerm> list, IUnifier unifier) {
        List<String> tbr = new ArrayList<>(list.size());
        for (ITerm term : list) {
            try {
                tbr.add(term(unifier).match(term, unifier).get());
            } catch (IllegalStateException ex) {
                System.err.println("Term is not ground: " + ex);
                tbr.add(term.toString());
            }
        }
        return tbr;
    }

    private static String formatList(List<String> l) {
        return "[" + String.join(", ", l) + "]";
    }
    
    // --------------------------------------------------------------------------------------------
    // Formatter
    // --------------------------------------------------------------------------------------------
    
    /**
     * Creates a term formatter which formats terms with the pretty printer.
     * 
     * @param unifier
     *      the unifier
     * 
     * @return
     *      the term formatter
     */
    public static TermFormatter formatter(IUnifier unifier) {
        final IUnifier uunifier = unifier.unrestricted();
        return t -> TPrettyPrinter.printTerm(t, uunifier);
    }
}
