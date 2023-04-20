package mb.statix.solver;

import java.util.List;

import org.metaborg.util.collection.ImList;
import org.metaborg.util.functions.Action1;
import org.metaborg.util.unit.Unit;

import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.terms.unification.ud.Diseq;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.CInequal;

public class StateUtil {

    public static List<IConstraint> asConstraint(IUniDisunifier unifier) {
        final ImList.Mutable<IConstraint> constraints = ImList.Mutable.of();
        buildEqualities(unifier, constraints::add);
        buildInequalities(unifier.disequalities(), constraints::add);
        return constraints.freeze();
    }

    public static List<CEqual> asEqualities(IUnifier unifier) {
        final ImList.Mutable<CEqual> constraints = ImList.Mutable.of();
        buildEqualities(unifier, constraints::add);
        return constraints.freeze();
    }

    public static List<CInequal> asInequalities(IUniDisunifier unifier) {
        final ImList.Mutable<CInequal> constraints = ImList.Mutable.of();
        buildInequalities(unifier.disequalities(), constraints::add);
        return constraints.freeze();
    }

    private static void buildEqualities(IUnifier unifier, Action1<CEqual> add) {
        for(ITermVar var : unifier.domainSet()) {
            add.apply(new CEqual(var, unifier.findTerm(var)));
        }
    }

    private static void buildInequalities(Iterable<Diseq> diseqs, Action1<CInequal> add) {
        diseqs.forEach(diseq -> {
            diseq.toTuple().apply((us, left, right) -> {
                add.apply(new CInequal(us, left, right));
                return Unit.unit;
            });
        });
    }

}