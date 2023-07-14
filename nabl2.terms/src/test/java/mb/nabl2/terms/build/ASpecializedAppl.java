package mb.nabl2.terms.build;

import java.util.Objects;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.collection.ImList;

import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.IAttachments;
import mb.nabl2.terms.ITerm;

import static mb.nabl2.terms.build.TermBuild.B;

@Value.Immutable
@Serial.Version(42L)
abstract class ASpecializedAppl extends AbstractApplTerm {

    static final String OP = "Specialized";

    @Value.Parameter public abstract String getFirstArg();

    @Value.Parameter public abstract int getSecondArg();

    @Override public String getOp() {
        return OP;
    }

    @Override public ImList.Immutable<ITerm> getArgs() {
        return ImList.Immutable.of(B.newString(getFirstArg()), B.newInt(getSecondArg()));
    }

    @Override public IApplTerm withAttachments(IAttachments value) {
        return SpecializedAppl.copyOf(this).withAttachments(value);
    }

    @Override protected ASpecializedAppl check() {
        return this;
    }

    @Override public int hashCode() {
        // We use the super-class hashcode to ensure that a SpecializedAppl and an IApplTerm
        // with the same term representation have the same hash code.
        return super.hashCode();
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ASpecializedAppl)) return super.equals(other);
        ASpecializedAppl that = (ASpecializedAppl)other;
        if (this.hashCode() != that.hashCode()) return false;
        // @formatter:off
        return Objects.equals(this.getFirstArg(), that.getFirstArg())
            && Objects.equals(this.getSecondArg(), that.getSecondArg());
        // @formatter:on
    }

    @Override public String toString() {
        return getFirstArg() + ":" + getSecondArg();
    }

}
