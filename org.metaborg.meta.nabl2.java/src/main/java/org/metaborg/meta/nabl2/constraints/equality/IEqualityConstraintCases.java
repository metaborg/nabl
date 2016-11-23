package org.metaborg.meta.nabl2.constraints.equality;

import java.util.function.Function;

public interface IEqualityConstraintCases<T> extends Function<IEqualityConstraint,T> {

    T apply(Equal equal);

    T apply(Inequal inequal);

    static <T> IEqualityConstraintCases<T> of(Function<Equal,T> trueFunction, Function<Inequal,T> falseFunction) {
        return new IEqualityConstraintCases<T>() {

            @Override public T apply(Equal _true) {
                return trueFunction.apply(_true);
            }

            @Override public T apply(Inequal _false) {
                return falseFunction.apply(_false);
            }

            @Override public T apply(IEqualityConstraint equality) {
                return equality.match(this);
            }

        };
    }

}