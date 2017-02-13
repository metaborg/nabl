package org.metaborg.meta.nabl2.constraints.base;

import java.util.function.Function;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.util.functions.CheckedFunction1;

public interface IBaseConstraint extends IConstraint {

    <T> T match(Cases<T> function);

    interface Cases<T> {

        T caseTrue(CTrue constraint);

        T caseFalse(CFalse constraint);

        static <T> Cases<T> of(Function<CTrue, T> onTrue, Function<CFalse, T> onFalse) {
            return new Cases<T>() {

                @Override public T caseTrue(CTrue constraint) {
                    return onTrue.apply(constraint);
                }

                @Override public T caseFalse(CFalse constraint) {
                    return onFalse.apply(constraint);
                }

            };
        }

    }


    <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> function) throws E;

    interface CheckedCases<T, E extends Throwable> {

        T caseTrue(CTrue constraint) throws E;

        T caseFalse(CFalse constraint) throws E;

        static <T, E extends Throwable> CheckedCases<T, E> of(CheckedFunction1<CTrue, T, E> onTrue,
            CheckedFunction1<CFalse, T, E> onFalse) {
            return new CheckedCases<T, E>() {

                @Override public T caseTrue(CTrue constraint) throws E {
                    return onTrue.apply(constraint);
                }

                @Override public T caseFalse(CFalse constraint) throws E {
                    return onFalse.apply(constraint);
                }

            };
        }

    }

}