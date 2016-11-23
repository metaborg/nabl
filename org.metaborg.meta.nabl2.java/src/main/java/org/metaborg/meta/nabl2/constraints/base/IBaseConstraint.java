package org.metaborg.meta.nabl2.constraints.base;

import org.metaborg.meta.nabl2.constraints.IConstraint;

public interface IBaseConstraint extends IConstraint {

    <T> T match(IBaseConstraintCases<T> function);
    
}