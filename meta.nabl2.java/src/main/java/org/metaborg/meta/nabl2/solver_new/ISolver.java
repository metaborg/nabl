package org.metaborg.meta.nabl2.solver_new;

import org.metaborg.meta.nabl2.constraints.IConstraint;

public interface ISolver<C extends IConstraint,R> {

    boolean add(C constraint) throws InterruptedException;

    boolean iterate() throws InterruptedException;

    R finish();

}