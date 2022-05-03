package mb.statix.arithmetic;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Optional;

import org.metaborg.util.functions.Function2;
import org.metaborg.util.functions.Predicate2;

import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.statix.spoofax.StatixTerms;

public class ArithTerms {

    public static IMatcher<ArithTest> matchTest() {
        return (subj, u) -> {
            if(subj instanceof IApplTerm) {
                final IApplTerm applTerm = (IApplTerm) subj;
                final String termOp = applTerm.getOp();
                switch(termOp) {
                    case "Equal":
                        return M.appl0("Equal", (t) -> {
                            return new ArithTest("=", new Eq(), true);
                        }).match(subj, u);
                    case "NotEqual":
                        return M.appl0("NotEqual", (t) -> {
                            return new ArithTest("\\=", new Neq(), false);
                        }).match(subj, u);
                    case "GreaterThanEqual":
                        return M.appl0("GreaterThanEqual", (t) -> {
                            return new ArithTest(">=", new Gte(), false);
                        }).match(subj, u);
                    case "LessThanEqual":
                        return M.appl0("LessThanEqual", (t) -> {
                            return new ArithTest("=<", new Lte(), false);
                        }).match(subj, u);
                    case "GreaterThan":
                        return M.appl0("GreaterThan", (t) -> {
                            return new ArithTest(">", new Ge(), false);
                        }).match(subj, u);
                    case "LessThan":
                        return M.appl0("LessThan", (t) -> {
                            return new ArithTest("<", new Le(), false);
                        }).match(subj, u);
                }
            }
            return Optional.empty();
        };
    }

    public static IMatcher<ArithExpr> matchExpr() {
        return (subj, u) -> {
            if(subj instanceof IApplTerm) {
                final IApplTerm applTerm = (IApplTerm) subj;
                final String termOp = applTerm.getOp();
                switch(termOp) {
                    case "Var":
                        return StatixTerms.varTerm().map(t -> (ArithExpr) new TermExpr(t)).match(subj, u);
                    case "Int":
                        return StatixTerms.intTerm().map(t -> (ArithExpr) new TermExpr(t)).match(subj, u);
                    case "Add":
                        return M.appl2("Add", matchExpr(), matchExpr(), (t, ae1, ae2) -> {
                            return (ArithExpr) new BinExpr("+", ae1, ae2, new Add());
                        }).match(subj, u);
                    case "Mul":
                        return M.appl2("Mul", matchExpr(), matchExpr(), (t, ae1, ae2) -> {
                            return (ArithExpr) new BinExpr("*", ae1, ae2, new Mul());
                        }).match(subj, u);
                    case "Sub":
                        return M.appl2("Sub", matchExpr(), matchExpr(), (t, ae1, ae2) -> {
                            return (ArithExpr) new BinExpr("-", ae1, ae2, new Sub());
                        }).match(subj, u);
                    case "Min":
                        return M.appl2("Min", matchExpr(), matchExpr(), (t, ae1, ae2) -> {
                            return (ArithExpr) new BinExpr("min", ae1, ae2, new Min());
                        }).match(subj, u);
                    case "Max":
                        return M.appl2("Max", matchExpr(), matchExpr(), (t, ae1, ae2) -> {
                            return (ArithExpr) new BinExpr("max", ae1, ae2, new Max());
                        }).match(subj, u);
                    case "Mod":
                        return M.appl2("Mod", matchExpr(), matchExpr(), (t, ae1, ae2) -> {
                            return (ArithExpr) new BinExpr("mod", ae1, ae2, new Mod());
                        }).match(subj, u);
                    case "Div":
                        return M.appl2("Div", matchExpr(), matchExpr(), (t, ae1, ae2) -> {
                            return (ArithExpr) new BinExpr("div", ae1, ae2, new Div());
                        }).match(subj, u);
                }
            }
            return Optional.empty();
        };
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

    private static class Eq implements Predicate2<Integer, Integer>, Serializable {

        private static final long serialVersionUID = 1L;

        @Override public boolean test(Integer i1, Integer i2) {
            return i1 == i2;
        }

    }

    private static class Neq implements Predicate2<Integer, Integer>, Serializable {

        private static final long serialVersionUID = 1L;

        @Override public boolean test(Integer i1, Integer i2) {
            return i1 != i2;
        }

    }

    private static class Gte implements Predicate2<Integer, Integer>, Serializable {

        private static final long serialVersionUID = 1L;

        @Override public boolean test(Integer i1, Integer i2) {
            return i1 >= i2;
        }

    }

    private static class Lte implements Predicate2<Integer, Integer>, Serializable {

        private static final long serialVersionUID = 1L;

        @Override public boolean test(Integer i1, Integer i2) {
            return i1 <= i2;
        }

    }

    private static class Ge implements Predicate2<Integer, Integer>, Serializable {

        private static final long serialVersionUID = 1L;

        @Override public boolean test(Integer i1, Integer i2) {
            return i1 > i2;
        }

    }

    private static class Le implements Predicate2<Integer, Integer>, Serializable {

        private static final long serialVersionUID = 1L;

        @Override public boolean test(Integer i1, Integer i2) {
            return i1 < i2;
        }

    }
}