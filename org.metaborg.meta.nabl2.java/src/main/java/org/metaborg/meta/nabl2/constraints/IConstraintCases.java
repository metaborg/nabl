package org.metaborg.meta.nabl2.constraints;

import java.util.function.Function;

import org.metaborg.meta.nabl2.constraints.base.IBaseConstraint;
import org.metaborg.meta.nabl2.constraints.equality.IEqualityConstraint;

public interface IConstraintCases<T> extends Function<IConstraint,T> {

    T caseOf(IBaseConstraint base);

    T caseOf(IEqualityConstraint equality);

    static <T> IConstraintCases<T> of(Function<IBaseConstraint,T> baseFunction,
            Function<IEqualityConstraint,T> equalityFunction) {
        return new IConstraintCases<T>() {

            @Override public T caseOf(IBaseConstraint base) {
                return baseFunction.apply(base);
            }

            @Override public T caseOf(IEqualityConstraint equality) {
                return equalityFunction.apply(equality);
            }

            @Override public T apply(IConstraint constraint) {
                return constraint.match(this);
            }

        };
    }

}