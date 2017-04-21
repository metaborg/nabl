package org.metaborg.meta.nabl2.constraints.poly;

import java.util.function.Function;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.util.functions.CheckedFunction1;

public interface IPolyConstraint extends IConstraint {

    <T> T match(Cases<T> function);

    interface Cases<T> {

        T caseGeneralize(CGeneralize constraint);

        T caseInstantiate(CInstantiate constraint);

        static <T> Cases<T> of(Function<CGeneralize,T> onGeneralize, Function<CInstantiate,T> onInstantiate) {
            return new Cases<T>() {

                @Override public T caseGeneralize(CGeneralize constraint) {
                    return onGeneralize.apply(constraint);
                }

                @Override public T caseInstantiate(CInstantiate constraint) {
                    return onInstantiate.apply(constraint);
                }

            };
        }

    }

    <T, E extends Throwable> T matchOrThrow(CheckedCases<T,E> function) throws E;

    interface CheckedCases<T, E extends Throwable> {

        T caseGeneralize(CGeneralize constraint) throws E;

        T caseInstantiate(CInstantiate constraint) throws E;

        static <T, E extends Throwable> CheckedCases<T,E> of(CheckedFunction1<CGeneralize,T,E> onGeneralize,
                CheckedFunction1<CInstantiate,T,E> onInstantiate) {
            return new CheckedCases<T,E>() {

                @Override public T caseGeneralize(CGeneralize constraint) throws E {
                    return onGeneralize.apply(constraint);
                }

                @Override public T caseInstantiate(CInstantiate constraint) throws E {
                    return onInstantiate.apply(constraint);
                }

            };
        }

    }

}