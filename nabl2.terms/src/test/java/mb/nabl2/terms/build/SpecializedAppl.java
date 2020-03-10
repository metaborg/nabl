package mb.nabl2.terms.build;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Objects;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.ITerm;

@Value.Immutable
@Serial.Version(42L)
abstract class SpecializedAppl extends AbstractApplTerm {

    static final String OP = "Specialized";

    @Value.Parameter public abstract String getFirstArg();

    @Value.Parameter public abstract int getSecondArg();

    @Override public String getOp() {
        return OP;
    }

    @Override public List<ITerm> getArgs() {
        return ImmutableList.of(B.newString(getFirstArg()), B.newInt(getSecondArg()));
    }

    @Override public IApplTerm withAttachments(ImmutableClassToInstanceMap<Object> value) {
        return ImmutableSpecializedAppl.copyOf(this).withAttachments(value);
    }

    @Override protected SpecializedAppl check() {
        return this;
    }

    @Override public int hashCode() {
        // We use the super-class hashcode to ensure that a SpecializedAppl and an IApplTerm
        // with the same term representation have the same hash code.
        return super.hashCode();
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SpecializedAppl)) return super.equals(other);
        SpecializedAppl that = (SpecializedAppl)other;
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
