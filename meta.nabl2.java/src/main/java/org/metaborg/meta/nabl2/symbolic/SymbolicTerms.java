package org.metaborg.meta.nabl2.symbolic;

import static org.metaborg.meta.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.unification.IUnifier;

public class SymbolicTerms {

    public static ITerm build(ISymbolicConstraints symbolicConstraints, IUnifier unifier) {
        final List<ITerm> facts =
                symbolicConstraints.getFacts().stream().map(unifier::findRecursive).collect(Collectors.toList());
        final List<ITerm> goals =
                symbolicConstraints.getGoals().stream().map(unifier::findRecursive).collect(Collectors.toList());
        return B.newAppl("SymbolicConstraints", B.newList(facts), B.newList(goals));
    }

}