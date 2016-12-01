package org.metaborg.meta.nabl2.constraints.namebinding;

import java.util.function.Function;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.functions.CheckedFunction1;

public interface INamebindingConstraint extends IConstraint {

    <T> T match(Cases<T> cases);

    interface Cases<T> extends Function<INamebindingConstraint,T> {

        T caseDecl(Decl decl);

        T caseResolve(Resolve resolve);

        static <T> Cases<T> of(Function<Decl,T> onDecl, Function<Resolve,T> onResolve) {
            return new Cases<T>() {

                @Override public T caseDecl(Decl constraint) {
                    return onDecl.apply(constraint);
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


    <T,E extends Throwable> T matchThrows(CheckedCases<T,E> cases) throws E;

    interface CheckedCases<T,E extends Throwable> extends CheckedFunction1<INamebindingConstraint,T,E> {

        T caseDecl(Decl decl) throws E;

        T caseResolve(Resolve resolve) throws E;

        static <T, E extends Throwable> CheckedCases<T,E> of(CheckedFunction1<Decl,T,E> onDecl, CheckedFunction1<Resolve,T,E> onResolve) {
            return new CheckedCases<T,E>() {

                @Override public T caseDecl(Decl constraint) throws E {
                    return onDecl.apply(constraint);
                }

                @Override public T caseResolve(Resolve constraint) throws E {
                    return onResolve.apply(constraint);
                }

                @Override public T apply(INamebindingConstraint constraint) throws E {
                    return constraint.matchThrows(this);
                }

            };
        }

    }

}