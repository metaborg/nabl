package mb.nabl2.terms.build;

import java.util.Objects;

import org.immutables.value.Value;

import mb.nabl2.terms.IAttachments;
import mb.nabl2.terms.ITerm;

public abstract class AbstractTerm implements ITerm {

    @Override public IAttachments getAttachments() {
        return __attachments();
    }

    @Override public ITerm withAttachments(IAttachments value) {
        return value == __attachments() ? this : with__attachments(value);
    }

    @Value.Auxiliary @Value.Default public IAttachments __attachments() {
        return Attachments.empty();
    }

    public abstract ITerm with__attachments(IAttachments value);

    @Override public abstract int hashCode();

    @Override public abstract boolean equals(Object other);

    @Override public boolean equals(Object other, boolean compareAttachments) {
        if(this == other)
            return true;
        if(!(other instanceof ITerm))
            return false;
        // @formatter:off
        return equals(other)
            && (!compareAttachments || Objects.equals(this.getAttachments(), ((ITerm)other).getAttachments()));
        // @formatter:on
    }

}