package org.metaborg.meta.nabl2.scopegraph.terms.path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;
import org.metaborg.meta.nabl2.scopegraph.terms.ImmutableLabel;
import org.metaborg.meta.nabl2.scopegraph.terms.ImmutableScope;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;

public class PathsTest {

    Label l = ImmutableLabel.P;

    Scope s1 = ImmutableScope.of("", "1");
    Scope s2 = ImmutableScope.of("", "2");
    Scope s3 = ImmutableScope.of("", "3");
    Scope s4 = ImmutableScope.of("", "4");

    IScopePath<Scope, Label, IOccurrence> st12 = Paths.direct(s1, l, s2);
    IScopePath<Scope, Label, IOccurrence> st21 = Paths.direct(s2, l, s1);
    IScopePath<Scope, Label, IOccurrence> st23 = Paths.direct(s2, l, s3);
    IScopePath<Scope, Label, IOccurrence> st34 = Paths.direct(s3, l, s4);

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

}