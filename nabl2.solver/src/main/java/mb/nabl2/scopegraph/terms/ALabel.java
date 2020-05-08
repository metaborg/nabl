package mb.nabl2.scopegraph.terms;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.functions.Function1;

import com.google.common.collect.ImmutableList;

import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.build.AbstractApplTerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ALabel extends AbstractApplTerm implements ILabel, IApplTerm {

    private static final String D_OP = "D";
    private static final String R_OP = "R";
    private static final String I_OP = "I";
    private static final String P_OP = "P";
    private static final String Q_OP = "?";
    private static final String OP = "Label";

    public static final Label D = Label.of(D_OP);
    public static final Label R = Label.of(R_OP);
    public static final Label I = Label.of(I_OP);
    public static final Label P = Label.of(P_OP);
    public static final Label Q = Label.of(Q_OP);

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
        // @formatter:off
        return M.cases(
            M.appl0(D_OP, (t) -> f.apply(D)),
            M.appl0(R_OP, (t) -> f.apply(R)),
            M.appl0(I_OP, (t) -> f.apply(I)),
            M.appl0(P_OP, (t) -> f.apply(P)),
            M.appl0(Q_OP, (t) -> f.apply(Q)),
            M.appl1(OP, M.stringValue(), (t,l) -> f.apply(Label.of(l)))
        );
        // @formatter:on
    }

    @Override @Value.Check protected ALabel check() {
        return this;
    }

    // Object implementation

    @Override public boolean equals(Object other) {
        if(other == null) {
            return false;
        }
        if(other == this) {
            return true;
        }
        if(!(other instanceof Label)) {
            return super.equals(other);
        }
        final Label that = (Label) other;
        if(!getName().equals(that.getName())) {
            return false;
        }
        return true;
    }

    @Override public int hashCode() {
        return super.hashCode();
    }

    @Override public String toString() {
        return super.toString();
    }

}