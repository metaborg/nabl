package mb.nabl2.terms.stratego;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;

import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.util.TermUtils;

import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.IAttachments;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.build.TermBuild;

/**
 * Functions for working with Stratego placeholders.
 */
public final class StrategoPlaceholders {

    private static final String PLACEHOLDER_SUFFIX = "-Plhdr";

    // Prevent instantiation.
    private StrategoPlaceholders() {}

    /**
     * Determines whether the term is just a variable (or injection of a variable) whose sort is a literal.
     *
     * @param term the term to check
     * @return {@code true} when the term is (the injection of) a variable whose sort is a literal;
     * otherwise, {@code false}
     */
    public static boolean isLiteralVar(ITerm term) {
        return term.match(Terms.<Boolean>casesFix(
            (m, appl) ->  {
                if (!isInjectionConstructor(appl)) return false;
                // Injection
                return isLiteralVar(appl.getArgs().get(0));
            },
            (m, list) -> list.match(ListTerms.<Boolean>casesFix(
                (lm, cons) -> false,
                (lm, nil) -> false,
                (lm, var) -> false
            )),
            (m, string) -> false,
            (m, integer) -> false,
            (m, blob) -> false,
            (m, var) -> isLiteralSort(getSortFromAttachments(var.getAttachments()))
        ));
    }

    /**
     * Determines whether the term contains a variable whose sort is a literal.
     *
     * @param term the term to check
     * @return {@code true} when the term contains a variable whose sort is a literal;
     * otherwise, {@code false}
     */
    public static boolean containsLiteralVar(ITerm term) {
        return term.match(Terms.<Boolean>casesFix(
            (m, appl) ->  {
                if (isPlaceholder(appl)) {
                    // Placeholder
                    return isLiteralSort(getSortFromAttachments(appl.getAttachments()));
                } else {
                    return appl.getArgs().stream().anyMatch(a -> a.match(m));
                }
            },
            (m, list) -> list.match(ListTerms.<Boolean>casesFix(
                (lm, cons) -> cons.getHead().match(m) || cons.getTail().match(lm),
                (lm, nil) -> false,
                (lm, var) -> isLiteralSort(getSortFromAttachments(var.getAttachments()))
            )),
            (m, string) -> false,
            (m, integer) -> false,
            (m, blob) -> false,
            (m, var) -> isLiteralSort(getSortFromAttachments(var.getAttachments()))
        ));
    }

    /**
     * Gets the sort of the term from its attachments.
     *
     * @param attachments the attachments of the term
     * @return the sort; or {@code null} when not found
     */
    private static @Nullable IStrategoTerm getSortFromAttachments(IAttachments attachments) {
        @Nullable StrategoAnnotations annotations = attachments.get(StrategoAnnotations.class);
        if (annotations == null) return null;
        return getSortFromAnnotations(annotations.getAnnotationList());
    }

    /**
     * Gets the sort of the term from its annotations.
     *
     * This code looks for an annotation of the form {@code OfSort(_)}.
     *
     * @param annotations the annotations of the term
     * @return the sort; or {@code null} when not found
     */
    private static @Nullable IStrategoTerm getSortFromAnnotations(List<IStrategoTerm> annotations) {
        for(IStrategoTerm term : annotations) {
            if (!TermUtils.isAppl(term, "OfSort", 1)) continue; // OfSort(_)
            return term.getSubterm(0);
        }
        // Not found.
        return null;
    }

    /**
     * Determines whether the sort is a literal.
     *
     * The sort term is the term that's contained in the {@code OfSort(_)} annotation.
     *
     * @param term the sort term; or {@code null}
     * @return {@code true} when it is a literal; otherwise, {@code false}
     */
    private static boolean isLiteralSort(@Nullable IStrategoTerm term) {
        if (term == null) return false;
        if (!TermUtils.isAppl(term)) throw new UnsupportedOperationException("Unknown sort: " + term);
        IStrategoAppl appl = TermUtils.toAppl(term);
        switch (appl.getConstructor().getName()) {
            case "STRING":
                return true;
            case "SORT":
            case "LIST":
            case "SCOPE":
            case "OCCURRENCE":
            case "PATH":
                return false;
            default:
                throw new UnsupportedOperationException("Unknown sort: " + term);
        }
    }

