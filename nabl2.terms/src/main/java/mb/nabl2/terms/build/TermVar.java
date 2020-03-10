package mb.nabl2.terms.build;

import java.util.Objects;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMultiset;

import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class TermVar extends AbstractTerm implements ITermVar {

    @Value.Parameter @Override public abstract String getResource();

    @Value.Parameter @Override public abstract String getName();

    @Value.Check protected void check() {
        Preconditions.checkState(!(getResource().isEmpty() && getName().isEmpty()),
                "'resource' and 'name' cannot both be empty");
    }

    @Override public boolean isGround() {
        return false;
    }

    @Value.Lazy @Override public ImmutableMultiset<ITermVar> getVars() {
        return ImmutableMultiset.of(this);
    }

    @Override public <T> T match(ITerm.Cases<T> cases) {
        return cases.caseVar(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(ITerm.CheckedCases<T, E> cases) throws E {
        return cases.caseVar(this);
    }

    @Override public <T> T match(IListTerm.Cases<T> cases) {
        return cases.caseVar(this);
    }

    @Override public <T, E extends Throwable> T matchOrThrow(IListTerm.CheckedCases<T, E> cases) throws E {
        return cases.caseVar(this);
    }

    @Value.Lazy @Override public int hashCode() {
        return Objects.hash(
            getResource(),
            getName()
        );
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        return other instanceof ITermVar
            && equals((ITermVar)other, false);
    }

    public boolean equals(ITermVar that, boolean compareAttachments) {
        if (this == that) return true;
        if (that == null) return false;
        if (this.hashCode() != that.hashCode()) return false;
        // @formatter:off
        return Objects.equals(this.getResource(), that.getResource())
            && Objects.equals(this.getName(), that.getName())
            && (!compareAttachments || Objects.equals(this.getAttachments(), that.getAttachments()));
        // @formatter:on
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("?");
        if(!getResource().isEmpty()) {
            sb.append(getResource());
            sb.append("-");
        }
        sb.append(getName());
        return sb.toString();
    }

}
