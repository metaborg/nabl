package org.metaborg.meta.nabl2.constraints.controlflow;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.Function1;

public interface IControlFlowConstraint extends IConstraint {

    <T> T match(Cases<T> cases);

    interface Cases<T> {

        T caseDecl(CFDecl decl);

        T caseDirectEdge(CFDirectEdge<?> directEdge);

        T caseProperty(CFDeclProperty property);

        static <T> Cases<T> of(
            // @formatter:off
            Function1<CFDecl,T> onDecl,
            Function1<CFDirectEdge<?>,T> onDirectEdge,
            Function1<CFDeclProperty, T> onProperty
            // @formatter:on
        ) {
            return new Cases<T>() {

                @Override public T caseDecl(CFDecl constraint) {
                    return onDecl.apply(constraint);
                }

                @Override public T caseDirectEdge(CFDirectEdge<?> directEdge) {
                    return onDirectEdge.apply(directEdge);
                }

                @Override public T caseProperty(CFDeclProperty property) {
                    return onProperty.apply(property);
                }

            };
        }

    }

    <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E;

    interface CheckedCases<T, E extends Throwable> {

        T caseDecl(CFDecl decl) throws E;

        T caseDirectEdge(CFDirectEdge<?> directEdge) throws E;

        T caseProperty(CFDeclProperty property) throws E;

        static <T, E extends Throwable> CheckedCases<T, E> of(
            // @formatter:off
            CheckedFunction1<CFDecl,T,E> onDecl,
            CheckedFunction1<CFDirectEdge<?>,T,E> onDirectEdge,
            CheckedFunction1<CFDeclProperty,T,E> onProperty
            // @formatter:on
        ) {
            return new CheckedCases<T, E>() {

                @Override public T caseDecl(CFDecl constraint) throws E {
                    return onDecl.apply(constraint);
                }

                @Override public T caseDirectEdge(CFDirectEdge<?> directEdge) throws E {
                    return onDirectEdge.apply(directEdge);
                }

                @Override public T caseProperty(CFDeclProperty property) throws E {
                    return onProperty.apply(property);
                }

            };
        }

    }

    public static boolean is(IConstraint constraint) {
        return constraint.match(IConstraint.Cases.of(c -> false, c -> false, c -> false, c -> false, c -> false,
                c -> false, c -> false, c -> false, c -> false, c -> true));
    }

}