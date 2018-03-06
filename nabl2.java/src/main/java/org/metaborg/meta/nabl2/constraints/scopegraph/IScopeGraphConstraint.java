package org.metaborg.meta.nabl2.constraints.scopegraph;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.Function1;

public interface IScopeGraphConstraint extends IConstraint {

    <T> T match(Cases<T> cases);

    interface Cases<T> {

        T caseDecl(CGDecl decl);

        T caseRef(CGRef ref);

        T caseDirectEdge(CGDirectEdge directEdge);

        T caseAssoc(CGExportEdge assoc);

        T caseImport(CGImportEdge importEdge);

        static <T> Cases<T> of(
            // @formatter:off
            Function1<CGDecl,T> onDecl,
            Function1<CGRef,T> onRef,
            Function1<CGDirectEdge,T> onDirectEdge,
            Function1<CGExportEdge,T> onExportEdge,
            Function1<CGImportEdge,T> onImportEdge
            // @formatter:on
        ) {
            return new Cases<T>() {

                @Override public T caseDecl(CGDecl constraint) {
                    return onDecl.apply(constraint);
                }

                @Override public T caseRef(CGRef ref) {
                    return onRef.apply(ref);
                }

                @Override public T caseDirectEdge(CGDirectEdge directEdge) {
                    return onDirectEdge.apply(directEdge);
                }

                @Override public T caseAssoc(CGExportEdge exportEdge) {
                    return onExportEdge.apply(exportEdge);
                }

                @Override public T caseImport(CGImportEdge importEdge) {
                    return onImportEdge.apply(importEdge);
                }

            };
        }

    }

    <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E;

    interface CheckedCases<T, E extends Throwable> {

        T caseDecl(CGDecl decl) throws E;

        T caseRef(CGRef ref) throws E;

        T caseDirectEdge(CGDirectEdge directEdge) throws E;

        T caseAssoc(CGExportEdge assoc) throws E;

        T caseImport(CGImportEdge importEdge) throws E;

        static <T, E extends Throwable> CheckedCases<T, E> of(
            // @formatter:off
            CheckedFunction1<CGDecl,T,E> onDecl,
            CheckedFunction1<CGRef,T,E> onRef,
            CheckedFunction1<CGDirectEdge,T,E> onDirectEdge,
            CheckedFunction1<CGExportEdge,T,E> onExportEdge,
            CheckedFunction1<CGImportEdge,T,E> onImportEdge
            // @formatter:on
        ) {
            return new CheckedCases<T, E>() {

                @Override public T caseDecl(CGDecl constraint) throws E {
                    return onDecl.apply(constraint);
                }

                @Override public T caseRef(CGRef constraint) throws E {
                    return onRef.apply(constraint);
                }

                @Override public T caseDirectEdge(CGDirectEdge directEdge) throws E {
                    return onDirectEdge.apply(directEdge);
                }

                @Override public T caseAssoc(CGExportEdge exportEdge) throws E {
                    return onExportEdge.apply(exportEdge);
                }

                @Override public T caseImport(CGImportEdge importEdge) throws E {
                    return onImportEdge.apply(importEdge);
                }

            };
        }

    }

    public static boolean is(IConstraint constraint) {
        return constraint.match(IConstraint.Cases.of(c -> false, c -> false, c -> false, c -> true, c -> false,
                c -> false, c -> false, c -> false, c -> false, c -> false));
    }

}