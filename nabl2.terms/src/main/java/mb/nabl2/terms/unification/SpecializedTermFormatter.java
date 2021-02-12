package mb.nabl2.terms.unification;

import java.util.Optional;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.util.TermFormatter;

@FunctionalInterface
public interface SpecializedTermFormatter {

    /**
     * Return specialized formatting for the given term or nothing.
     * 
     * @param term
     *            Term to be formatted.
     * @param unifier
     *            Unifier that is driving the formatting. Can be used when matching the term with {@link TermMatch}. The
     *            formatter should be careful about the potential of recursive terms and use the sub term formatter
     *            where possible, instead of calling {@link IUnifier::toString(ITerm)} on this unifier.
     * @param subTermFormatter
     *            Function to use for formatting sub terms, which correctly handles recursive terms and specialized
     *            formatting of sub terms.
     */
    Optional<String> formatSpecialized(ITerm term, IUnifier unifier, TermFormatter subTermFormatter);

}