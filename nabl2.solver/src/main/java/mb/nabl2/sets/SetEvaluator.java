package mb.nabl2.sets;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Optional;

import org.metaborg.util.iterators.Iterables2;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;
import mb.nabl2.terms.matching.TermMatch.IMatcher;

public class SetEvaluator {

    public static <T> IMatcher<ISetProducer<T>> matcher(IMatcher<ISetProducer<T>> elemMatcher) {
        // @formatter:off
        return M.<ISetProducer<T>>casesFix(m -> Iterables2.from(
            elemMatcher,
            M.appl0("EmptySet", (t) -> () -> Set.Immutable.of()),
            M.appl2("Union", m, m, (t, leftSet, rightSet) -> () -> {
                final Set.Immutable<IElement<T>> _leftSet = leftSet.apply();
                final Set.Immutable<IElement<T>> _rightSet = rightSet.apply();
                return Set.Immutable.union(_leftSet, _rightSet);
            }),
            M.appl3("Isect", m, SetTerms.projectionMatcher(), m, (t, leftSet, proj, rightSet) -> () -> {
                final Set.Immutable<IElement<T>> _leftSet = leftSet.apply();
                final Set.Immutable<IElement<T>> _rightSet = rightSet.apply();
                SetMultimap.Immutable<Object,IElement<T>> leftProj = project(_leftSet, proj);
                SetMultimap.Immutable<Object,IElement<T>> rightProj = project(_rightSet, proj);
                Set.Transient<IElement<T>> result = Set.Transient.of();
                for(Object key : leftProj.keySet()) {
                    if(rightProj.containsKey(key)) {
                        result.__insertAll(leftProj.get(key));
                        result.__insertAll(rightProj.get(key));
                    }
                }
                return result.freeze();
            }),
            M.appl3("Lsect", m, SetTerms.projectionMatcher(), m, (t, leftSet, proj, rightSet) -> () -> {
                final Set.Immutable<IElement<T>> _leftSet = leftSet.apply();
                final Set.Immutable<IElement<T>> _rightSet = rightSet.apply();
                SetMultimap.Immutable<Object,IElement<T>> leftProj = project(_leftSet, proj);
                SetMultimap.Immutable<Object,IElement<T>> rightProj = project(_rightSet, proj);
                Set.Transient<IElement<T>> result = Set.Transient.of();
                for(Object key : leftProj.keySet()) {
                    if(rightProj.containsKey(key)) {
                        result.__insertAll(leftProj.get(key));
                    }
                }
                return result.freeze();
            }),
            M.appl3("Diff", m, SetTerms.projectionMatcher(), m, (t, leftSet, proj, rightSet) -> () -> {
                final Set.Immutable<IElement<T>> _leftSet = leftSet.apply();
                final Set.Immutable<IElement<T>> _rightSet = rightSet.apply();
                SetMultimap.Immutable<Object,IElement<T>> leftProj = project(_leftSet, proj);
                SetMultimap.Immutable<Object,IElement<T>> rightProj = project(_rightSet, proj);
                Set.Transient<IElement<T>> result = Set.Transient.of();
                for(Object key : leftProj.keySet()) {
                    if(!rightProj.containsKey(key)) {
                        result.__insertAll(leftProj.get(key));
                    }
                }
                return result.freeze();
            })
        ));
        // @formatter:on
    }

    public static <T> SetMultimap.Immutable<Object, IElement<T>> project(Set<IElement<T>> elems,
            Optional<String> proj) {
        SetMultimap.Transient<Object, IElement<T>> result = SetMultimap.Transient.of();
        for(IElement<T> elem : elems) {
            result.__insert(proj.map(p -> elem.project(p)).orElseGet(() -> elem.getValue()), elem);
        }
        return result.freeze();
    }

}