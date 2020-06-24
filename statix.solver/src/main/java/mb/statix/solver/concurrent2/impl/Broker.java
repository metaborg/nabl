package mb.statix.solver.concurrent2.impl;

import java.util.Map;

import mb.statix.solver.concurrent2.IBroker;
import mb.statix.solver.concurrent2.IUnit;

public class Broker<S, L, D> implements IBroker<S, L, D> {

    private final Map<String, IUnit<S, L, D>> units;

}