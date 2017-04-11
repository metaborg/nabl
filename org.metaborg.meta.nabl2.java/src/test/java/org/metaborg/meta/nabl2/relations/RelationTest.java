package org.metaborg.meta.nabl2.relations;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.metaborg.meta.nabl2.relations.RelationDescription.Reflexivity;
import org.metaborg.meta.nabl2.relations.RelationDescription.Symmetry;
import org.metaborg.meta.nabl2.relations.RelationDescription.Transitivity;
import org.metaborg.meta.nabl2.relations.terms.Relation;

import com.google.common.collect.Iterables;

public class RelationTest {

    // reflexivity

    @Test public void testReflexive_AddRefl() throws RelationException {
        Relation<Integer> r = new Relation<>(ImmutableRelationDescription.of(Reflexivity.REFLEXIVE,
                Symmetry.NON_SYMMETRIC, Transitivity.NON_TRANSITIVE));
        r.add(1, 1);
        assertTrue(r.contains(1, 1));
    }

    @Test public void testReflexive_ContainsImplicitRefl() throws RelationException {
        Relation<Integer> r = new Relation<>(ImmutableRelationDescription.of(Reflexivity.REFLEXIVE,
                Symmetry.NON_SYMMETRIC, Transitivity.NON_TRANSITIVE));
        assertTrue(r.contains(1, 1));
    }

    @Test public void testReflexive_ImplicitLargerRefl() throws RelationException {
        Relation<Integer> r = new Relation<>(ImmutableRelationDescription.of(Reflexivity.REFLEXIVE,
                Symmetry.NON_SYMMETRIC, Transitivity.NON_TRANSITIVE));
        assertTrue(Iterables.contains(r.larger(1), 1));
    }

    @Test public void testReflexive_ImplicitSmallerRefl() throws RelationException {
        Relation<Integer> r = new Relation<>(ImmutableRelationDescription.of(Reflexivity.REFLEXIVE,
                Symmetry.NON_SYMMETRIC, Transitivity.NON_TRANSITIVE));
        assertTrue(Iterables.contains(r.smaller(1), 1));
    }

    @Test(expected = ReflexivityException.class) public void testIrreflexive_ReflThrows() throws RelationException {
        Relation<Integer> r = new Relation<>(ImmutableRelationDescription.of(Reflexivity.IRREFLEXIVE,
                Symmetry.NON_SYMMETRIC, Transitivity.NON_TRANSITIVE));
        r.add(1, 1);
    }

    @Test public void testIrreflexive_ReflFalse() throws RelationException {
        Relation<Integer> r = new Relation<>(ImmutableRelationDescription.of(Reflexivity.IRREFLEXIVE,
                Symmetry.NON_SYMMETRIC, Transitivity.NON_TRANSITIVE));
        assertFalse(r.contains(1, 1));
    }

    @Test public void testIrreflexive_ExtendNoRefl() throws RelationException {
        Relation<Integer> r = new Relation<>(ImmutableRelationDescription.of(Reflexivity.IRREFLEXIVE,
                Symmetry.NON_SYMMETRIC, Transitivity.TRANSITIVE));
        r.add(1, 2);
        r.add(2, 3);
        assertFalse(Iterables.contains(r.larger(2), 2));
        assertFalse(Iterables.contains(r.smaller(2), 2));
    }

    @Test public void testNonreflexive_Refl() throws RelationException {
        Relation<Integer> r = new Relation<>(ImmutableRelationDescription.of(Reflexivity.NON_REFLEXIVE,
                Symmetry.NON_SYMMETRIC, Transitivity.NON_TRANSITIVE));
        r.add(1, 1);
        assertTrue(r.contains(1, 1));
    }

    @Test public void testNonreflexive_NoImplicitRefl() throws RelationException {
        Relation<Integer> r = new Relation<>(ImmutableRelationDescription.of(Reflexivity.NON_REFLEXIVE,
                Symmetry.NON_SYMMETRIC, Transitivity.NON_TRANSITIVE));
        assertFalse(r.contains(1, 1));
    }

    @Test public void testNonreflexive_NonRefl() throws RelationException {
        Relation<Integer> r = new Relation<>(ImmutableRelationDescription.of(Reflexivity.NON_REFLEXIVE,
                Symmetry.NON_SYMMETRIC, Transitivity.NON_TRANSITIVE));
        r.add(1, 2);
        assertTrue(r.contains(1, 2));
    }

    // symmetry

