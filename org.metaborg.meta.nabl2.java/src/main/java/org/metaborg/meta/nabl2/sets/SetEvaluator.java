package org.metaborg.meta.nabl2.sets;

import org.metaborg.meta.nabl2.collections.Multibag;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.util.iterators.Iterables2;

public class SetEvaluator {

    public static IMatcher<Multibag<ITerm,ITerm>> matcher(IMatcher<Multibag<ITerm,ITerm>> elemMatcher) {
        return M.casesFix(m -> Iterables2.from(
            // @formatter:off
            elemMatcher,
            M.appl0("EmptySet",(t) -> Multibag.create()),
            M.appl2("Union", m, m, (t, leftSet, rightSet) -> {
                Multibag<ITerm,ITerm> result = Multibag.create();
                result.putAll(leftSet);
                result.putAll(rightSet);
                return result;
            }),
            M.appl2("Isect", m, m, (t, leftSet, rightSet) -> {
                Multibag<ITerm,ITerm> result = Multibag.create();
                result.putAll(leftSet);
                result.putAll(rightSet);
                result.keySet().retainAll(rightSet.keySet());
                result.keySet().retainAll(leftSet.keySet());
                return result;
            }),
            M.appl2("Diff", m, m, (t, leftSet, rightSet) -> {
                Multibag<ITerm,ITerm> result = Multibag.create();
                result.putAll(leftSet);
                result.keySet().removeAll(rightSet.keySet());
                return result;
            })
            // @formatter:on
        ));
    }

}