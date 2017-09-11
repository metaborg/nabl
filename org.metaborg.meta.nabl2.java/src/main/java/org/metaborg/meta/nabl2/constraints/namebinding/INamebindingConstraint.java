package org.metaborg.meta.nabl2.constraints.namebinding;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.util.functions.CheckedFunction1;
import org.metaborg.meta.nabl2.util.functions.Function1;

public interface INamebindingConstraint extends IConstraint {

    <T> T match(Cases<T> cases);

    interface Cases<T> {

        T caseDecl(CGDecl decl);

        T caseRef(CGRef ref);

        T caseDirectEdge(CGDirectEdge<?> directEdge);

        T caseAssoc(CGExportEdge assoc);

        T caseImport(CGImportEdge<?> importEdge);

        T caseResolve(CResolve resolve);

        T caseAssoc(CAssoc assoc);

        T caseProperty(CDeclProperty property);

        static <T> Cases<T> of(
            // @formatter:off
            Function1<CGDecl,T> onDecl,
            Function1<CGRef,T> onRef,
            Function1<CGDirectEdge<?>,T> onDirectEdge,
            Function1<CGExportEdge,T> onExportEdge,
            Function1<CGImportEdge<?>,T> onImportEdge,
            Function1<CResolve,T> onResolve,
            Function1<CAssoc,T> onAssoc,
            Function1<CDeclProperty,T> onProperty
            // @formatter:on
        ) {
            return new Cases<T>() {

                @Override public T caseDecl(CGDecl constraint) {
                    return onDecl.apply(constraint);
                }

                @Override public T caseRef(CGRef ref) {
                    return onRef.apply(ref);
                }

                @Override public T caseDirectEdge(CGDirectEdge<?> directEdge) {
                    return onDirectEdge.apply(directEdge);
                }

                @Override public T caseAssoc(CGExportEdge exportEdge) {
                    return onExportEdge.apply(exportEdge);
                }

                @Override public T caseImport(CGImportEdge<?> importEdge) {
                    return onImportEdge.apply(importEdge);
                }

                @Override public T caseResolve(CResolve constraint) {
                    return onResolve.apply(constraint);
                }

                @Override public T caseAssoc(CAssoc assoc) {
                    return onAssoc.apply(assoc);
                }

                @Override public T caseProperty(CDeclProperty property) {
                    return onProperty.apply(property);
                }

            };
        }

    }

    <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E;

    interface CheckedCases<T, E extends Throwable> {

        T caseDecl(CGDecl decl) throws E;

        T caseRef(CGRef ref) throws E;

        T caseDirectEdge(CGDirectEdge<?> directEdge) throws E;

        T caseAssoc(CGExportEdge assoc) throws E;

        T caseImport(CGImportEdge<?> importEdge) throws E;

        T caseResolve(CResolve resolve) throws E;

        T caseAssoc(CAssoc assoc) throws E;

        T caseProperty(CDeclProperty property) throws E;

        static <T, E extends Throwable> CheckedCases<T, E> of(
            // @formatter:off
            CheckedFunction1<CGDecl,T,E> onDecl,
            CheckedFunction1<CGRef,T,E> onRef,
            CheckedFunction1<CGDirectEdge<?>,T,E> onDirectEdge,
            CheckedFunction1<CGExportEdge,T,E> onExportEdge,
            CheckedFunction1<CGImportEdge<?>,T,E> onImportEdge,
            CheckedFunction1<CResolve,T,E> onResolve,
            CheckedFunction1<CAssoc,T,E> onAssoc,
            CheckedFunction1<CDeclProperty,T,E> onProperty
            // @formatter:on
        ) {
            return new CheckedCases<T, E>() {

                @Override public T caseDecl(CGDecl constraint) throws E {
                    return onDecl.apply(constraint);
                }

                @Override public T caseRef(CGRef constraint) throws E {
                    return onRef.apply(constraint);
                }

                @Override public T caseDirectEdge(CGDirectEdge<?> directEdge) throws E {
                    return onDirectEdge.apply(directEdge);
                }

                @Override public T caseAssoc(CGExportEdge exportEdge) throws E {
                    return onExportEdge.apply(exportEdge);
                }

                @Override public T caseImport(CGImportEdge<?> importEdge) throws E {
                    return onImportEdge.apply(importEdge);
                }

                @Override public T caseResolve(CResolve constraint) throws E {
                    return onResolve.apply(constraint);
                }

                @Override public T caseAssoc(CAssoc assoc) throws E {
                    return onAssoc.apply(assoc);
                }

                @Override public T caseProperty(CDeclProperty property) throws E {
                    return onProperty.apply(property);
                }

            };
        }

    }

}