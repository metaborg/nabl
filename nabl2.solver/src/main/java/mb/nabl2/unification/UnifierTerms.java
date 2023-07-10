package mb.nabl2.unification;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;

import org.metaborg.util.collection.ImList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.u.IUnifier;

public final class UnifierTerms {

    private final IUnifier unifier;

    private UnifierTerms(IUnifier unifier) {
        this.unifier = unifier;
    }

    private ITerm build() {
        final List<ITerm> entries =
                unifier.domainSet().stream().map(this::buildVar).collect(ImList.Immutable.toImmutableList());
        return B.newAppl("Unifier", (ITerm) B.newList(entries));
    }

    private ITerm buildVar(ITermVar var) {
        return B.newTuple(var, unifier.findRecursive(var));
    }

    public static ITerm build(IUnifier unifier) {
        return new UnifierTerms(unifier).build();
    }

}