package mb.nabl2.constraints.sets;

import java.util.function.Function;

import org.metaborg.util.functions.CheckedFunction1;

import mb.nabl2.constraints.IConstraint;

public interface ISetConstraint extends IConstraint {

    <T> T match(Cases<T> function);

    interface Cases<T> {

        T caseSubsetEq(CSubsetEq subseteq);

        T caseDistinct(CDistinct distinct);

        T caseEval(CEvalSet eval);

        static <T> Cases<T> of(Function<CSubsetEq, T> onSubsetEq, Function<CDistinct, T> onDistinct,
                Function<CEvalSet, T> onEval) {
            return new Cases<T>() {

                @Override public T caseSubsetEq(CSubsetEq constraint) {
                    return onSubsetEq.apply(constraint);
                }

                @Override public T caseDistinct(CDistinct constraint) {
                    return onDistinct.apply(constraint);
                }

                @Override public T caseEval(CEvalSet eval) {
                    return onEval.apply(eval);
                }

            };
        }

    }

    <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> function) throws E;

    interface CheckedCases<T, E extends Throwable> {

        T caseSubsetEq(CSubsetEq subseteq) throws E;

        T caseDistinct(CDistinct distinct) throws E;

        T caseEval(CEvalSet eval) throws E;

        static <T, E extends Throwable> CheckedCases<T, E> of(CheckedFunction1<CSubsetEq, T, E> onSubsetEq,
                CheckedFunction1<CDistinct, T, E> onDistinct, CheckedFunction1<CEvalSet, T, E> onEval) {
            return new CheckedCases<T, E>() {

                @Override public T caseSubsetEq(CSubsetEq constraint) throws E {
                    return onSubsetEq.apply(constraint);
                }

                @Override public T caseDistinct(CDistinct constraint) throws E {
                    return onDistinct.apply(constraint);
                }

                @Override public T caseEval(CEvalSet eval) throws E {
                    return onEval.apply(eval);
                }

            };
        }

    }

    public static boolean is(IConstraint constraint) {
        return constraint.match(IConstraint.Cases.of(c -> false, c -> false, c -> false, c -> false, c -> false,
                c -> false, c -> true, c -> false));
    }

}