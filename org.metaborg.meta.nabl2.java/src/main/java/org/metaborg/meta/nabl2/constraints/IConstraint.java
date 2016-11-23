package org.metaborg.meta.nabl2.constraints;

public interface IConstraint {

    <T> T match(IConstraintCases<T> function);

}