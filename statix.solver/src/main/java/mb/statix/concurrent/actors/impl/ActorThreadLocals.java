package mb.statix.concurrent.actors.impl;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

class ActorThreadLocals {

    private static final ILogger logger = LoggerUtils.logger(ActorThreadLocals.class);

    static final ThreadLocal<IActorInternal<?>> current = ThreadLocal.withInitial(() -> {
        final IllegalStateException ex = new IllegalStateException("Cannot get current actor.");
        logger.error("Cannot get current actor.", ex);
        throw ex;
    });

}