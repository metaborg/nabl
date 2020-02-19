package mb.nabl2.terms;

import com.google.common.collect.ImmutableClassToInstanceMap;

public interface IIntTerm extends ITerm {

    int getValue();

    @Override IIntTerm withAttachments(ImmutableClassToInstanceMap<Object> value);

}