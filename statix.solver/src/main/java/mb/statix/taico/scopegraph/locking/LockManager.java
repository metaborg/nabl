package mb.statix.taico.scopegraph.locking;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import mb.statix.taico.module.IModule;
import mb.statix.taico.util.IOwnable;

public class LockManager implements IOwnable {
    private final Set<Lock> locks = new HashSet<>();
    private final IModule owner;
    private final Object reason;
    
    public LockManager(IModule owner) {
        this(owner, null);
    }
    
    public LockManager(IModule owner, Object reason) {
        this.owner = owner;
        this.reason = reason;
    }
    
    @Override
    public IModule getOwner() {
        return owner;
    }
    
    public synchronized void acquire(Lock lock) {
        if (locks.contains(lock)) return;
        lock.lock();
        locks.add(lock);
    }
    
    public synchronized void release(Lock lock) {
        try {
            lock.unlock();
        } finally {
            locks.remove(lock);
        }
    }
    
    public synchronized void releaseAll() {
        Iterator<Lock> it = locks.iterator();
        Throwable e = null;
        while (it.hasNext()) {
            try {
                it.next().unlock();
            } catch (Throwable ex) {
                e = ex;
            } finally {
                it.remove();
            }
        }
        
        if (e != null) throw new RuntimeException("Releasing one or more locks caused an exception!", e);
    }
    
    public synchronized boolean isDone() {
        return locks.isEmpty();
    }
    
    /**
     * Absorbs the given lock manager. The given lock manager will be cleared.
     * 
     * @param lockManager
     *      the lock manager to absorb
     */
    public synchronized void absorb(LockManager lockManager) {
        if (lockManager == null) return;
        
        synchronized (lockManager) {
            if (lockManager.isDone()) System.err.println("DEBUG: Absorbing empty lock manager " + lockManager);
            Iterator<Lock> it = lockManager.locks.iterator();
            while (it.hasNext()) {
                Lock lock = it.next();
                if (!locks.add(lock)) {
                    //We already have this lock, so reduce the counter
                    try {
                        lock.unlock();
                    } catch (Throwable t) {
                        System.err.println("Error unlocking a lock while absorbing lock manager " + lockManager + ": " + t);
                    }
                }
                it.remove();
            }
        }
    }
    
    @Override
    public String toString() {
        return "LockManager<" + owner.getId() + ", reason=" + reason + ">";
    }
    
    @Override
    protected void finalize() throws Throwable {
        if (locks.isEmpty()) return;
        
        System.err.println("FATAL: Lock manager " + this + " is getting garbage collected while there are still locks held.");
        System.err.println("FATAL RECOVERY: Releasing all locks of garbage collected lock manager");
        
        for (Lock lock : locks) {
            try {
                lock.unlock();
            } catch (Throwable t) {
                //Silently swallow
            }
        }
    }
}
