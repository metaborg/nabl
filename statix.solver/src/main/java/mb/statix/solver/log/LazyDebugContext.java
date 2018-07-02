package mb.statix.solver.log;

import java.util.Collections;

import org.metaborg.util.log.Level;

public class LazyDebugContext implements IDebugContext {

    private final IDebugContext debug;
    private int depth;
    private final Log log;

    public LazyDebugContext(IDebugContext debug) {
        this(debug, debug.getDepth(), new Log());
    }

    private LazyDebugContext(IDebugContext debug, int depth, Log log) {
        this.debug = debug;
        this.depth = depth;
        this.log = log;
    }

    @Override public Level getLevel() {
        return debug.getLevel();
    }

    @Override public int getDepth() {
        return depth;
    }

    @Override public IDebugContext subContext() {
        return new LazyDebugContext(debug, depth + 1, log);
    }

    @Override public void info(String fmt, Object... args) {
        if(Level.Info.compareTo(getLevel()) < 0) {
            return;
        }
        log.append(Level.Info, prefix(depth) + fmt, args);
    }

    @Override public void warn(String fmt, Object... args) {
        if(Level.Warn.compareTo(getLevel()) < 0) {
            return;
        }
        log.append(Level.Warn, prefix(depth) + fmt, args);
    }

    @Override public void error(String fmt, Object... args) {
        if(Level.Error.compareTo(getLevel()) < 0) {
            return;
        }
        log.append(Level.Error, prefix(depth) + fmt, args);
    }

    @Override public void log(Level level, String fmt, Object... args) {
        if(level.compareTo(getLevel()) < 0) {
            return;
        }
        log.append(level, prefix(depth) + fmt, args);
    }

    public void commit() {
        log.flush(debug);
    }

    public Log clear() {
        return log.clear();
    }

    private String prefix(int depth) {
        return String.join("", Collections.nCopies(depth, "| "));
    }

}