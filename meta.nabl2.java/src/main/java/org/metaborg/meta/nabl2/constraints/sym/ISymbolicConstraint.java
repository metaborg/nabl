package org.metaborg.meta.nabl2.constraints.sym;

import java.util.function.Function;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.util.functions.CheckedFunction1;

public interface ISymbolicConstraint extends IConstraint {

    <T> T match(Cases<T> function);

    interface Cases<T> {

        T caseFact(CFact constraint);

        T caseGoal(CGoal constraint);

        static <T> Cases<T> of(Function<CFact,T> onFact, Function<CGoal,T> onGoal) {
            return new Cases<T>() {

                @Override public T caseFact(CFact constraint) {
                    return onFact.apply(constraint);
                }

                @Override public T caseGoal(CGoal constraint) {
                    return onGoal.apply(constraint);
                }

            };
        }

    }

    <T, E extends Throwable> T matchOrThrow(CheckedCases<T,E> function) throws E;

    interface CheckedCases<T, E extends Throwable> {

        T caseFact(CFact constraint) throws E;

        T caseGoal(CGoal constraint) throws E;

        static <T, E extends Throwable> CheckedCases<T,E> of(CheckedFunction1<CFact,T,E> onFact,
                CheckedFunction1<CGoal,T,E> onGoal) {
            return new CheckedCases<T,E>() {

                @Override public T caseFact(CFact constraint) throws E {
                    return onFact.apply(constraint);
                }

                @Override public T caseGoal(CGoal constraint) throws E {
                    return onGoal.apply(constraint);
                }

            };
        }

    }

}