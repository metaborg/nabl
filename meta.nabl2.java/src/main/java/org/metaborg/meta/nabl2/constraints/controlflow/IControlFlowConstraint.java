package org.metaborg.meta.nabl2.constraints.controlflow;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.Function1;

public interface IControlFlowConstraint extends IConstraint {

    <T> T match(Cases<T> cases);

    interface Cases<T> {

        T caseDirectEdge(CFDirectEdge<?> directEdge);

        static <T> Cases<T> of(
            // @formatter:off
            Function1<CFDirectEdge<?>,T> onDirectEdge
            // @formatter:on
        ) {
            return new Cases<T>() {

                @Override public T caseDirectEdge(CFDirectEdge<?> directEdge) {
                    return onDirectEdge.apply(directEdge);
                }

            };
        }

    }

    <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E;

    interface CheckedCases<T, E extends Throwable> {

        T caseDirectEdge(CFDirectEdge<?> directEdge) throws E;

        static <T, E extends Throwable> CheckedCases<T, E> of(
            // @formatter:off
            CheckedFunction1<CFDirectEdge<?>,T,E> onDirectEdge
            // @formatter:on
        ) {
            return new CheckedCases<T, E>() {

                @Override public T caseDirectEdge(CFDirectEdge<?> directEdge) throws E {
                    return onDirectEdge.apply(directEdge);
                }

            };
        }

    }

    public static boolean is(IConstraint constraint) {
        return constraint.match(IConstraint.Cases.of(c -> false, c -> false, c -> false, c -> false, c -> false,
                c -> false, c -> false, c -> false, c -> false, c -> true));
    }

}