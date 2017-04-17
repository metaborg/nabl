package org.metaborg.meta.nabl2.constraints;

import java.util.function.Function;

import org.metaborg.meta.nabl2.constraints.ast.IAstConstraint;
import org.metaborg.meta.nabl2.constraints.base.IBaseConstraint;
import org.metaborg.meta.nabl2.constraints.equality.IEqualityConstraint;
import org.metaborg.meta.nabl2.constraints.messages.IMessageContent;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.namebinding.INamebindingConstraint;
import org.metaborg.meta.nabl2.constraints.poly.IPolyConstraint;
import org.metaborg.meta.nabl2.constraints.relations.IRelationConstraint;
import org.metaborg.meta.nabl2.constraints.sets.ISetConstraint;
import org.metaborg.meta.nabl2.constraints.sym.ISymbolicConstraint;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.util.functions.CheckedFunction1;
import org.pcollections.PSet;

public interface IConstraint {

    IMessageInfo getMessageInfo();

    IMessageContent pp();

    PSet<ITermVar> getVars();
    
    <T> T match(Cases<T> function);

    interface Cases<T> {

        T caseAst(IAstConstraint constraint);

        T caseBase(IBaseConstraint constraint);

        T caseEquality(IEqualityConstraint constraint);

        T caseNamebinding(INamebindingConstraint constraint);

        T caseRelation(IRelationConstraint constraint);

        T caseSet(ISetConstraint constraint);

        T caseSym(ISymbolicConstraint constraint);

        T casePoly(IPolyConstraint constraint);

        static <T> Cases<T> of(
            // @formatter:off
            Function<IAstConstraint,T> onAst,
            Function<IBaseConstraint,T> onBase,
            Function<IEqualityConstraint,T> onEquality,
            Function<INamebindingConstraint,T> onNamebinding,
            Function<IRelationConstraint,T> onRelation,
            Function<ISetConstraint,T> onSet,
            Function<ISymbolicConstraint,T> onSym,
            Function<IPolyConstraint,T> onPoly
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

                @Override public T caseSym(ISymbolicConstraint constraint) {
                    return onSym.apply(constraint);
                }

                @Override public T casePoly(IPolyConstraint constraint) {
                    return onPoly.apply(constraint);
                }

            };
        }

    }

    <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> function) throws E;

    interface CheckedCases<T, E extends Throwable> {

        T caseAst(IAstConstraint constraint) throws E;

        T caseBase(IBaseConstraint constraint) throws E;

        T caseEquality(IEqualityConstraint constraint) throws E;

        T caseNamebinding(INamebindingConstraint constraint) throws E;

        T caseRelation(IRelationConstraint constraint) throws E;

        T caseSet(ISetConstraint constraint) throws E;

        T caseSym(ISymbolicConstraint cFact) throws E;

        T casePoly(IPolyConstraint constraint) throws E;

        static <T, E extends Throwable> CheckedCases<T, E> of(
            // @formatter:off
            CheckedFunction1<IAstConstraint,T,E> onAst,
            CheckedFunction1<IBaseConstraint,T,E> onBase,
            CheckedFunction1<IEqualityConstraint,T,E> onEquality,
            CheckedFunction1<INamebindingConstraint,T,E> onNamebinding,
            CheckedFunction1<IRelationConstraint,T,E> onRelation,
            CheckedFunction1<ISetConstraint,T,E> onSet,
            CheckedFunction1<ISymbolicConstraint,T,E> onSym,
            CheckedFunction1<IPolyConstraint,T,E> onPoly
            // @formatter:on
        ) {
            return new CheckedCases<T, E>() {

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

                @Override public T caseSym(ISymbolicConstraint constraint) throws E {
                    return onSym.apply(constraint);
                }

                @Override public T casePoly(IPolyConstraint constraint) throws E {
                    return onPoly.apply(constraint);
                }

            };
        }

    }

}