package mb.nabl2.scopegraph.esop.bottomup;

import org.metaborg.util.task.ICancel;

public interface InterruptibleRunnable {

    void run(ICancel cancel) throws InterruptedException;

}