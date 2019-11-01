package mb.statix.modular.observers;

import java.util.function.Consumer;

import mb.nabl2.terms.ITermVar;

@FunctionalInterface
public interface VarCompleteObserver extends Consumer<ITermVar> {
    
}
