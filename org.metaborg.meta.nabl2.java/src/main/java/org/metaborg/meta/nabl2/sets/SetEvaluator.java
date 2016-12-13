package org.metaborg.meta.nabl2.sets;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class SetEvaluator {

    public static IMatcher<Multimap<ITerm,ITerm>> matcher(IMatcher<Multimap<ITerm,ITerm>> elemMatcher) {
        return M.casesFix(m -> Iterables2.from(
            // @formatter:off
            elemMatcher,
            M.appl0("EmptySet",(t) -> HashMultimap.create()),
            M.appl2("Union", m, m, (t, leftSet, rightSet) -> {
                Multimap<ITerm,ITerm> result = HashMultimap.create(leftSet);
                result.putAll(leftSet);
                result.putAll(rightSet);
                return result;
            }),
            M.appl2("Isect", m, m, (t, leftSet, rightSet) -> {
                Multimap<ITerm,ITerm> result = HashMultimap.create();
                result.putAll(leftSet);
                result.putAll(rightSet);
                result.keySet().retainAll(rightSet.keySet());
                result.keySet().retainAll(leftSet.keySet());
                return result;
            }),
            M.appl2("Diff", m, m, (t, leftSet, rightSet) -> {
                Multimap<ITerm,ITerm> result = HashMultimap.create(leftSet);
                result.keySet().removeAll(rightSet.keySet());
                return result;
            })
            // @formatter:on
        ));
    }

}