package org.metaborg.meta.nabl2.scopegraph;

import java.util.List;
import java.util.function.Function;

import org.metaborg.meta.nabl2.functions.CheckedFunction1;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.TermVar;

public interface IResolutionResult {

    <T> T match(Cases<T> cases);

    interface Cases<T> {

        T resolvedTo(List<ITerm> declarations);

        T incompleteIn(TermVar var);

        static <T> Cases<T> of(Function<List<ITerm>,T> resolvedFunction, Function<TermVar,T> incompleteFunction) {
            return new Cases<T>() {

                @Override public T resolvedTo(List<ITerm> declarations) {
                    return resolvedFunction.apply(declarations);
                }

                @Override public T incompleteIn(TermVar var) {
                    return incompleteFunction.apply(var);
                }

            };
        }

    }


    <T, E extends Throwable> T matchThrows(CheckedCases<T,E> cases);

    interface CheckedCases<T, E extends Throwable> {

        T resolvedTo(List<ITerm> declarations) throws E;

        T incompleteIn(TermVar var) throws E;

        static <T, E extends Throwable> CheckedCases<T,E> of(CheckedFunction1<List<ITerm>,T,E> resolvedFunction,
                CheckedFunction1<TermVar,T,E> incompleteFunction) {
            return new CheckedCases<T,E>() {

                @Override public T resolvedTo(List<ITerm> declarations) throws E {
                    return resolvedFunction.apply(declarations);
                }

                @Override public T incompleteIn(TermVar var) throws E {
                    return incompleteFunction.apply(var);
                }

            };
        }

    }

}