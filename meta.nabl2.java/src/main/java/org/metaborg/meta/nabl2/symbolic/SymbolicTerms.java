package org.metaborg.meta.nabl2.symbolic;

import java.util.List;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.solver.ISymbolicConstraints;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.meta.nabl2.unification.IUnifier;

public class SymbolicTerms {

    public static ITerm build(ISymbolicConstraints symbolicConstraints, IUnifier unifier) {
        final List<ITerm> facts =
                symbolicConstraints.getFacts().stream().map(unifier::find).collect(Collectors.toList());
        final List<ITerm> goals =
                symbolicConstraints.getGoals().stream().map(unifier::find).collect(Collectors.toList());
        return TB.newAppl("SymbolicConstraints", TB.newList(facts), TB.newList(goals));
    }

}