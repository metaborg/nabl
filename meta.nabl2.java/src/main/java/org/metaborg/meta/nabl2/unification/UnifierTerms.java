package org.metaborg.meta.nabl2.unification;

import java.util.List;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.generic.TB;

public final class UnifierTerms {

    private final IUnifier unifier;

    private UnifierTerms(IUnifier unifier) {
        this.unifier = unifier;
    }

    private ITerm build() {
        List<ITerm> entries = unifier.getAllVars().stream().map(this::buildVar).collect(Collectors.toList());
        return TB.newAppl("Unifier", (ITerm) TB.newList(entries));
    }

    private ITerm buildVar(ITermVar var) {
        return TB.newTuple(var, unifier.find(var));
    }

    public static ITerm build(IUnifier unifier) {
        return new UnifierTerms(unifier).build();
    }

}