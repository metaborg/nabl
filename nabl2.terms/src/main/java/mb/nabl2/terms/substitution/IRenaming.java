package mb.nabl2.terms.substitution;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.metaborg.util.collection.ImList;

import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;

public interface IRenaming {

    boolean isEmpty();

    Set<ITermVar> keySet();

    Set<ITermVar> valueSet();

    Set<? extends Map.Entry<ITermVar, ITermVar>> entrySet();

    ITermVar rename(ITermVar term);

    default List<ITermVar> rename(List<ITermVar> terms) {
        final ImList.Transient<ITermVar> vars = new ImList.Transient<>(terms.size());
        for(ITermVar term : terms) {
            vars.add(rename(term));
        }
        return vars.freeze();
    }

    default Set<ITermVar> rename(Set<ITermVar> terms) {
        return terms.stream().map(this::rename).collect(CapsuleCollectors.toSet());
    }

    ITerm apply(ITerm term);

    default List<ITerm> apply(List<ITerm> terms) {
        final ImList.Transient<ITerm> newTerms = new ImList.Transient<>(terms.size());
        for(ITerm term : terms) {
            newTerms.add(apply(term));
        }
        return newTerms.freeze();
    }

    ISubstitution.Immutable asSubstitution();

    Map<ITermVar, ITermVar> asMap();

}