package mb.nabl2.terms.build;

import org.immutables.value.Value;

import com.google.common.collect.ImmutableClassToInstanceMap;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.Terms;

import java.util.Objects;

public abstract class AbstractTerm implements ITerm {

    @Override
    @Value.Auxiliary @Value.Default public ImmutableClassToInstanceMap<Object> getAttachments() {
        return Terms.NO_ATTACHMENTS;
    }

    @Override public abstract int hashCode();

    @Override public abstract boolean equals(Object other);

    @Override public boolean equals(Object other, boolean compareAttachments) {
        if (this == other) return true;
        if (!(other instanceof ITerm)) return false;
        // @formatter:off
        return equals(other)
            && (!compareAttachments || Objects.equals(this.getAttachments(), ((ITerm)other).getAttachments()));
        // @formatter:on
    }
}
