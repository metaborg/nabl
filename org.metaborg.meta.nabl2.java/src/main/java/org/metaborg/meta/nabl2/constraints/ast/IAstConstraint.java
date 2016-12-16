package org.metaborg.meta.nabl2.constraints.ast;

import java.util.function.Function;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.util.functions.CheckedFunction1;

public interface IAstConstraint extends IConstraint {

    <T> T match(Cases<T> function);

    interface Cases<T> {

        T caseProperty(CAstProperty constraint);

        static <T> Cases<T> of(Function<CAstProperty,T> onProperty) {
            return new Cases<T>() {

                @Override public T caseProperty(CAstProperty constraint) {
                    return onProperty.apply(constraint);
                }

            };
        }

    }

    <T, E extends Throwable> T matchOrThrow(CheckedCases<T,E> function) throws E;

    interface CheckedCases<T, E extends Throwable> {

        T caseProperty(CAstProperty constraint) throws E;

        static <T, E extends Throwable> CheckedCases<T,E> of(CheckedFunction1<CAstProperty,T,E> onProperty) {
            return new CheckedCases<T,E>() {

                @Override public T caseProperty(CAstProperty constraint) throws E {
                    return onProperty.apply(constraint);
                }

            };
        }

    }

}