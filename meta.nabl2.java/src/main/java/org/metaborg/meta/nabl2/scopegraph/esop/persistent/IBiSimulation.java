package org.metaborg.meta.nabl2.scopegraph.esop.persistent;

import java.util.function.Supplier;

public interface IBiSimulation {

    static void signalError() {
        // throw new IllegalStateException();
    }

    static <T> T choose(T one, T two) {
        return two;
    }

    static <T> T biSimulate(Supplier<T> supplierOne, Supplier<T> supplierTwo) {
        final T resultOne = supplierOne.get();
        final T resultTwo = supplierTwo.get();
        boolean equal = resultOne.equals(resultTwo);

        if (!equal)
            signalError();

        return choose(resultOne, resultTwo);
    }

}