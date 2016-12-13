package org.metaborg.meta.nabl2.constraints.relations;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.functions.CheckedFunction1;
import org.metaborg.meta.nabl2.functions.Function1;

public interface IRelationConstraint extends IConstraint {

    <T> T match(Cases<T> cases);

    interface Cases<T> {

        T caseBuild(CBuildRelation build);

        T caseCheck(CCheckRelation check);

        static <T> Cases<T> of(
            // @formatter:off
            Function1<CBuildRelation,T> onBuild,
            Function1<CCheckRelation,T> onCheck
            // @formatter:on
        ) {
            return new Cases<T>() {

                @Override public T caseBuild(CBuildRelation build) {
                    return onBuild.apply(build);
                }

                @Override public T caseCheck(CCheckRelation check) {
                    return onCheck.apply(check);
                }

            };
        }

    }

    <T, E extends Throwable> T matchOrThrow(CheckedCases<T,E> cases) throws E;

    interface CheckedCases<T, E extends Throwable> {

        T caseBuild(CBuildRelation build) throws E;

        T caseCheck(CCheckRelation check) throws E;

        static <T, E extends Throwable> CheckedCases<T,E> of(
            // @formatter:off
            CheckedFunction1<CBuildRelation,T,E> onBuild,
            CheckedFunction1<CCheckRelation,T,E> onCheck
            // @formatter:on
        ) {
            return new CheckedCases<T,E>() {

                @Override public T caseBuild(CBuildRelation build) throws E {
                    return onBuild.apply(build);
                }

                @Override public T caseCheck(CCheckRelation check) throws E {
                    return onCheck.apply(check);
                }

            };
        }

    }

}