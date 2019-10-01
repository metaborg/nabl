package mb.nabl2.terms.unification;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;

import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;

public class UnificationPerformanceTest {

    private static final String X = "a";
    private static final String Y = "b";
    private static final String Z = "c";

    public static void main(String[] args) {
        testCycle();
        for(int n = 0; n <= 1000; n += 100) {
            System.out.println("Testing n = " + n);
            final long t0 = System.currentTimeMillis();
            System.out.println(testUnify(n));
            final long dt = System.currentTimeMillis() - t0;
            System.out.println("Finished in " + (dt / 1000.0) + "s");
        }
    }

    private static void testCycle() {
        System.out.println("Testing cycle");
        final IUnifier.Transient unifier = PersistentUnifier.Immutable.of(false).melt();
        ITermVar varA = B.newVar("", X);
        ITermVar varB = B.newVar("", Y);
        ITermVar varC = B.newVar("", Z);
        ITerm termB = B.newTuple(varB, varB);
        ITerm termC = B.newTuple(varC, varC);
        try {
            unifier.unify(varA, termB).orElseThrow(() -> new IllegalArgumentException());
            unifier.unify(varB, termC).orElseThrow(() -> new IllegalArgumentException());
            unifier.unify(varC, termB).orElseThrow(() -> new IllegalArgumentException());
            System.out.println(unifier);
        } catch(OccursException e) {
            System.out.println("Could not unify");
        }
        System.out.println("ground = " + unifier.isGround(termB));
        System.out.println("cyclic = " + unifier.isCyclic(termB));
        System.out.println("size = " + unifier.size(termB));
        System.out.println("vars = " + unifier.getVars(termB));
        System.out.println("equal = " + unifier.diff(termB, termC));
        System.out.println("string = " + unifier.toString(varA));
        System.out.println("string = " + unifier.toString(varB));
        System.out.println("string = " + unifier.toString(varC));
    }

    private static IUnifier testUnify(int n) {
        final IUnifier.Transient unifier = PersistentUnifier.Immutable.of().melt();
        final ITerm left = B.newTuple(
                Iterables.concat(createVars(X, n), createTuples(Y, n), Iterables2.singleton(createVar(X, n))));
        final ITerm right = B.newTuple(
                Iterables.concat(createTuples(X, n), createVars(Y, n), Iterables2.singleton(createVar(Y, n))));
        try {
            unifier.unify(left, right).orElseThrow(() -> new IllegalArgumentException());
        } catch(OccursException e) {
            System.err.println("Unification failed");
            e.printStackTrace(System.err);
        }
        System.out.println("ground = " + unifier.isGround(left));
        System.out.println("cyclic = " + unifier.isCyclic(left));
        System.out.println("size = " + unifier.size(left));
        System.out.println("vars = " + unifier.getVars(left));
        System.out.println("equal = " + unifier.diff(left, right));
        return unifier.freeze();
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
        return B.newVar("", name + "-" + i);
    }

    private static ITerm createTuple(String name, int i) {
        return B.newTuple(createVar(name, i - 1), createVar(name, i - 1));
    }

}