package mb.nabl2.terms;

import com.google.common.collect.ImmutableClassToInstanceMap;

public interface IConsList extends IListTerm, IApplTerm {

    ITerm getHead();

    IListTerm getTail();

    @Override IConsList withAttachments(ImmutableClassToInstanceMap<Object> value);

}