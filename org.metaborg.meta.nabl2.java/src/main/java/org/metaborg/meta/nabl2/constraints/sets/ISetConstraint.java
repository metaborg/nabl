package org.metaborg.meta.nabl2.constraints.sets;

import java.util.function.Function;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.util.functions.CheckedFunction1;

public interface ISetConstraint extends IConstraint {

    <T> T match(Cases<T> function);

    interface Cases<T> {

        T caseSubsetEq(CSubsetEq constraint);

        T caseDistinct(CDistinct constraint);

        static <T> Cases<T> of(Function<CSubsetEq,T> onSubsetEq, Function<CDistinct,T> onDistinct) {
            return new Cases<T>() {

                @Override public T caseSubsetEq(CSubsetEq constraint) {
                    return onSubsetEq.apply(constraint);
                }

                @Override public T caseDistinct(CDistinct constraint) {
                    return onDistinct.apply(constraint);
                }

            };
        }

    }

    <T, E extends Throwable> T matchOrThrow(CheckedCases<T,E> function) throws E;

    interface CheckedCases<T, E extends Throwable> {

        T caseSubsetEq(CSubsetEq equal) throws E;

        T caseDistinct(CDistinct inequal) throws E;

        static <T, E extends Throwable> CheckedCases<T,E> of(CheckedFunction1<CSubsetEq,T,E> onSubsetEq,
                CheckedFunction1<CDistinct,T,E> onDistinct) {
            return new CheckedCases<T,E>() {

                @Override public T caseSubsetEq(CSubsetEq constraint) throws E {
                    return onSubsetEq.apply(constraint);
                }

                @Override public T caseDistinct(CDistinct constraint) throws E {
                    return onDistinct.apply(constraint);
                }

            };
        }

    }

}