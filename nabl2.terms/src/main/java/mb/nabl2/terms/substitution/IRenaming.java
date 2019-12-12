package mb.nabl2.terms.substitution;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;

public interface IRenaming {

    Set<ITermVar> varSet();

    ITermVar rename(ITermVar term);

    default List<ITermVar> rename(List<ITermVar> terms) {
        return terms.stream().map(this::rename).collect(ImmutableList.toImmutableList());
    }

    ITerm apply(ITerm term);

    default List<ITerm> apply(List<ITerm> terms) {
        return terms.stream().map(this::apply).collect(ImmutableList.toImmutableList());
    }

}