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
        assertTrue(phi.disequalities().stream().noneMatch(
                diseq -> diseq.toTuple().apply(theta::disunify).map(r -> r.result().isPresent()).orElse(true)));
        assertTrue(theta.disequalities().stream().noneMatch(
                diseq -> diseq.toTuple().apply(phi::disunify).map(r -> r.result().isPresent()).orElse(true)));
    }

}