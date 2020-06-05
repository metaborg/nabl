package mb.statix.solver.log;

import java.util.Collections;

import org.metaborg.util.log.Level;

public class LazyDebugContext implements IDebugContext {

    private final IDebugContext debug;
    private int offset;
    private final Log log;

    public LazyDebugContext(IDebugContext debug) {
        this(debug, 0, new Log());
    }

    private LazyDebugContext(IDebugContext debug, int offset, Log log) {
        this.debug = debug;
        this.offset = offset;
        this.log = log;
    }

    @Override public Level getLevel() {
        return debug.getLevel();
    }

    @Override public int getDepth() {
        return debug.getDepth() + offset;
    }

    @Override public boolean isEnabled(Level level) {
        return debug.isEnabled(level);
    }

    @Override public IDebugContext subContext() {
        return new LazyDebugContext(debug, offset + 1, log);
    }

    @Override public void info(String fmt, Object... args) {
        if(isEnabled(Level.Info)) {
            log.append(Level.Info, prefix() + fmt, null, args);
        }
    }

    @Override public void info(String fmt, Throwable t, Object... args) {
        if(isEnabled(Level.Info)) {
            log.append(Level.Info, prefix() + fmt, t, args);
        }
    }

    @Override public void warn(String fmt, Object... args) {
        if(isEnabled(Level.Warn)) {
            log.append(Level.Warn, prefix() + fmt, null, args);
        }
    }

    @Override public void warn(String fmt, Throwable t, Object... args) {
        if(isEnabled(Level.Warn)) {
            log.append(Level.Warn, prefix() + fmt, t, args);
        }
    }

    @Override public void error(String fmt, Object... args) {
        if(isEnabled(Level.Error)) {
            log.append(Level.Error, prefix() + fmt, null, args);
        }
    }

    @Override public void error(String fmt, Throwable t, Object... args) {
        if(isEnabled(Level.Error)) {
            log.append(Level.Error, prefix() + fmt, t, args);
        }
    }

    @Override public void log(Level level, String fmt, Object... args) {
        if(isEnabled(level)) {
            log.append(level, prefix() + fmt, null, args);
        }
    }

    @Override public void log(Level level, String fmt, Throwable t, Object... args) {
        if(isEnabled(level)) {
            log.append(level, prefix() + fmt, t, args);
        }
    }

    public void commit() {
        log.flush(debug);
    }

    public Log clear() {
        return log.clear();
    }

    private String prefix() {
        return String.join("", Collections.nCopies(offset, "| "));
    }

}