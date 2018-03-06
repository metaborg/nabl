package mb.nabl2.symbolic;

import org.metaborg.util.functions.Function1;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;

public interface ISymbolicConstraints {

    Set.Immutable<ITerm> getFacts();

    Set.Immutable<ITerm> getGoals();

    ISymbolicConstraints map(Function1<ITerm, ITerm> mapper);

}