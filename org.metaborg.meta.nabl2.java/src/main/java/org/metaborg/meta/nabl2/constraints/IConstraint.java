package org.metaborg.meta.nabl2.constraints;

import java.util.function.Function;

import org.metaborg.meta.nabl2.constraints.ast.IAstConstraint;
import org.metaborg.meta.nabl2.constraints.base.IBaseConstraint;
import org.metaborg.meta.nabl2.constraints.equality.IEqualityConstraint;
import org.metaborg.meta.nabl2.constraints.namebinding.INamebindingConstraint;
import org.metaborg.meta.nabl2.constraints.relations.IRelationConstraint;
import org.metaborg.meta.nabl2.constraints.sets.ISetConstraint;
import org.metaborg.meta.nabl2.functions.CheckedFunction1;

public interface IConstraint {

    MessageInfo getMessageInfo();

    <T> T match(Cases<T> function);

    interface Cases<T> {

        T caseAst(IAstConstraint constraint);

        T caseBase(IBaseConstraint constraint);

        T caseEquality(IEqualityConstraint constraint);

        T caseNamebinding(INamebindingConstraint constraint);

        T caseRelation(IRelationConstraint constraint);

        T caseSet(ISetConstraint constraint);

        static <T> Cases<T> of(
            // @formatter:off
            Function<IAstConstraint,T> onAst,
            Function<IBaseConstraint,T> onBase,
            Function<IEqualityConstraint,T> onEquality,
            Function<INamebindingConstraint,T> onNamebinding,
            Function<IRelationConstraint,T> onRelation,
            Function<ISetConstraint,T> onSet
            // @formatter:on
        ) {
            return new Cases<T>() {

                @Override public T caseAst(IAstConstraint constraint) {
                    return onAst.apply(constraint);
                }

                @Override public T caseBase(IBaseConstraint constraint) {
                    return onBase.apply(constraint);
                }

                @Override public T caseEquality(IEqualityConstraint constraint) {
                    return onEquality.apply(constraint);
                }

                @Override public T caseNamebinding(INamebindingConstraint constraint) {
                    return onNamebinding.apply(constraint);
                }

                @Override public T caseRelation(IRelationConstraint constraint) {
                    return onRelation.apply(constraint);
                }

                @Override public T caseSet(ISetConstraint constraint) {
                    return onSet.apply(constraint);
                }

            };
        }

    }

    <T, E extends Throwable> T matchOrThrow(CheckedCases<T,E> function) throws E;

    interface CheckedCases<T, E extends Throwable> {

        T caseAst(IAstConstraint constraint) throws E;

        T caseBase(IBaseConstraint constraint) throws E;

        T caseEquality(IEqualityConstraint constraint) throws E;

        T caseNamebinding(INamebindingConstraint constraint) throws E;

        T caseRelation(IRelationConstraint constraint) throws E;

        T caseSet(ISetConstraint constraint) throws E;

        static <T, E extends Throwable> CheckedCases<T,E> of(
            // @formatter:off
            CheckedFunction1<IAstConstraint,T,E> onAst,
            CheckedFunction1<IBaseConstraint,T,E> onBase,
            CheckedFunction1<IEqualityConstraint,T,E> onEquality,
            CheckedFunction1<INamebindingConstraint,T,E> onNamebinding,
            CheckedFunction1<IRelationConstraint,T,E> onRelation,
            CheckedFunction1<ISetConstraint,T,E> onSet
            // @formatter:on
        ) {
            return new CheckedCases<T,E>() {

                @Override public T caseAst(IAstConstraint constraint) throws E {
                    return onAst.apply(constraint);
                }

                @Override public T caseBase(IBaseConstraint constraint) throws E {
                    return onBase.apply(constraint);
                }

                @Override public T caseEquality(IEqualityConstraint constraint) throws E {
                    return onEquality.apply(constraint);
                }

                @Override public T caseNamebinding(INamebindingConstraint constraint) throws E {
                    return onNamebinding.apply(constraint);
                }

                @Override public T caseRelation(IRelationConstraint constraint) throws E {
                    return onRelation.apply(constraint);
                }

                @Override public T caseSet(ISetConstraint constraint) throws E {
                    return onSet.apply(constraint);
                }

            };
        }

    }

}