    /**
     * Determines whether the given term is a placeholder term.
     *
     * @param term the term to check
     * @return {@code true} when the term is a placeholder term; otherwise, {@code false}
     */
    public static boolean isPlaceholder(IStrategoAppl term) {
        IStrategoConstructor constructor = term.getConstructor();
        return constructor.getName().endsWith(PLACEHOLDER_SUFFIX) && constructor.getArity() == 0;
    }

    /**
     * Determines whether the given term is a placeholder term.
     *
     * @param term the term to check
     * @return {@code true} when the term is a placeholder term; otherwise, {@code false}
     */
    public static boolean isPlaceholder(IStrategoTerm term) {
        return term instanceof IStrategoAppl && isPlaceholder((IStrategoAppl)term);
    }

    /**
     * Determines whether the given term is a placeholder term.
     *
     * @param term the term to check
     * @return {@code true} when the term is a placeholder term; otherwise, {@code false}
     */
    public static boolean isPlaceholder(IApplTerm term) {
        return term.getOp().endsWith(PLACEHOLDER_SUFFIX) && term.getArity() == 0;
    }

    /**
     * Determines whether the given term is a placeholder term.
     *
     * @param term the term to check
     * @return {@code true} when the term is a placeholder term; otherwise, {@code false}
     */
    public static boolean isPlaceholder(ITerm term) {
        return term instanceof IApplTerm && isPlaceholder((IApplTerm)term);
    }

    /**
     * Replaces placeholders by term variables.
     *
     * @param term the term in which to replace the placeholders
     * @param placeholderVarMap the map to which mappings from placeholders to term variables are added
     * @return the term with its placeholders replaced
     */
    public static ITerm replacePlaceholdersByVariables(ITerm term, PlaceholderVarMap placeholderVarMap) {
        return term.match(Terms.<ITerm>casesFix(
            (m, appl) ->  {
                if (isPlaceholder(appl)) {
                    // Placeholder
                    return placeholderVarMap.addPlaceholderMapping(appl);
                } else {
                    return TermBuild.B.newAppl(appl.getOp(), appl.getArgs().stream().map(a -> a.match(m)).collect(Collectors.toList()), appl.getAttachments());
                }
            },
            (m, list) -> list.match(ListTerms.<IListTerm>casesFix(
                (lm, cons) -> TermBuild.B.newCons(cons.getHead().match(m), cons.getTail().match(lm), cons.getAttachments()),
                (lm, nil) -> nil,
                (lm, var) -> var
            )),
            (m, string) -> string,
            (m, integer) -> integer,
            (m, blob) -> blob,
            (m, var) -> var
        ));
    }

    /**
     * Replaces term variables by placeholders.
     *
     * @param term the term in which to replace the term variables
     * @param placeholderVarMap the map from which mappings from placeholders to term variables are taken
     * @return the term with its term variables replaced
     */
    public static ITerm replaceVariablesByPlaceholders(ITerm term, PlaceholderVarMap placeholderVarMap) {
        return term.match(Terms.cases(
            appl -> {
                if (isInjectionConstructor(appl) && onlyInjectionConstructorsAndVariables(appl)) {
                    return getPlaceholderForTerm(appl);
                } else {
                    return TermBuild.B.newAppl(appl.getOp(), appl.getArgs().stream().map(a -> replaceVariablesByPlaceholders(a, placeholderVarMap)).collect(Collectors.toList()), appl.getAttachments());
                }
            },
            list -> replaceVariablesByPlaceholdersInList(list, placeholderVarMap),
            string -> string,
            integer -> integer,
            blob -> blob,
            // TODO: Ability to relate placeholders, such that typing in the editor in one placeholder also types in another
            var -> getPlaceholderForVar(var, placeholderVarMap)
        ));
    }

    public static IListTerm replaceVariablesByPlaceholdersInList(IListTerm term, PlaceholderVarMap placeholderVarMap) {
        return term.match(ListTerms.cases(
            cons -> TermBuild.B.newCons(replaceVariablesByPlaceholders(cons.getHead(), placeholderVarMap), replaceVariablesByPlaceholdersInList(cons.getTail(), placeholderVarMap), cons.getAttachments()),
            nil -> nil,
            var -> TermBuild.B.newCons(replaceVariablesByPlaceholders(var, placeholderVarMap), TermBuild.B.newNil())
            //var -> TermBuild.B.newNil()// var    // FIXME: Should be make a placeholder for list tails?
        ));
    }

