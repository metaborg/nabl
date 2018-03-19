package mb.nabl2.constraints.controlflow;

import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.Function1;

import mb.nabl2.constraints.IConstraint;

public interface IControlFlowConstraint extends IConstraint {

    <T> T match(Cases<T> cases);

    interface Cases<T> {

        T caseDirectEdge(CFDirectEdge<?> directEdge);

        T caseTFAppl(CTFAppl tfAppl);

        static <T> Cases<T> of(
            // @formatter:off
            Function1<CFDirectEdge<?>,T> onDirectEdge,
            Function1<CTFAppl,T> onTFAppl
            // @formatter:on
        ) {
            return new Cases<T>() {

                @Override public T caseDirectEdge(CFDirectEdge<?> directEdge) {
                    return onDirectEdge.apply(directEdge);
                }

                @Override public T caseTFAppl(CTFAppl tfAppl) {
                    return onTFAppl.apply(tfAppl);
                }

            };
        }

    }

    <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E;

    interface CheckedCases<T, E extends Throwable> {

        T caseDirectEdge(CFDirectEdge<?> directEdge) throws E;

        T caseTFAppl(CTFAppl tfAppl) throws E;

        static <T, E extends Throwable> CheckedCases<T, E> of(
            // @formatter:off
            CheckedFunction1<CFDirectEdge<?>,T,E> onDirectEdge,
            CheckedFunction1<CTFAppl,T,E> onTFAppl
            // @formatter:on
        ) {
            return new CheckedCases<T, E>() {

                @Override public T caseDirectEdge(CFDirectEdge<?> directEdge) throws E {
                    return onDirectEdge.apply(directEdge);
                }

                @Override public T caseTFAppl(CTFAppl tfAppl) throws E {
                    return onTFAppl.apply(tfAppl);
                }

            };
        }

    }

    public static boolean is(IConstraint constraint) {
        return constraint.match(IConstraint.Cases.of(c -> false, c -> false, c -> false, c -> false, c -> false,
                c -> false, c -> false, c -> false, c -> false, c -> true));
    }

}