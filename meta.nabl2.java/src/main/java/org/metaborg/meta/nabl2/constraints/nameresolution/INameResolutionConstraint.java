package org.metaborg.meta.nabl2.constraints.nameresolution;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.Function1;

public interface INameResolutionConstraint extends IConstraint {

    <T> T match(Cases<T> cases);

    interface Cases<T> {

        T caseResolve(CResolve resolve);

        T caseAssoc(CAssoc assoc);

        T caseProperty(CDeclProperty property);

        static <T> Cases<T> of(
            // @formatter:off
            Function1<CResolve,T> onResolve,
            Function1<CAssoc,T> onAssoc,
            Function1<CDeclProperty,T> onProperty
            // @formatter:on
        ) {
            return new Cases<T>() {

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

        T caseResolve(CResolve resolve) throws E;

        T caseAssoc(CAssoc assoc) throws E;

        T caseProperty(CDeclProperty property) throws E;

        static <T, E extends Throwable> CheckedCases<T, E> of(
            // @formatter:off
            CheckedFunction1<CResolve,T,E> onResolve,
            CheckedFunction1<CAssoc,T,E> onAssoc,
            CheckedFunction1<CDeclProperty,T,E> onProperty
            // @formatter:on
        ) {
            return new CheckedCases<T, E>() {

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