    /**
     * Replaces term variables in list positions by empty lists.
     *
     * This is because they are not supported in Stratego, and no conversion is possible.
     *
     * @param term the term in which to replace the term variables
     * @return the term with its term variables replaced
     */
    public static ITerm replaceListVariablesByEmptyList(ITerm term) {
        return term.match(Terms.cases(
            appl -> TermBuild.B.newAppl(appl.getOp(), appl.getArgs().stream().map(StrategoPlaceholders::replaceListVariablesByEmptyList).collect(Collectors.toList()), appl.getAttachments()),
            list -> replaceListVariablesByEmptyListInList(list),
            string -> string,
            integer -> integer,
            blob -> blob,
            var -> var
        ));
    }

    public static IListTerm replaceListVariablesByEmptyListInList(IListTerm term) {
        return term.match(ListTerms.cases(
            cons -> TermBuild.B.newCons(replaceListVariablesByEmptyList(cons.getHead()), replaceListVariablesByEmptyListInList(cons.getTail()), cons.getAttachments()),
            nil -> nil,
            var -> TermBuild.B.newNil()
        ));
    }

    public static boolean isInjectionConstructor(ITerm term) {
        return term instanceof IApplTerm && isInjectionConstructor((IApplTerm)term);
    }

    public static boolean isInjectionConstructor(IApplTerm appl) {
        // TODO: This heuristic should be a Stratego strategy generated by sdf3.ext.statix
        return appl.getOp().contains("2")
            && appl.getArgs().size() == 1
            && appl.getArgs().get(0) instanceof ITermVar;
    }

    /**
     * Returns the name of the sort being injected.
     *
     * It is an error to call this method on a term that is not an injection constructor.
     * Use {@link #isInjectionConstructor} to check for this.
     *
     * @param appl the injection constructor term
     * @return the name of the sort being injected
     */
    public static String getInjectionSortName(IApplTerm appl) {
        assert isInjectionConstructor(appl);
        // TODO: This heuristic should be a Stratego strategy generated by sdf3.ext.statix
        final String op = appl.getOp();
        return op.substring(0, op.indexOf('2'));
    }

    /**
     * Returns the argument of the injected sort.
     *
     * It is an error to call this method on a term that is not an injection constructor.
     * Use {@link #isInjectionConstructor} to check for this.
     *
     * @param appl the injection constructor term
     * @return the argument of the constructor
     */
    public static ITermVar getInjectionArgument(IApplTerm appl) {
        assert isInjectionConstructor(appl);
        // TODO: This heuristic should be a Stratego strategy generated by sdf3.ext.statix
        return (ITermVar)appl.getArgs().get(0);
    }

    public static boolean onlyInjectionConstructorsAndVariables(ITerm term) {
        return term.match(Terms.cases(
            appl -> {
                if (!isInjectionConstructor(appl)) {
                    return false;
                } else {
                    return appl.getArgs().stream().allMatch(StrategoPlaceholders::onlyInjectionConstructorsAndVariables);
                }
            },
            list -> false,
            string -> false,
            integer -> false,
            blob -> false,
            var -> true
        ));
    }

    /**
     * Counts the number of common injection constructors.
     *
     * @param terms the terms to test
     * @return the number of common injection constructors
     */
    public static int countCommonInjectionConstructors(List<ITerm> terms) {
        if (terms.isEmpty()) return 0;
        int level = 0;
        List<ITerm> currentTerms = new ArrayList<>(terms);
        outerLoop:
        while (true) {
            // Assert that they all start with the same constructor, and that this constructor is an injection constructor
            final ITerm candidateInjConstructor = currentTerms.get(0);
            @Nullable IApplTerm injConstructor = isInjectionConstructor(candidateInjConstructor) ? (IApplTerm)candidateInjConstructor : null;
            if (injConstructor == null) break;
            final List<ITerm> newTerms = new ArrayList<>(currentTerms.size());
            for(ITerm term : currentTerms) {
                if(!(term instanceof IApplTerm)) break outerLoop;
                final IApplTerm applTerm = (IApplTerm)term;
                if(!applTerm.getOp().equals(injConstructor.getOp()) || applTerm.getArity() != injConstructor.getArity()) break outerLoop;
                assert applTerm.getArity() == 1 : "We assume injector constructors have only one argument.";
                newTerms.add(applTerm.getArgs().get(0));
            }
            currentTerms = newTerms;
            level += 1;
        }
        return level;
    }

