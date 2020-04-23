package mb.nabl2.terms;

import com.google.common.collect.ImmutableClassToInstanceMap;

public interface IStringTerm extends ITerm {

    String getValue();

    @Override IStringTerm withAttachments(ImmutableClassToInstanceMap<Object> value);

}
