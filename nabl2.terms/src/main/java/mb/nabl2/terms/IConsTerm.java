package mb.nabl2.terms;

import com.google.common.collect.ImmutableClassToInstanceMap;

public interface IConsTerm extends IListTerm {

    ITerm getHead();

    IListTerm getTail();

    @Override
    IConsTerm withAttachments(ImmutableClassToInstanceMap<Object> value);

}