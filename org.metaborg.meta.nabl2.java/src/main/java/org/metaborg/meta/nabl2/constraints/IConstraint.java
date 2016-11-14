package org.metaborg.meta.nabl2.constraints;

public interface IConstraint {

    <T> T accept(IConstraintVisitor<T> visitor);

}