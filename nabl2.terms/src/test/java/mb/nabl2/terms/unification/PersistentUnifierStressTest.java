package mb.nabl2.terms.unification;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import org.junit.Test;
import org.metaborg.util.functions.Function0;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;

@SuppressWarnings("unused")
public class PersistentUnifierStressTest {

    private static final int N = 100;

    private final ITermVar a = B.newVar("", "a");
    private final ITermVar b = B.newVar("", "b");
    private final ITermVar c = B.newVar("", "c");
    private final ITermVar d = B.newVar("", "d");
    private final ITermVar e = B.newVar("", "e");
    private final List<ITermVar> vars = ImmutableList.of(a, b, c, d, e);

    private final String f = "f";
    private final String g = "g";
    private final String h = "h";
    private final String i = "i";
    private final String j = "j";

    private final ITerm x = B.newString("x");
    private final ITerm y = B.newString("y");
    private final ITerm z = B.newString("z");

    private IUnifier.Immutable makeRandomUnifier1() {
        // @formatter:off
        return makeRandomUnifier(ImmutableMultimap.<ITerm, ITerm>builder()
                .put(a, b)
                .put(c, b)
                .put(b, d)
                .build());
        // @formatter:on
    }

    private IUnifier.Immutable makeRandomUnifier2() {
        // @formatter:off
        return makeRandomUnifier(ImmutableMultimap.<ITerm, ITerm>builder()
                .put(a, b)
                .put(b, c)
                .put(b, B.newAppl(f, x, d))
                .put(d, B.newAppl(g, e))
                .build());
        // @formatter:on
    }

    private IUnifier.Immutable makeRandomUnifier3() {
        // @formatter:off
        return makeRandomUnifier(ImmutableMultimap.<ITerm, ITerm>builder()
                .put(a, b)
                .put(c, d)
                .put(b, B.newAppl(f, x, d))
                .put(e, B.newAppl(g, a))
                .build());
        // @formatter:on
    }

    private IUnifier.Immutable makeRandomUnifier(ImmutableMultimap<ITerm, ITerm> init) {
        final List<Entry<ITerm, ITerm>> equalities = Lists.newArrayList(init.entries());
        final Random rnd = new Random(System.currentTimeMillis());
        try {
            IUnifier.Transient unifier = PersistentUnifier.Immutable.of().melt();
            Collections.shuffle(equalities);
            for(Entry<ITerm, ITerm> equality : equalities) {
                final ITerm left;
                final ITerm right;
                if(rnd.nextBoolean()) {
                    left = equality.getKey();
                    right = equality.getValue();
                } else {
                    left = equality.getValue();
                    right = equality.getKey();
                }
                unifier.unify(equality.getKey(), equality.getValue()).orElseThrow(() -> new IllegalArgumentException());
            }
            return unifier.freeze();
        } catch(OccursException e) {
            throw new IllegalStateException("Inconsistent equalities list.", e);
        }
    }

    @Test public void testEquals1() {
        testEquals(this::makeRandomUnifier1);
    }

    @Test public void testRemove1() {
        testRemove(this::makeRandomUnifier1);
    }

    @Test public void testEquals2() {
        testEquals(this::makeRandomUnifier2);
    }

    @Test public void testRemove2() {
        testRemove(this::makeRandomUnifier2);
    }

    @Test public void testEquals3() {
        testEquals(this::makeRandomUnifier3);
    }

    @Test public void testRemove3() {
        testRemove(this::makeRandomUnifier3);
    }

    private void testEquals(Function0<IUnifier.Immutable> init) {
        for(int i = 0; i < N; i++) {
            final IUnifier.Immutable phi = init.apply();
            final IUnifier.Immutable theta = init.apply();
            assertSame(phi, theta);
        }
    }

    private void testRemove(Function0<IUnifier.Immutable> init) {
        for(ITermVar v : vars) {
            for(int i = 0; i < N; i++) {
                final IUnifier.Immutable phi = init.apply();
                final IUnifier.Immutable theta = init.apply();
                assertSame(phi.remove(v).unifier(), theta.remove(v).unifier());
            }
        }
    }

    private void assertSame(IUnifier phi, IUnifier theta) {
        boolean phiEqualsTheta = phi.equals(theta);
        boolean thetaEqualsPhi = theta.equals(phi);
        if(!phiEqualsTheta || !thetaEqualsPhi) {
            throw new AssertionError();
        }
    }

}