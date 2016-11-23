package org.metaborg.meta.nabl2.constraints.base;

import java.util.function.Function;

public interface IBaseConstraintCases<T> extends Function<IBaseConstraint,T> {

    T caseOf(True _true);

    T caseOf(False _false);

    static <T> IBaseConstraintCases<T> of(Function<True,T> trueFunction, Function<False,T> falseFunction) {
        return new IBaseConstraintCases<T>() {

            @Override public T caseOf(True _true) {
                return trueFunction.apply(_true);
            }

            @Override public T caseOf(False _false) {
                return falseFunction.apply(_false);
            }

            @Override public T apply(IBaseConstraint base) {
                return base.match(this);
            }

        };
    }

}