package org.metaborg.meta.nabl2.solver;

import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.terms.ITermVar;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class IncrementalSolverConfig {

    @Value.Parameter public abstract Set<ITermVar> getActiveVars();

    @Value.Parameter public abstract IMessageInfo getMessageInfo();

}