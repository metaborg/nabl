package org.metaborg.meta.nabl2.constraints;

public interface IConstraintVisitor<T> {

    T visit(True true_);

    T visit(False false_);

    T visit(Conj and);

    T visit(Equal equal);

    T visit(Inequal inequal);

}