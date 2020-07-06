package mb.statix.arithmetic;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.io.Serializable;
import java.util.Arrays;

import org.metaborg.util.functions.Function2;

import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.statix.spoofax.StatixTerms;

public class ArithTerms {

    public static IMatcher<ArithTest> matchTest() {
        // @formatter:off
        return M.<ArithTest>cases(
            M.appl0("Equal", (t) -> {
                return new ArithTest("=", (i1, i2) -> i1 == i2, true);
            }),
            M.appl0("NotEqual", (t) -> {
                return new ArithTest("\\=", (i1, i2) -> i1 != i2, false);
            }),
            M.appl0("GreaterThanEqual", (t) -> {
                return new ArithTest(">=", (i1, i2) -> i1 >= i2, false);
            }),
            M.appl0("LessThanEqual", (t) -> {
                return new ArithTest("=<", (i1, i2) -> i1 <= i2, false);
            }),
            M.appl0("GreaterThan", (t) -> {
                return new ArithTest(">", (i1, i2) -> i1 > i2, false);
            }),
            M.appl0("LessThan", (t) -> {
                return new ArithTest("<", (i1, i2) -> i1 < i2, false);
            })
        );
        // @formatter:on
    }

    public static IMatcher<ArithExpr> matchExpr() {
        // @formatter:off
        return M.<ArithExpr>casesFix(m -> Arrays.asList(
            StatixTerms.varTerm().map(TermExpr::new),
            StatixTerms.intTerm().map(TermExpr::new),
            M.appl2("Add", m, m, (t, ae1, ae2) -> {
                return new BinExpr("+", ae1, ae2, new Add());
            }),
            M.appl2("Mul", m, m, (t, ae1, ae2) -> {
                return new BinExpr("*", ae1, ae2, new Mul());
            }),
            M.appl2("Sub", m, m, (t, ae1, ae2) -> {
                return new BinExpr("-", ae1, ae2, new Sub());
            }),
            M.appl2("Min", m, m, (t, ae1, ae2) -> {
                return new BinExpr("min", ae1, ae2, new Min());
            }),
            M.appl2("Max", m, m, (t, ae1, ae2) -> {
                return new BinExpr("max", ae1, ae2, new Max());
            }),
            M.appl2("Mod", m, m, (t, ae1, ae2) -> {
                return new BinExpr("mod", ae1, ae2, new Mod());
            }),
            M.appl2("Div", m, m, (t, ae1, ae2) -> {
                return new BinExpr("div", ae1, ae2, new Div());
            })
        ));
        // @formatter:on
    }

    private static class Add implements Function2<Integer, Integer, Integer>, Serializable {

        private static final long serialVersionUID = 1L;

        @Override public Integer apply(Integer i1, Integer i2) {
            return i1 + i2;
        }

    }

    private static class Mul implements Function2<Integer, Integer, Integer>, Serializable {

        private static final long serialVersionUID = 1L;

        @Override public Integer apply(Integer i1, Integer i2) {
            return i1 * i2;
        }

    }

    private static class Sub implements Function2<Integer, Integer, Integer>, Serializable {

        private static final long serialVersionUID = 1L;

        @Override public Integer apply(Integer i1, Integer i2) {
            return i1 - i2;
        }

    }

    private static class Min implements Function2<Integer, Integer, Integer>, Serializable {

        private static final long serialVersionUID = 1L;

        @Override public Integer apply(Integer i1, Integer i2) {
            return Math.min(i1, i2);
        }

    }

    private static class Max implements Function2<Integer, Integer, Integer>, Serializable {

        private static final long serialVersionUID = 1L;

        @Override public Integer apply(Integer i1, Integer i2) {
            return Math.max(i1, i2);
        }

    }

    private static class Mod implements Function2<Integer, Integer, Integer>, Serializable {

        private static final long serialVersionUID = 1L;

        @Override public Integer apply(Integer i1, Integer i2) {
            return Math.floorMod(i1, i2);
        }

    }

    private static class Div implements Function2<Integer, Integer, Integer>, Serializable {

        private static final long serialVersionUID = 1L;

        @Override public Integer apply(Integer i1, Integer i2) {
            return Math.floorDiv(i1, i2);
        }

    }

}