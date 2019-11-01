package mb.statix.solver.log;

import org.metaborg.util.log.Level;

public class PrefixedDebugContext implements IDebugContext {
    private final IDebugContext parent;
    private final String square;
    private final String prefix;
    private final int depth;
    
    public PrefixedDebugContext(String square, IDebugContext parent) {
        this(square, 0, parent);
    }
    
    private PrefixedDebugContext(String square, int depth, IDebugContext parent) {
        this.parent = parent;
        this.depth = depth;
        this.square = square;
        this.prefix = prefix(square, depth);
    }
    
    /**
     * Generates a prefix string in the form of "[square] | | ...".
     * 
     * @param square
     *      the part that goes between the square brackets
     * @param depth
     *      the depth of the debug context
     * @return
     *      the prefix string
     */
    private static final String prefix(String square, int depth) {
        StringBuilder sb = new StringBuilder(square.length() + 3 + depth * 2);
        sb.append('[');
        sb.append(square);
        sb.append("] ");
        for (int i = 0; i < depth; i++) {
            sb.append("| ");
        }
        
        return sb.toString();
    }

    @Override
    public Level getLevel() {
        return parent.getLevel();
    }

    @Override
    public int getDepth() {
        return depth;
    }

    @Override
    public boolean isEnabled(Level level) {
        return parent.isEnabled(level);
    }

    @Override
    public IDebugContext subContext() {
        return new PrefixedDebugContext(square, depth + 1, parent);
    }

    @Override
    public void info(String fmt, Object... args) {
        if (!isEnabled(Level.Info)) return;
        parent._log(Level.Info, prefix + fmt, args);
    }

    @Override
    public void warn(String fmt, Object... args) {
        if (!isEnabled(Level.Warn)) return;
        parent._log(Level.Warn, prefix + fmt, args);
    }

    @Override
    public void error(String fmt, Object... args) {
        if (!isEnabled(Level.Error)) return;
        parent._log(Level.Error, prefix + fmt, args);
    }

    @Override
    public void log(Level level, String fmt, Object... args) {
        if (!isEnabled(level)) return;
        parent._log(level, prefix + fmt, args);
    }
    
    @Override
    public void _log(Level level, String fmt, Object... args) {
        parent._log(level, prefix + fmt, args);
    }

    /**
     * Creates a sibling debug context.
     * A sibling is a debug context with the same parent and depth, but with a different prefix.
     * 
     * @param square
     *      the part between the square brackets
     * @return
     *      the sibling context
     */
    public PrefixedDebugContext createSibling(String square) {
        return new PrefixedDebugContext(square, depth, parent);
    }
}
