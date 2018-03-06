package mb.nabl2.constraints.base;

import java.util.function.Function;

import org.metaborg.util.functions.CheckedFunction1;

import mb.nabl2.constraints.IConstraint;

public interface IBaseConstraint extends IConstraint {

    <T> T match(Cases<T> function);

    interface Cases<T> {

        T caseTrue(CTrue constraint);

        T caseFalse(CFalse constraint);

        T caseConj(CConj constraint);

        T caseExists(CExists constraint);

        T caseNew(CNew constraint);

        static <T> Cases<T> of(Function<CTrue, T> onTrue, Function<CFalse, T> onFalse, Function<CConj, T> onConj,
                Function<CExists, T> onExists, Function<CNew, T> onNew) {
            return new Cases<T>() {

                @Override public T caseTrue(CTrue constraint) {
                    return onTrue.apply(constraint);
                }

                @Override public T caseFalse(CFalse constraint) {
                    return onFalse.apply(constraint);
                }

                public T caseConj(CConj constraint) {
                    return onConj.apply(constraint);
                }

                @Override public T caseExists(CExists constraint) {
                    return onExists.apply(constraint);
                }

                public T caseNew(CNew constraint) {
                    return onNew.apply(constraint);
                }

            };
        }

    }

    <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> function) throws E;

    interface CheckedCases<T, E extends Throwable> {

        T caseTrue(CTrue constraint) throws E;

        T caseFalse(CFalse constraint) throws E;

        T caseConj(CConj constraint) throws E;

        T caseExists(CExists constraint) throws E;

        T caseNew(CNew constraint) throws E;

        static <T, E extends Throwable> CheckedCases<T, E> of(CheckedFunction1<CTrue, T, E> onTrue,
                CheckedFunction1<CFalse, T, E> onFalse, CheckedFunction1<CConj, T, E> onConj,
                CheckedFunction1<CExists, T, E> onExists, CheckedFunction1<CNew, T, E> onNew) {
            return new CheckedCases<T, E>() {

                @Override public T caseTrue(CTrue constraint) throws E {
                    return onTrue.apply(constraint);
                }

                @Override public T caseFalse(CFalse constraint) throws E {
                    return onFalse.apply(constraint);
                }

                @Override public T caseConj(CConj constraint) throws E {
                    return onConj.apply(constraint);
                }

                @Override public T caseExists(CExists constraint) throws E {
                    return onExists.apply(constraint);
                }

                @Override public T caseNew(CNew constraint) throws E {
                    return onNew.apply(constraint);
                }

            };
        }

    }

}