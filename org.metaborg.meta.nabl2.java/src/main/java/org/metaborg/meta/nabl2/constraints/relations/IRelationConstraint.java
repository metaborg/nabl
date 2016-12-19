package org.metaborg.meta.nabl2.constraints.relations;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.util.functions.CheckedFunction1;
import org.metaborg.meta.nabl2.util.functions.Function1;

public interface IRelationConstraint extends IConstraint {

    <T> T match(Cases<T> cases);

    interface Cases<T> {

        T caseBuild(CBuildRelation constraint);

        T caseCheck(CCheckRelation constraint);

        T caseLub(CLub constraint);

        T caseGlb(CGlb constraint);

        static <T> Cases<T> of(
            // @formatter:off
            Function1<CBuildRelation,T> onBuild,
            Function1<CCheckRelation,T> onCheck,
            Function1<CLub,T> onLub,
            Function1<CGlb,T> onGlb
            // @formatter:on
        ) {
            return new Cases<T>() {

                @Override public T caseBuild(CBuildRelation build) {
                    return onBuild.apply(build);
                }

                @Override public T caseCheck(CCheckRelation check) {
                    return onCheck.apply(check);
                }

                @Override public T caseLub(CLub constraint) {
                    return onLub.apply(constraint);
                }

                @Override public T caseGlb(CGlb constraint) {
                    return onGlb.apply(constraint);
                }

            };
        }

    }

    <T, E extends Throwable> T matchOrThrow(CheckedCases<T,E> cases) throws E;

    interface CheckedCases<T, E extends Throwable> {

        T caseBuild(CBuildRelation constraint) throws E;

        T caseCheck(CCheckRelation constraint) throws E;

        T caseLub(CLub constraint) throws E;

        T caseGlb(CGlb constraint) throws E;

        static <T, E extends Throwable> CheckedCases<T,E> of(
            // @formatter:off
            CheckedFunction1<CBuildRelation,T,E> onBuild,
            CheckedFunction1<CCheckRelation,T,E> onCheck,
            CheckedFunction1<CLub,T,E> onLub,
            CheckedFunction1<CGlb,T,E> onGlb
            // @formatter:on
        ) {
            return new CheckedCases<T,E>() {

                @Override public T caseBuild(CBuildRelation constraint) throws E {
                    return onBuild.apply(constraint);
                }

                @Override public T caseCheck(CCheckRelation constraint) throws E {
                    return onCheck.apply(constraint);
                }

                @Override public T caseLub(CLub constraint) throws E {
                    return onLub.apply(constraint);
                }

                @Override public T caseGlb(CGlb constraint) throws E {
                    return onGlb.apply(constraint);
                }

            };
        }

    }

}