    /**
     * Gets the placeholder term for a given term variable.
     *
     * @param var the term variable
     * @param placeholderVarMap the map from which mappings from placeholders to term variables are taken
     * @return the placeholder term
     */
    private static ITerm getPlaceholderForVar(ITermVar var, PlaceholderVarMap placeholderVarMap) {
        @Nullable PlaceholderVarMap.Placeholder placeholder = placeholderVarMap.getPlaceholder(var);
        if (placeholder != null) return placeholder.getTerm();

        return getPlaceholderForTerm(var);
    }
    private static ITerm getPlaceholderForTerm(ITerm term) {
        @Nullable ITerm newPlaceholder = getPlaceholderFromAttachments(term.getAttachments());
        return newPlaceholder != null ? newPlaceholder : TermBuild.B.newAppl("??-Plhdr");
    }

    /**
     * Gets the sort of the term from its attachments.
     *
     * @param attachments the attachments of the term
     * @return the placeholder term; or {@code null} when not found
     */
    private static @Nullable ITerm getPlaceholderFromAttachments(IAttachments attachments) {
        @Nullable StrategoAnnotations annotations = attachments.get(StrategoAnnotations.class);
        if (annotations == null) return null;
        return getPlaceholderFromAnnotations(annotations.getAnnotationList());
    }

    /**
     * Gets the sort of the term from its annotations.
     *
     * This code looks for an annotation of the form {@code OfSort(_)}.
     *
     * @param annotations the annotations of the term
     * @return the placeholder term; or {@code null} when not found
     */
    private static @Nullable ITerm getPlaceholderFromAnnotations(List<IStrategoTerm> annotations) {
        for(IStrategoTerm term : annotations) {
            if (!TermUtils.isAppl(term, "OfSort", 1)) continue; // OfSort(_)
            return getPlaceholderFromSortTerm(term.getSubterm(0));
        }
        // Not found.
        return null;
    }

    /**
     * Gets the sort of the term from its sort term.
     *
     * The sort term is a term built by NaBL2 and stored in the {@code OfSort(_)} annotation.
     *
     * @param term the sort term
     * @return the placeholder term; or {@code null} when not found
     */
    private static @Nullable ITerm getPlaceholderFromSortTerm(IStrategoTerm term) {
        // TODO: Lots of things don't have sort names.
        // Perhaps we should follow the example of ?? and use "??" for all placeholders,
        // or parse any $name$ within dollars as a placeholder, so we can describe the placeholder (and perhaps relate them: "$x$ = $x$ + 1").
        if (TermUtils.isAppl(term, "SORT", 1)) {
            // SORT(_)
            return TermBuild.B.newAppl(TermUtils.toJavaStringAt(term, 0) + PLACEHOLDER_SUFFIX);
        } else if (TermUtils.isAppl(term, "LIST", 1)) {
            // LIST(_)
            // We make a singleton list with a placeholder. Similarly, we could opt to make an empty list.
            return TermBuild.B.newList(getPlaceholderFromSortTerm(term.getSubterm(0)));
        } else if (TermUtils.isAppl(term, "STRING", 0)) {
            // STRING()
            // TODO: Find the original sort and insert that
            //  Here we just insert $ID for any STRING, which is abviously incorrect
            String name = TermUtils.toAppl(term).getConstructor().getName();
            return TermBuild.B.newAppl("ID" + PLACEHOLDER_SUFFIX);
        } else if (TermUtils.isAppl(term, null, 0)) {
            // SCOPE()
            // STRING()
            // OCCURRENCE()
            // PATH()
            // TODO: What to do with these built-in sorts? We should never encounter SCOPE(), OCCURRENCE(), or PATH()
            String name = TermUtils.toAppl(term).getConstructor().getName();
            return TermBuild.B.newAppl("_" + Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase());
        } else {
            throw new UnsupportedOperationException("Unknown sort: " + term);
        }
    }
}