    @Test public void testSymmetric_ExplicitSym() throws RelationException {
        Relation<Integer> r = new Relation<>(ImmutableRelationDescription.of(Reflexivity.NON_REFLEXIVE,
                Symmetry.SYMMETRIC, Transitivity.NON_TRANSITIVE));
        r.add(1, 2);
        r.add(2, 1);
        assertTrue(r.contains(1, 2));
        assertTrue(r.contains(2, 1));
    }

    @Test public void testSymmetric_ImplicitSym() throws RelationException {
        Relation<Integer> r = new Relation<>(ImmutableRelationDescription.of(Reflexivity.NON_REFLEXIVE,
                Symmetry.SYMMETRIC, Transitivity.NON_TRANSITIVE));
        r.add(1, 2);
        assertTrue(r.contains(1, 2));
        assertTrue(r.contains(2, 1));
    }

    @Test public void testAntisymmetric_Antisym() throws RelationException {
        Relation<Integer> r = new Relation<>(ImmutableRelationDescription.of(Reflexivity.NON_REFLEXIVE,
                Symmetry.ANTI_SYMMETRIC, Transitivity.NON_TRANSITIVE));
        r.add(1, 2);
        r.add(2, 3);
        assertTrue(r.contains(1, 2));
    }

    @Test(expected = SymmetryException.class) public void testAntisymmetric_Sym() throws RelationException {
        Relation<Integer> r = new Relation<>(ImmutableRelationDescription.of(Reflexivity.NON_REFLEXIVE,
                Symmetry.ANTI_SYMMETRIC, Transitivity.NON_TRANSITIVE));
        r.add(1, 2);
        r.add(2, 1);
    }

    @Test public void testNonsymmetric_Nonsym() throws RelationException {
        Relation<Integer> r = new Relation<>(ImmutableRelationDescription.of(Reflexivity.NON_REFLEXIVE,
                Symmetry.NON_SYMMETRIC, Transitivity.NON_TRANSITIVE));
        r.add(1, 2);
        assertTrue(r.contains(1, 2));
    }

    @Test public void testNonsymmetric_Sym() throws RelationException {
        Relation<Integer> r = new Relation<>(ImmutableRelationDescription.of(Reflexivity.NON_REFLEXIVE,
                Symmetry.NON_SYMMETRIC, Transitivity.NON_TRANSITIVE));
        r.add(1, 2);
        r.add(2, 1);
        assertTrue(r.contains(1, 2));
        assertTrue(r.contains(2, 1));
    }

    // transitiviy

    @Test public void testTransitive_Trans() throws RelationException {
        Relation<Integer> r = new Relation<>(ImmutableRelationDescription.of(Reflexivity.NON_REFLEXIVE,
                Symmetry.NON_SYMMETRIC, Transitivity.TRANSITIVE));
        r.add(1, 2);
        r.add(2, 3);
        assertTrue(r.contains(1, 3));
    }

    @Test public void testTransitive_TransReverse() throws RelationException {
        Relation<Integer> r = new Relation<>(ImmutableRelationDescription.of(Reflexivity.NON_REFLEXIVE,
                Symmetry.NON_SYMMETRIC, Transitivity.TRANSITIVE));
        r.add(2, 3);
        r.add(1, 2);
        assertTrue(r.contains(1, 3));
    }

    @Test(expected = TransitivityException.class) public void testTransitive_TransThrows() throws RelationException {
        Relation<Integer> r = new Relation<>(ImmutableRelationDescription.of(Reflexivity.NON_REFLEXIVE,
                Symmetry.NON_SYMMETRIC, Transitivity.ANTI_TRANSITIVE));
        r.add(1, 2);
        r.add(2, 3);
        r.add(1, 3);
    }

    @Test public void testTransitiveSymmetric_Trans() throws RelationException {
        Relation<Integer> r = new Relation<>(ImmutableRelationDescription.of(Reflexivity.NON_REFLEXIVE,
                Symmetry.SYMMETRIC, Transitivity.TRANSITIVE));
        r.add(1, 2);
        r.add(2, 3);
        r.add(3, 4);
        assertTrue(r.contains(4, 1));
    }

    @Test(expected = TransitivityException.class) public void testTransitiveSymmetric_TransThrows()
            throws RelationException {
        Relation<Integer> r = new Relation<>(ImmutableRelationDescription.of(Reflexivity.NON_REFLEXIVE,
                Symmetry.SYMMETRIC, Transitivity.ANTI_TRANSITIVE));
        r.add(1, 2);
        r.add(2, 3);
        r.add(3, 1);
    }

}