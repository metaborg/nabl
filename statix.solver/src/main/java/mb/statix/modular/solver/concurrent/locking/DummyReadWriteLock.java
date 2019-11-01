package mb.statix.modular.solver.concurrent.locking;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

public class DummyReadWriteLock implements ReadWriteLock {
    public static final DummyReadWriteLock DUMMY = new DummyReadWriteLock();

    private DummyReadWriteLock() {}

    @Override
    public Lock readLock() {
        return DummyLock.of();
    }

    @Override
    public Lock writeLock() {
        return DummyLock.of();
    }

    public static DummyReadWriteLock of() {
        return DUMMY;
    }
}
