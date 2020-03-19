package mb.nabl2.solver.solvers;

import java.util.Collection;
import java.util.Optional;

import org.metaborg.util.functions.PartialFunction2;

import mb.nabl2.terms.ITerm;

@FunctionalInterface
public interface CallExternal extends PartialFunction2<String, Collection<? extends ITerm>, ITerm> {
    static CallExternal never() {
        return (t1, t2) -> Optional.empty();
    }
}