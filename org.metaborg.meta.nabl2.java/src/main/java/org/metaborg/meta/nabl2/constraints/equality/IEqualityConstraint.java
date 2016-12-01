package org.metaborg.meta.nabl2.constraints.equality;

import java.util.function.Function;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.functions.CheckedFunction1;

public interface IEqualityConstraint extends IConstraint {

    <T> T match(Cases<T> function);

    interface Cases<T> extends Function<IEqualityConstraint,T> {

        T caseEqual(Equal equal);

        T caseInequal(Inequal inequal);

        static <T> Cases<T> of(Function<Equal,T> onEqual, Function<Inequal,T> onInequal) {
            return new Cases<T>() {

                @Override public T caseEqual(Equal constraint) {
                    return onEqual.apply(constraint);
                }

                @Override public T caseInequal(Inequal constraint) {
                    return onInequal.apply(constraint);
                }

                @Override public T apply(IEqualityConstraint equality) {
                    return equality.match(this);
                }

            };
        }

    }

    <T, E extends Throwable> T matchOrThrow(CheckedCases<T,E> function) throws E;

    interface CheckedCases<T, E extends Throwable> extends CheckedFunction1<IEqualityConstraint,T,E> {

        T caseEqual(Equal equal) throws E;

        T caseInequal(Inequal inequal) throws E;

        static <T, E extends Throwable> CheckedCases<T,E> of(CheckedFunction1<Equal,T,E> onEqual,
                CheckedFunction1<Inequal,T,E> onInequal) {
            return new CheckedCases<T,E>() {

                @Override public T caseEqual(Equal constraint) throws E {
                    return onEqual.apply(constraint);
                }

                @Override public T caseInequal(Inequal constraint) throws E {
                    return onInequal.apply(constraint);
                }

                @Override public T apply(IEqualityConstraint equality) throws E {
                    return equality.matchOrThrow(this);
                }

            };
        }

    }

}