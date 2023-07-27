package mb.nabl2.log;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

public class Logger {

    private static final ILogger log = LoggerUtils.logger(Logger.class);

    private final String name;
    private final boolean enabled;

    public Logger(String name) {
        this.name = truncate(name);
        enabled = !name.startsWith("mb.nabl2.solver.components");
    }

    public void debug(String message, Object... args) {
        log(format(message, args), "[DEBUG] -");
    }

    public void debug(String message, Throwable ex, Object... args) {
        log(format(message, args), "[DEBUG] -");
        ex.printStackTrace(System.out);
    }

    public void info(String message, Object... args) {
        log(format(message, args), "[INFO]  -");
    }

    public void info(String message, Throwable ex, Object... args) {
        log(format(message, args), "[INFO]  -");
        ex.printStackTrace(System.out);
    }

    public void warn(String message, Object... args) {
        log(format(message, args), "[WARN]  -");
    }

    public void warn(String message, Throwable ex, Object... args) {
        log(format(message, args), "[WARN]  -");
        ex.printStackTrace(System.out);
    }

    public void error(String message, Object... args) {
        log(format(message, args), "[ERROR] -");
    }

    public void error(String message, Throwable ex, Object... args) {
        log(format(message, args), "[ERROR] -");
        ex.printStackTrace(System.out);
    }

    private String format(String format, Object... args) {
        return log.format(format, args);
    }

    private void log(String message, String level) {
        if(!enabled) {
            return;
        }
        System.out.printf("%s %-32s | %s%n", level, name, message);
    }

    private String truncate(String name) {
        if(name.length() > 32) {
            return name.substring(name.length() - 32);
        } else {
            return name;
        }
    }

    public static Logger logger(Class<?> clz) {
        return new Logger(clz.getCanonicalName());
    }

}
