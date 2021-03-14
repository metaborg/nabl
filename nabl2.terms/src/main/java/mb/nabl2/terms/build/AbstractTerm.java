package mb.nabl2.terms.build;

import java.util.Objects;

import org.immutables.value.Value;

import mb.nabl2.terms.IAttachments;
import mb.nabl2.terms.ITerm;

public abstract class AbstractTerm implements ITerm {

    @Override @Value.Auxiliary @Value.Default public IAttachments getAttachments() {
        return Attachments.empty();
    }

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