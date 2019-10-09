package mb.nabl2.terms.build;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.build.ImmutableSpecializedAppl;

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
        return super.hashCode();
    }

    @Override public boolean equals(Object other) {
        return super.equals(other);
    }

    @Override public String toString() {
        return getFirstArg() + ":" + getSecondArg();
    }

}