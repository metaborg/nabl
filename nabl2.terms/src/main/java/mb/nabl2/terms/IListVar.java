package mb.nabl2.terms;

import com.google.common.collect.ImmutableClassToInstanceMap;

public interface IListVar extends IListTerm, ITermVar {

    @Override
    IListVar withAttachments(ImmutableClassToInstanceMap<Object> value);

}