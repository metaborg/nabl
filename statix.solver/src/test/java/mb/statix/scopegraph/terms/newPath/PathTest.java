package mb.statix.scopegraph.terms.newPath;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.Lists;

import mb.statix.scopegraph.path.IStep;
import mb.statix.scopegraph.terms.Scope;

public class PathTest {

    private final Scope s1 = Scope.of("", "1");
    private final Scope s2 = Scope.of("", "2");
    private final Scope s3 = Scope.of("", "3");

    @Test public void testEmptyPath() {
        final ScopePath<Scope, Unit> p = new ScopePath<>(s1);
        assertEquals(0, p.size());
        assertEquals(s1, p.getSource());
        assertEquals(s1, p.getTarget());
        List<IStep<Scope, Unit>> steps = Lists.newArrayList(p);
        assertEquals(0, steps.size());
    }

    @Test public void testOneStepPath() {
        final ScopePath<Scope, Unit> p = new ScopePath<Scope, Unit>(s1).step(Unit.unit, s2).get();
        assertEquals(1, p.size());
        assertEquals(s1, p.getSource());
        assertEquals(s2, p.getTarget());
        List<IStep<Scope, Unit>> steps = Lists.newArrayList(p);
        assertEquals(1, steps.size());
        assertEquals(s1, steps.get(0).getSource());
        assertEquals(s2, steps.get(0).getTarget());
    }

    @Test public void testTwoStepPath() {
        final ScopePath<Scope, Unit> p =
                new ScopePath<Scope, Unit>(s1).step(Unit.unit, s2).get().step(Unit.unit, s3).get();
        assertEquals(2, p.size());
        assertEquals(s1, p.getSource());
        assertEquals(s3, p.getTarget());
        List<IStep<Scope, Unit>> steps = Lists.newArrayList(p);
        assertEquals(2, steps.size());
        assertEquals(s1, steps.get(0).getSource());
        assertEquals(s2, steps.get(0).getTarget());
        assertEquals(s2, steps.get(1).getSource());
        assertEquals(s3, steps.get(1).getTarget());
    }

    @Test public void testDirectCycle() {
        final Optional<ScopePath<Scope, Unit>> p = new ScopePath<Scope, Unit>(s1).step(Unit.unit, s1);
        assertFalse(p.isPresent());
    }

    @Test public void testIndirectCycle() {
        final Optional<ScopePath<Scope, Unit>> p =
                new ScopePath<Scope, Unit>(s1).step(Unit.unit, s2).get().step(Unit.unit, s1);
        assertFalse(p.isPresent());
    }

    @Test public void testPathEquality() {
        final ScopePath<Scope, Unit> p1 =
                new ScopePath<Scope, Unit>(s1).step(Unit.unit, s2).get().step(Unit.unit, s3).get();
        final ScopePath<Scope, Unit> p2 =
                new ScopePath<Scope, Unit>(s1).step(Unit.unit, s2).get().step(Unit.unit, s3).get();
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

}