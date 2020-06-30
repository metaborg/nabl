package mb.statix.arithmetic;

import java.io.Serializable;
import java.util.Objects;

import org.metaborg.util.functions.Function2;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.Delay;

class BinExpr implements ArithExpr, Serializable {

    private static final long serialVersionUID = 1L;

    private final String op;
    private final ArithExpr ae1;
    private final ArithExpr ae2;
    private final Function2<Integer, Integer, Integer> f;

    public BinExpr(String op, ArithExpr ae1, ArithExpr ae2, Function2<Integer, Integer, Integer> f) {
        this.op = op;
        this.ae1 = ae1;
        this.ae2 = ae2;
        this.f = f;
    }

    @Override public int eval(IUniDisunifier unifier) throws Delay {
        return f.apply(ae1.eval(unifier), ae2.eval(unifier));
    }

    @Override public ArithExpr apply(ISubstitution.Immutable subst) {
        return new BinExpr(op, ae1.apply(subst), ae2.apply(subst), f);
    }

    @Override public ArithExpr apply(IRenaming subst) {
        return new BinExpr(op, ae1.apply(subst), ae2.apply(subst), f);
    }

    @Override public String toString(TermFormatter termToString) {
        return ae1.toString(termToString) + " " + op + " " + ae2.toString(termToString);
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

    @Override public boolean equals(Object o) {
        if(this == o)
            return true;
        if(o == null || getClass() != o.getClass())
            return false;
        BinExpr binExpr = (BinExpr) o;
        return Objects.equals(op, binExpr.op) && Objects.equals(ae1, binExpr.ae1) && Objects.equals(ae2, binExpr.ae2)
                && Objects.equals(f, binExpr.f);
    }

    @Override public int hashCode() {
        return Objects.hash(op, ae1, ae2, f);
    }
}
