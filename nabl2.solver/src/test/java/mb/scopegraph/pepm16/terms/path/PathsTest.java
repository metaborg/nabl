package mb.scopegraph.pepm16.terms.path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.google.common.collect.Streams;

import mb.scopegraph.pepm16.IOccurrence;
import mb.scopegraph.pepm16.path.IScopePath;
import mb.scopegraph.pepm16.path.IStep;
import mb.scopegraph.pepm16.terms.Label;
import mb.scopegraph.pepm16.terms.Scope;

public class PathsTest {

    Label l = Label.P;

    Scope s1 = Scope.of("", "1");
    Scope s2 = Scope.of("", "2");
    Scope s3 = Scope.of("", "3");
    Scope s4 = Scope.of("", "4");

    IStep<Scope, Label, IOccurrence> st12 = Paths.direct(s1, l, s2);
    IStep<Scope, Label, IOccurrence> st21 = Paths.direct(s2, l, s1);
    IStep<Scope, Label, IOccurrence> st23 = Paths.direct(s2, l, s3);
    IStep<Scope, Label, IOccurrence> st34 = Paths.direct(s3, l, s4);

    IScopePath<Scope, Label, IOccurrence> id1 = Paths.empty(s1);
    IScopePath<Scope, Label, IOccurrence> id2 = Paths.empty(s2);
    IScopePath<Scope, Label, IOccurrence> id3 = Paths.empty(s3);
    IScopePath<Scope, Label, IOccurrence> id4 = Paths.empty(s4);

    @Test public void testAppendEmpty() {
        assertTrue(Paths.append(Paths.empty(s1), st12).isPresent());
        assertTrue(Paths.append(st12, Paths.empty(s2)).isPresent());
    }

    @Test public void testAppendSteps() {
        assertTrue(Paths.append(st12, st23).isPresent());
        assertTrue(Paths.append(st23, st34).isPresent());
        assertFalse(Paths.append(st12, st21).isPresent());
    }

    @Test public void testEquals() {
        IScopePath<Scope, Label, IOccurrence> p1 = Paths.append(Paths.append(st12, st23).get(), st34).get();
        IScopePath<Scope, Label, IOccurrence> p2 = Paths.append(st12, Paths.append(st23, st34).get()).get();
        assertEquals(p1.hashCode(), p2.hashCode());
        assertEquals(p1, p2);
    }

    @Test public void testEmpty() {
        IScopePath<Scope, Label, IOccurrence> p1 = Paths.append(Paths.empty(s1), st12).get();
        IScopePath<Scope, Label, IOccurrence> p2 = Paths.append(st12, Paths.empty(s2)).get();
        assertEquals(p1.hashCode(), p2.hashCode());
        assertEquals(p1, p2);
    }

    @Test public void testEmptyIterator() {
        IScopePath<Scope, Label, IOccurrence> path = id1;
        List<IStep<Scope, Label, IOccurrence>> steps = Streams.stream(path).collect(Collectors.toList());
        assertEquals(Arrays.asList(), steps);
    }

    @Test public void testSingleStepIterator() {
        IScopePath<Scope, Label, IOccurrence> path = st23;
        List<IStep<Scope, Label, IOccurrence>> steps = Streams.stream(path).collect(Collectors.toList());
        assertEquals(Arrays.asList(st23), steps);
    }

    @Test public void testSingleStepAndIdIterator() {
        IScopePath<Scope, Label, IOccurrence> path = Paths.append(st23, id3).get();
        List<IStep<Scope, Label, IOccurrence>> steps = Streams.stream(path).collect(Collectors.toList());
        assertEquals(Arrays.asList(st23), steps);
    }

    @Test public void testTwoStepsIterator() {
        IScopePath<Scope, Label, IOccurrence> path = Paths.append(st12, st23).get();
        List<IStep<Scope, Label, IOccurrence>> steps = Streams.stream(path).collect(Collectors.toList());
        assertEquals(Arrays.asList(st12, st23), steps);
    }

    @Test public void testStepIdStepIterator() {
        IScopePath<Scope, Label, IOccurrence> path = Paths.append(st12, Paths.append(id2, st23).get()).get();
        List<IStep<Scope, Label, IOccurrence>> steps = Streams.stream(path).collect(Collectors.toList());
        assertEquals(Arrays.asList(st12, st23), steps);
    }

}