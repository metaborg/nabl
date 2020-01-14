package mb.nabl2.terms;

import com.google.common.collect.ImmutableClassToInstanceMap;

public interface INilList extends IListTerm, IApplTerm {

    @Override
    INilList withAttachments(ImmutableClassToInstanceMap<Object> value);

}