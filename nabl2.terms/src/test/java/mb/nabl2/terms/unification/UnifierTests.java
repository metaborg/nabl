package mb.nabl2.terms.unification;

import static org.junit.Assert.assertTrue;

import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.terms.unification.ud.IUniDisunifier;

public class UnifierTests {

    public static void assertSame(IUnifier.Immutable phi, IUnifier.Immutable theta) {
        try {
            assertTrue(phi.unify(theta).map(r -> r.result().isEmpty()).orElse(false));
        } catch(OccursException e) {
            throw new AssertionError();
        }
    }

    public static void assertSame(IUniDisunifier.Immutable phi, IUniDisunifier.Immutable theta) {
        try {
            assertTrue(phi.unify(theta).map(r -> r.result().isEmpty()).orElse(false));
        } catch(OccursException e) {
            throw new AssertionError();
        }
        // FIXME Check disequality entailment
    }

}