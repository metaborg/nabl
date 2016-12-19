package org.metaborg.meta.nabl2.constraints.equality;

import java.util.function.Function;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.util.functions.CheckedFunction1;

public interface IEqualityConstraint extends IConstraint {

    <T> T match(Cases<T> function);

    interface Cases<T> {

        T caseEqual(CEqual equal);

        T caseInequal(CInequal inequal);

        static <T> Cases<T> of(Function<CEqual,T> onEqual, Function<CInequal,T> onInequal) {
            return new Cases<T>() {

                @Override public T caseEqual(CEqual constraint) {
                    return onEqual.apply(constraint);
                }

                @Override public T caseInequal(CInequal constraint) {
                    return onInequal.apply(constraint);
                }

            };
        }

    }

    <T, E extends Throwable> T matchOrThrow(CheckedCases<T,E> function) throws E;

    interface CheckedCases<T, E extends Throwable> {

        T caseEqual(CEqual equal) throws E;

        T caseInequal(CInequal inequal) throws E;

        static <T, E extends Throwable> CheckedCases<T,E> of(CheckedFunction1<CEqual,T,E> onEqual,
                CheckedFunction1<CInequal,T,E> onInequal) {
            return new CheckedCases<T,E>() {

                @Override public T caseEqual(CEqual constraint) throws E {
                    return onEqual.apply(constraint);
                }

                @Override public T caseInequal(CInequal constraint) throws E {
                    return onInequal.apply(constraint);
                }

            };
        }

    }

}