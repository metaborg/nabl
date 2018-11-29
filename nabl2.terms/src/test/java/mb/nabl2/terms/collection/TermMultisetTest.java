package mb.nabl2.terms.collection;

import static mb.nabl2.terms.build.TermBuild.B;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.PersistentUnifier;

public class TermMultisetTest {

    private IUnifier.Transient unifier;
    private TermMultiset terms;

    @Before public void setUp() {
        this.unifier = PersistentUnifier.Transient.of();
        this.terms = new TermMultiset();

    }

    @Test public void testAdd1Ground() {
        final ITerm t1 = B.newInt(1);

        terms.add(t1, unifier);

        assertEquals(0, terms.varSet().size());
        assertEquals(1, terms.elementSet().size());
        assertEquals(1, terms.count(t1, unifier));
    }

    @Test public void testAdd2Seprately() {
        final ITerm t1 = B.newInt(1);

        terms.add(t1, unifier);
        terms.add(t1, unifier);

        assertEquals(0, terms.varSet().size());
        assertEquals(1, terms.elementSet().size());
        assertEquals(2, terms.count(t1, unifier));
    }

    @Test public void testAdd2AtOnce() {
        final ITerm t1 = B.newInt(1);

        terms.add(t1, 2, unifier);

        assertEquals(0, terms.varSet().size());
        assertEquals(1, terms.elementSet().size());
        assertEquals(2, terms.count(t1, unifier));
    }

    @Test public void testContainsOriginalAndUnifiedTerm() throws OccursException {
        final ITermVar v1 = B.newVar("", "v1");
        final ITerm t1 = B.newInt(1);
        final ITerm t2 = B.newTuple(v1);

        terms.add(t2, unifier);
        assertEquals(1, terms.varSet().size());

        IUnifier.Immutable result = unifier.unify(v1, t1).orElseThrow(() -> new IllegalStateException());
        terms.update(result.varSet(), unifier);

        assertTrue(terms.contains(t2, unifier));
        assertTrue(terms.contains(unifier.findTerm(t2), unifier));
        assertEquals(0, terms.varSet().size());
    }

}