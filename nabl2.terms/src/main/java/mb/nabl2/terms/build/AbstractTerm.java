package mb.nabl2.terms.build;

import org.immutables.value.Value;

import com.google.common.collect.ImmutableClassToInstanceMap;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.Terms;

public abstract class AbstractTerm implements ITerm {

    @Override
    @Value.Auxiliary @Value.Default public ImmutableClassToInstanceMap<Object> getAttachments() {
        return Terms.NO_ATTACHMENTS;
    }

}