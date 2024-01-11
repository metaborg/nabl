package mb.statix.solver.log;

import jakarta.annotation.Nullable;

import org.metaborg.util.log.Level;

public class Log {

    private Entry first;
    private Entry last;

    public Log() {
        this(null, null);
    }

    private Log(Entry first, Entry last) {
        this.first = first;
        this.last = last;
    }

    public boolean isEmpty() {
        return first == null;
    }

    public void append(Level level, String message, @Nullable Throwable t, Object[] args) {
        final Entry next = new Entry(level, message, t, args);
        if(isEmpty()) {
            first = next;
            last = next;
        } else {
            last.next = next;
            last = next;
        }
    }

    public void flush(IDebugContext debug) {
        while(!isEmpty()) {
            first.log(debug);
            first = first.next;
        }
    }

    public void absorb(final Log log) {
        if(!log.isEmpty()) {
            if(isEmpty()) {
                first = log.first;
                last = log.last;
            } else {
                last.next = log.first;
                last = log.last;
            }
            log.first = null;
            log.last = null;
        }
    }

    public Log clear() {
        final Log log = new Log(first, last);
        first = null;
        last = null;
        return log;
    }

    private static class Entry {

        private final Level level;
        private final String message;
        private final @Nullable Throwable t;
        private final Object[] args;
        private Entry next;

        private Entry(Level level, String message, @Nullable Throwable t, Object[] args) {
            this.level = level;
            this.message = message;
            this.t = t;
            this.args = args;
        }

        private void log(IDebugContext debug) {
            if(!debug.isEnabled(level)) {
                return;
            }
            if(t != null) {
                debug.log(level, message, t, args);
            } else {
                debug.log(level, message, args);
            }
        }

    }

}