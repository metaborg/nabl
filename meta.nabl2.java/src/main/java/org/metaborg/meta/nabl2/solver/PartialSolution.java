package org.metaborg.meta.nabl2.solver;

import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.solver.messages.IMessages;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.unification.IUnifier;

@Value.Immutable
@Serial.Version(value = 1L)
public abstract class PartialSolution implements IPartialSolution {

    @Value.Parameter @Override public abstract SolverConfig getConfig();

    @Value.Parameter @Override public abstract Set<ITerm> getInterface();

    @Value.Parameter @Override public abstract Set<IConstraint> getResidualConstraints();

    @Value.Parameter @Override public abstract IUnifier getUnifier();

    @Value.Parameter public abstract IMessages getMessages();

}