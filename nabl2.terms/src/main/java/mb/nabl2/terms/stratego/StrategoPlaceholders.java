package mb.nabl2.terms.stratego;

import com.google.common.collect.ImmutableClassToInstanceMap;

import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.build.TermBuild;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.util.TermUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Functions for working with Stratego placeholders.
 */
public final class StrategoPlaceholders {

    // Prevent instantiation.
    private StrategoPlaceholders() {}

    /**
     * Determines whether the given term is a placeholder term.
     *
     * @param term the term to check
     * @return {@code true} when the term is a placeholder term; otherwise, {@code false}
     */
    public static boolean isPlaceholder(IStrategoAppl term) {
        IStrategoConstructor constructor = term.getConstructor();
        return constructor.getName().endsWith("-Plhdr") && constructor.getArity() == 0;
    }

    /**
     * Determines whether the given term is a placeholder term.
     *
     * @param term the term to check
     * @return {@code true} when the term is a placeholder term; otherwise, {@code false}
     */
    public static boolean isPlaceholder(IApplTerm term) {
        return term.getOp().endsWith("-Plhdr") && term.getArity() == 0;
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
        return term.match(Terms.<ITerm>casesFix(
            (m, appl) ->  TermBuild.B.newAppl(appl.getOp(), appl.getArgs().stream().map(a -> a.match(m)).collect(Collectors.toList()), appl.getAttachments()),
            (m, list) -> list.match(ListTerms.<IListTerm>casesFix(
                (lm, cons) -> TermBuild.B.newCons(cons.getHead().match(m), cons.getTail().match(lm), cons.getAttachments()),
                (lm, nil) -> nil,
                (lm, var) -> var
            )),
            (m, string) -> string,
            (m, integer) -> integer,
            (m, blob) -> blob,
            // TODO: Ability to relate placeholders, such that typing in the editor in one placeholder also types in another
            (m, var) -> getPlaceholderForVar(var, placeholderVarMap)
        ));
    }

    /**
     * Gets the placeholder term for a given term variable.
     *
     * @param var the term variable
     * @param placeholderVarMap the map from which mappings from placeholders to term variables are taken
     * @return the placeholder term
     */
    private static IApplTerm getPlaceholderForVar(ITermVar var, PlaceholderVarMap placeholderVarMap) {
        @Nullable IApplTerm placeholder = placeholderVarMap.getPlaceholder(var);
        if (placeholder != null) return placeholder;

        @Nullable String name = getSortFromAttachments(var.getAttachments());
        if (name == null) {
            // No name
            return TermBuild.B.newAppl("??");
        }
        return TermBuild.B.newAppl(name + "-Plhdr");
    }

    /**
     * Gets the sort of the term from its attachments.
     *
     * @param attachments the attachments of the term
     * @return the name of the sort; or {@code null} if not found
     */
    private static @Nullable String getSortFromAttachments(ImmutableClassToInstanceMap<Object> attachments) {
        @Nullable StrategoAnnotations annotations = (StrategoAnnotations)attachments.get(StrategoAnnotations.class);
        if (annotations == null) return null;
        return getSortNameFromAnnotations(annotations.getAnnotationList());
    }

    /**
     * Gets the sort of the term from its annotations.
     *
     * This code looks for an annotation of the form {@code OfSort(_)}.
     *
     * @param annotations the annotations of the term
     * @return the name of the sort; or {@code null} if not found
     */
    private static @Nullable String getSortNameFromAnnotations(List<IStrategoTerm> annotations) {
        for(IStrategoTerm term : annotations) {
            if (!TermUtils.isAppl(term, "OfSort", 1)) continue; // OfSort(_)
            return getSortNameFromSortTerm(term.getSubterm(0));
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
     * @return the sort name
     */
    private static @Nullable String getSortNameFromSortTerm(IStrategoTerm term) {
        // TODO: Lots of things don't have sort names.
        // Perhaps we should follow the example of ?? and use "??" for all placeholders,
        // or parse any $name$ within dollars as a placeholder, so we can describe the placeholder (and perhaps relate then: "$x$ = $x$ + 1").
        if (TermUtils.isAppl(term, "SORT", 1)) {
            // SORT(_)
            return TermUtils.toJavaStringAt(term, 0);
        } else if (TermUtils.isAppl(term, "LIST", 1)) {
            // LIST(_)
            return getSortNameFromSortTerm(term.getSubterm(0)) + "-List";
        } else if (TermUtils.isAppl(term, null, 0)) {
            // SCOPE()
            // STRING()
            String name = TermUtils.toAppl(term).getConstructor().getName();
            return "_" + Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase();
        } else {
            throw new UnsupportedOperationException("Unknown sort: " + term);
        }
    }
}
