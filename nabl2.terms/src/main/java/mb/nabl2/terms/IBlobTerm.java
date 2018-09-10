package mb.nabl2.terms;

import com.google.common.collect.ImmutableClassToInstanceMap;

public interface IBlobTerm extends ITerm {

    Object getValue();

    IBlobTerm withAttachments(ImmutableClassToInstanceMap<Object> value);

}