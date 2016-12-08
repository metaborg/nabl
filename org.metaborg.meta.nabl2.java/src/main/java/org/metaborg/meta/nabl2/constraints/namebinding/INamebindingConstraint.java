package org.metaborg.meta.nabl2.constraints.namebinding;

import java.util.function.Function;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.functions.CheckedFunction1;
import org.metaborg.meta.nabl2.functions.Function1;

public interface INamebindingConstraint extends IConstraint {

    <T> T match(Cases<T> cases);

    interface Cases<T> extends Function<INamebindingConstraint,T> {

        T caseDecl(Decl decl);

        T caseRef(Ref ref);

        T caseDirectEdge(DirectEdge directEdge);

        T caseAssoc(Assoc exportEdge);

        T caseImport(Import importEdge);

        T caseResolve(Resolve resolve);

        static <T> Cases<T> of(
            // @formatter:off
            Function<Decl,T> onDecl,
            Function1<Ref,T> onRef,
            Function1<DirectEdge,T> onDirectEdge,
            Function1<Assoc,T> onExportEdge,
            Function1<Import,T> onImportEdge,
            Function<Resolve,T> onResolve
            // @formatter:on
        ) {
            return new Cases<T>() {

                @Override public T caseDecl(Decl constraint) {
                    return onDecl.apply(constraint);
                }

                @Override public T caseRef(Ref ref) {
                    return onRef.apply(ref);
                }

                @Override public T caseDirectEdge(DirectEdge directEdge) {
                    return onDirectEdge.apply(directEdge);
                }

                @Override public T caseAssoc(Assoc exportEdge) {
                    return onExportEdge.apply(exportEdge);
                }

                @Override public T caseImport(Import importEdge) {
                    return onImportEdge.apply(importEdge);
                }

                @Override public T caseResolve(Resolve constraint) {
                    return onResolve.apply(constraint);
                }

                @Override public T apply(INamebindingConstraint constraint) {
                    return constraint.match(this);
                }

            };
        }

    }

    <T, E extends Throwable> T matchOrThrow(CheckedCases<T,E> cases) throws E;

    interface CheckedCases<T, E extends Throwable> extends CheckedFunction1<INamebindingConstraint,T,E> {

        T caseDecl(Decl decl) throws E;

        T caseRef(Ref ref) throws E;

        T caseDirectEdge(DirectEdge directEdge) throws E;

        T caseAssoc(Assoc exportEdge) throws E;

        T caseImport(Import importEdge) throws E;

        T caseResolve(Resolve resolve) throws E;

        static <T, E extends Throwable> CheckedCases<T,E> of(
            // @formatter:off
            CheckedFunction1<Decl,T,E> onDecl,
            CheckedFunction1<Ref,T,E> onRef,
            CheckedFunction1<DirectEdge,T,E> onDirectEdge,
            CheckedFunction1<Assoc,T,E> onExportEdge,
            CheckedFunction1<Import,T,E> onImportEdge,
            CheckedFunction1<Resolve,T,E> onResolve
            // @formatter:on
        ) {
            return new CheckedCases<T,E>() {

                @Override public T caseDecl(Decl constraint) throws E {
                    return onDecl.apply(constraint);
                }

                @Override public T caseRef(Ref constraint) throws E {
                    return onRef.apply(constraint);
                }

                @Override public T caseDirectEdge(DirectEdge directEdge) throws E {
                    return onDirectEdge.apply(directEdge);
                }

                @Override public T caseAssoc(Assoc exportEdge) throws E {
                    return onExportEdge.apply(exportEdge);
                }

                @Override public T caseImport(Import importEdge) throws E {
                    return onImportEdge.apply(importEdge);
                }

                @Override public T caseResolve(Resolve constraint) throws E {
                    return onResolve.apply(constraint);
                }

                @Override public T apply(INamebindingConstraint constraint) throws E {
                    return constraint.matchOrThrow(this);
                }

            };
        }

    }

}