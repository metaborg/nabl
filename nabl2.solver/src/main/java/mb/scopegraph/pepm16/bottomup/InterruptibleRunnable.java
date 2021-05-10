package mb.scopegraph.pepm16.bottomup;

import org.metaborg.util.task.ICancel;

public interface InterruptibleRunnable {

    void run(ICancel cancel) throws InterruptedException;

}