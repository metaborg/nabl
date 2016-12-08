package org.metaborg.meta.nabl2.constraints;

import java.util.Optional;
import java.util.function.Function;

import org.metaborg.meta.nabl2.constraints.base.IBaseConstraint;
import org.metaborg.meta.nabl2.constraints.equality.IEqualityConstraint;
import org.metaborg.meta.nabl2.constraints.namebinding.INamebindingConstraint;
import org.metaborg.meta.nabl2.functions.CheckedFunction1;
import org.metaborg.meta.nabl2.terms.ITerm;

public interface IConstraint {

    Optional<ITerm> getOriginatingTerm();

    <T> T match(Cases<T> function);

    interface Cases<T> extends Function<IConstraint,T> {

        T caseBase(IBaseConstraint constraint);

        T caseEquality(IEqualityConstraint constraint);

        T caseNamebinding(INamebindingConstraint constraint);

        static <T> Cases<T> of(Function<IBaseConstraint,T> onBase, Function<IEqualityConstraint,T> onEquality,
                Function<INamebindingConstraint,T> onNamebinding) {
            return new Cases<T>() {

                @Override public T caseBase(IBaseConstraint constraint) {
                    return onBase.apply(constraint);
                }

                @Override public T caseEquality(IEqualityConstraint constraint) {
                    return onEquality.apply(constraint);
                }

                @Override public T caseNamebinding(INamebindingConstraint constraint) {
                    return onNamebinding.apply(constraint);
                }

                @Override public T apply(IConstraint constraint) {
                    return constraint.match(this);
                }

            };
        }

    }

    <T, E extends Throwable> T matchOrThrow(CheckedCases<T,E> function) throws E;

    interface CheckedCases<T, E extends Throwable> extends CheckedFunction1<IConstraint,T,E> {

        T caseBase(IBaseConstraint constraint) throws E;

        T caseEquality(IEqualityConstraint constraint) throws E;

        T caseNamebinding(INamebindingConstraint constraint) throws E;

        static <T, E extends Throwable> CheckedCases<T,E> of(CheckedFunction1<IBaseConstraint,T,E> onBase,
                CheckedFunction1<IEqualityConstraint,T,E> onEquality,
                CheckedFunction1<INamebindingConstraint,T,E> onNamebinding) {
            return new CheckedCases<T,E>() {

                @Override public T caseBase(IBaseConstraint constraint) throws E {
                    return onBase.apply(constraint);
                }

                @Override public T caseEquality(IEqualityConstraint constraint) throws E {
                    return onEquality.apply(constraint);
                }

                @Override public T caseNamebinding(INamebindingConstraint constraint) throws E {
                    return onNamebinding.apply(constraint);
                }

                @Override public T apply(IConstraint constraint) throws E {
                    return constraint.matchOrThrow(this);
                }

            };
        }

    }

}