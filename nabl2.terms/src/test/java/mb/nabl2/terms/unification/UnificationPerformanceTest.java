package mb.nabl2.terms.unification;

import java.util.List;

import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.build.TermBuild;

public class UnificationPerformanceTest {

    private static final String A = "a";
    private static final String B = "b";
    private static final String C = "c";

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
        final IUnifier.Transient unifier = PersistentUnifier.Transient.of();
        ITermVar varA = TermBuild.B.newVar("", A);
        ITermVar varB = TermBuild.B.newVar("", B);
        ITermVar varC = TermBuild.B.newVar("", C);
        ITerm termB = TermBuild.B.newTuple(varB, varB);
        ITerm termC = TermBuild.B.newTuple(varC, varC);
        try {
            unifier.unify(varA, termB);
            unifier.unify(varB, termC);
            unifier.unify(varC, termB);
            System.out.println(unifier);
        } catch(UnificationException e) {
            System.out.println("Could not unify");
        }
        System.out.println("ground = " + unifier.isGround(termB));
        System.out.println("cyclic = " + unifier.isCyclic(termB));
        System.out.println("size = " + unifier.size(termB));
        System.out.println("vars = " + unifier.getVars(termB));
        System.out.println("equal = " + unifier.areEqual(termB, termC));
        System.out.println("unequal = " + unifier.areUnequal(termB, termC));
    }

    private static IUnifier testUnify(int n) {
        final IUnifier.Transient unifier = PersistentUnifier.Transient.of();
        final ITerm left = TermBuild.B.newTuple(
                Iterables.concat(createVars(A, n), createTuples(B, n), Iterables2.singleton(createVar(A, n))));
        final ITerm right = TermBuild.B.newTuple(
                Iterables.concat(createTuples(A, n), createVars(B, n), Iterables2.singleton(createVar(B, n))));
        try {
            unifier.unify(left, right);
        } catch(UnificationException e) {
            System.err.println("Unification failed");
            e.printStackTrace(System.err);
        }
        System.out.println("ground = " + unifier.isGround(left));
        System.out.println("cyclic = " + unifier.isCyclic(left));
        System.out.println("size = " + unifier.size(left));
        System.out.println("vars = " + unifier.getVars(left));
        System.out.println("equal = " + unifier.areEqual(left, right));
        System.out.println("unequal = " + unifier.areUnequal(left, right));
        return unifier;
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
        return TermBuild.B.newVar("", name + "-" + i);
    }

    private static ITerm createTuple(String name, int i) {
        return TermBuild.B.newTuple(createVar(name, i - 1), createVar(name, i - 1));
    }

}