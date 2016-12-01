package org.metaborg.meta.nabl2.constraints.base;

import java.util.function.Function;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.functions.CheckedFunction1;

public interface IBaseConstraint extends IConstraint {

    <T> T match(Cases<T> function);

    interface Cases<T> extends Function<IBaseConstraint,T> {

        T caseTrue(True constraint);

        T caseFalse(False constraint);

        static <T> Cases<T> of(Function<True,T> onTrue, Function<False,T> onFalse) {
            return new Cases<T>() {

                @Override public T caseTrue(True constraint) {
                    return onTrue.apply(constraint);
                }

                @Override public T caseFalse(False constraint) {
                    return onFalse.apply(constraint);
                }

                @Override public T apply(IBaseConstraint base) {
                    return base.match(this);
                }

            };
        }

    }


    <T, E extends Throwable> T matchOrThrow(CheckedCases<T,E> function) throws E;

    interface CheckedCases<T, E extends Throwable> extends CheckedFunction1<IBaseConstraint,T,E> {

        T caseTrue(True constraint) throws E;

        T caseFalse(False constraint) throws E;

        static <T, E extends Throwable> CheckedCases<T,E> of(CheckedFunction1<True,T,E> onTrue,
                CheckedFunction1<False,T,E> onFalse) {
            return new CheckedCases<T,E>() {

                @Override public T caseTrue(True constraint) throws E {
                    return onTrue.apply(constraint);
                }

                @Override public T caseFalse(False constraint) throws E {
                    return onFalse.apply(constraint);
                }

                @Override public T apply(IBaseConstraint base) throws E {
                    return base.matchOrThrow(this);
                }

            };
        }

    }

}