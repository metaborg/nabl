package mb.statix.taico.solver.concurrent.locking;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class DummyLock implements Lock {
    public static final DummyLock DUMMY = new DummyLock();

    private DummyLock() {}

    @Override
    public void lock() {}

    @Override
    public void lockInterruptibly() throws InterruptedException {}

    @Override
    public boolean tryLock() {
        return true;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return true;
    }

    @Override
    public void unlock() {}

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }

    public static DummyLock of() {
        return DUMMY;
    }
}
