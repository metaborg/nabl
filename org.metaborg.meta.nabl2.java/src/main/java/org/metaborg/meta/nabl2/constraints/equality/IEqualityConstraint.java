package org.metaborg.meta.nabl2.constraints.equality;

import org.metaborg.meta.nabl2.constraints.IConstraint;

public interface IEqualityConstraint extends IConstraint {

    <T> T match(IEqualityConstraintCases<T> function);
    
}