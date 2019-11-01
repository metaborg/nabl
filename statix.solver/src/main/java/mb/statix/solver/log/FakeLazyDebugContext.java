package mb.statix.solver.log;

import org.metaborg.util.log.Level;

/**
 * A fake lazy debug context which is not in fact lazy. Messages are still kept and can be
 * absorbed into other logs.
 */
public class FakeLazyDebugContext extends LazyDebugContext {

    public FakeLazyDebugContext(IDebugContext context) {
        super(context);
    }
    
    @Override public FakeLazyDebugContext subContext() {
        return new FakeLazyDebugContext(debug.subContext());
    }

    @Override public void info(String fmt, Object... args) {
        if(isEnabled(Level.Info)) {
            String msg = prefix + fmt;
            debug.log(Level.Info, msg, args);
            log.append(Level.Info, msg, args);
        }
    }

    @Override public void warn(String fmt, Object... args) {
        if(isEnabled(Level.Warn)) {
            String msg = prefix + fmt;
            debug.log(Level.Warn, msg, args);
            log.append(Level.Warn, msg, args);
        }
    }

    @Override public void error(String fmt, Object... args) {
        if(isEnabled(Level.Error)) {
            String msg = prefix + fmt;
            debug.log(Level.Error, msg, args);
            log.append(Level.Error, msg, args);
        }
    }

    @Override public void log(Level level, String fmt, Object... args) {
        if(isEnabled(level)) {
            String msg = prefix + fmt;
            debug.log(level, msg, args);
            log.append(level, msg, args);
        }
    }
    
    @Override public void _log(Level level, String fmt, Object... args) {
        String msg = prefix + fmt;
        debug.log(level, msg, args);
        log.append(level, msg, args);
    }
    
    @Override
    public void commit() {
        log.clear();
    }
}
