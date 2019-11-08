package mb.statix.arithmetic;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Arrays;

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
                return new BinExpr("+", ae1, ae2, (i1, i2) -> i1 + i2);
            }),
            M.appl2("Mul", m, m, (t, ae1, ae2) -> {
                return new BinExpr("*", ae1, ae2, (i1, i2) -> i1 * i2);
            }),
            M.appl2("Sub", m, m, (t, ae1, ae2) -> {
                return new BinExpr("-", ae1, ae2, (i1, i2) -> i1 - i2);
            }),
            M.appl2("Min", m, m, (t, ae1, ae2) -> {
                return new BinExpr("min", ae1, ae2, (i1, i2) -> Math.min(i1, i2));
            }),
            M.appl2("Max", m, m, (t, ae1, ae2) -> {
                return new BinExpr("max", ae1, ae2, (i1, i2) -> Math.max(i1, i2));
            }),
            M.appl2("Mod", m, m, (t, ae1, ae2) -> {
                return new BinExpr("mod", ae1, ae2, (i1, i2) -> Math.floorMod(i1, i2));
            }),
            M.appl2("Div", m, m, (t, ae1, ae2) -> {
                return new BinExpr("div", ae1, ae2, (i1, i2) -> Math.floorDiv(i1, i2));
            })
        ));
        // @formatter:on
    }

}