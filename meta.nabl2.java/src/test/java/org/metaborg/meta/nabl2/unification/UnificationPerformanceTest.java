package org.metaborg.meta.nabl2.unification;

import java.util.List;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import io.usethesource.capsule.Map;

public class UnificationPerformanceTest {

    private static final String A = "a";
    private static final String B = "b";

    public static void main(String[] args) {
        for(int n = 0; n < 1000; n += 100) {
            System.out.println("Testing n = " + n);
            final long t0 = System.currentTimeMillis();
            System.out.println(testUnify(n));
            final long dt = System.currentTimeMillis() - t0;
            System.out.println("Finished in " + (dt / 1000.0) + "s");
        }
        testCycle();
    }

    private static void testCycle() {
        System.out.println("Testing cycle");
        final FastUnifier unifier = new FastUnifier();
        ITermVar varA = TB.newVar("", A);
        ITermVar varB = TB.newVar("", B);
        ITerm termA = TB.newAppl(A, varA);
        ITerm termB = TB.newAppl(B, varB);
        try {
            unifier.unify(varA, termB);
            unifier.unify(varB, termA);
            System.out.println(unifier.sub());
        } catch(UnificationException e) {
            System.out.println("Could not unify");
        }
    }
    
    private static Map.Immutable<ITermVar, ITerm> testUnify(int n) {
        final FastUnifier unifier = new FastUnifier();
        final ITerm left = TB.newTuple(
                Iterables.concat(createVars(A, n), createTuples(B, n), Iterables2.singleton(createVar(A, n))));
        final ITerm right = TB.newTuple(
                Iterables.concat(createTuples(A, n), createVars(B, n), Iterables2.singleton(createVar(B, n))));
        try {
            unifier.unify(left, right);
        } catch(UnificationException e) {
            System.err.println("Unification failed");
            e.printStackTrace(System.err);
        }
        return unifier.sub();
    }

    private static List<ITerm> createVars(String name, int n) {
        List<ITerm> vars = Lists.newArrayListWithExpectedSize(n);
        for(int i = 1; i <= n; i++) {
            vars.add(createVar(name, i));
        }
        return vars;
    }

    private static List<ITerm> createTuples(String name, int n) {
        List<ITerm> tups = Lists.newArrayListWithExpectedSize(n);
        for(int i = 1; i <= n; i++) {
            tups.add(createTuple(name, i));
        }
        return tups;
    }

    private static ITerm createVar(String name, int i) {
        return TB.newVar("", name + "-" + i);
    }

    private static ITerm createTuple(String name, int i) {
        return TB.newTuple(createVar(name, i - 1), createVar(name, i - 1));
    }


}