package org.metaborg.meta.nabl2.scopegraph.terms;

import static org.metaborg.meta.nabl2.terms.build.TermBuild.B;
import static org.metaborg.meta.nabl2.terms.matching.TermMatch.M;

import java.util.List;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.terms.IApplTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.build.AbstractApplTerm;
import org.metaborg.meta.nabl2.terms.matching.TermMatch.IMatcher;
import org.metaborg.util.functions.Function1;

import com.google.common.collect.ImmutableList;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class Label extends AbstractApplTerm implements ILabel, IApplTerm {

    private static final String D_OP = "D";
    private static final String R_OP = "R";
    private static final String I_OP = "I";
    private static final String P_OP = "P";
    private static final String Q_OP = "?";
    private static final String OP = "Label";

    public static final Label D = ImmutableLabel.of(D_OP);
    public static final Label R = ImmutableLabel.of(R_OP);
    public static final Label I = ImmutableLabel.of(I_OP);
    public static final Label P = ImmutableLabel.of(P_OP);
    public static final Label Q = ImmutableLabel.of(Q_OP);

    // ILabel implementation

    @Value.Parameter @Override public abstract String getName();

    // IApplTerm implementation

    @Value.Lazy @Override public String getOp() {
        switch(getName()) {
            case D_OP:
            case R_OP:
            case I_OP:
            case P_OP:
            case Q_OP:
                return getName();
            default:
                return OP;
        }
    }

    @Value.Lazy @Override public List<ITerm> getArgs() {
        switch(getName()) {
            case D_OP:
            case R_OP:
            case I_OP:
            case P_OP:
            case Q_OP:                
                return ImmutableList.of();
            default:
                return ImmutableList.of((ITerm) B.newString(getName()));
        }
    }

    public static IMatcher<Label> matcher() {
        return matcher(l -> l);
    }

    public static <R> IMatcher<R> matcher(Function1<Label, R> f) {
        return M.cases(
            // @formatter:off
            M.appl0(D_OP, (t) -> f.apply(ImmutableLabel.of(D_OP))),
            M.appl0(R_OP, (t) -> f.apply(ImmutableLabel.of(R_OP))),
            M.appl0(I_OP, (t) -> f.apply(ImmutableLabel.of(I_OP))),
            M.appl0(P_OP, (t) -> f.apply(ImmutableLabel.of(P_OP))),
            M.appl0(Q_OP, (t) -> f.apply(ImmutableLabel.of(Q_OP))),            
            M.appl1(OP, M.stringValue(), (t,l) -> f.apply(ImmutableLabel.of(l)))
            // @formatter:on
        );
    }

    @Override
    protected Label check() {
        return this;
    }

    // Object implementation

    @Override public boolean equals(Object other) {
        return super.equals(other);
    }

    @Override public int hashCode() {
        return super.hashCode();
    }

    @Override public String toString() {
        return super.toString();
    }

}