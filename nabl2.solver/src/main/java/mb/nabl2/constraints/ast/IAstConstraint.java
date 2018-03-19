package mb.nabl2.constraints.ast;

import java.util.function.Function;

import org.metaborg.util.functions.CheckedFunction1;

import mb.nabl2.constraints.IConstraint;

public interface IAstConstraint extends IConstraint {

    <T> T match(Cases<T> function);

    interface Cases<T> {

        T caseProperty(CAstProperty constraint);

        static <T> Cases<T> of(Function<CAstProperty, T> onProperty) {
            return new Cases<T>() {

                @Override public T caseProperty(CAstProperty constraint) {
                    return onProperty.apply(constraint);
                }

            };
        }

    }

    <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> function) throws E;

    interface CheckedCases<T, E extends Throwable> {

        T caseProperty(CAstProperty constraint) throws E;

        static <T, E extends Throwable> CheckedCases<T, E> of(CheckedFunction1<CAstProperty, T, E> onProperty) {
            return new CheckedCases<T, E>() {

                @Override public T caseProperty(CAstProperty constraint) throws E {
                    return onProperty.apply(constraint);
                }

            };
        }

    }

    public static boolean is(IConstraint constraint) {
        return constraint.match(IConstraint.Cases.of(c -> true, c -> false, c -> false, c -> false, c -> false,
                c -> false, c -> false, c -> false, c -> false, c -> false));
    }

}