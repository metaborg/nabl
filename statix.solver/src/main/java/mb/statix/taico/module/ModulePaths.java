package mb.statix.taico.module;

import java.util.regex.Pattern;

public class ModulePaths {
    public static final char PATH_SEPARATOR_CHAR = '%';
    public static final String PATH_SEPARATOR = String.valueOf(PATH_SEPARATOR_CHAR);
    public static final char REPLACEMENT_CHAR = '_';
    
    /**
     * Builds the path with the given path and name. The name is sanitized.
     * 
     * @param path
     *      the path so far
     * @param name
     *      the name of the module
     * 
     * @return
     *      the path
     */
    public static String build(String path, String name) {
        if (path.isEmpty()) return sanitize(name);
        return path + PATH_SEPARATOR + sanitize(name);
    }
    
    /**
     * Replaces all occurrences of {@value #PATH_SEPARATOR_CHAR} with {@value #REPLACEMENT_CHAR}.
     * 
     * @param name
     *      the name to sanitize
     * 
     * @return
     *      the sanitized name
     */
    public static String sanitize(String name) {
        return name.replace(PATH_SEPARATOR_CHAR, REPLACEMENT_CHAR);
    }
    
    /**
     * Computes the number of segments in the given path.
     * 
     * E.g.
     * <code>
     * Top{@value #PATH_SEPARATOR_CHAR}ModA{@value #PATH_SEPARATOR_CHAR}ModA1
     * </code>
     * has a path length of 3.
     * 
     * @param path
     *      the path
     * 
     * @return
     *      the number of segments in the path
     */
    public static int pathLength(String path) {
        return pathSegments(path).length;
    }
    
    /**
     * Splits a path into the individual components.
     * 
     * @param path
     *      the path
     * 
     * @return
     *      the path segments
     */
    public static String[] pathSegments(String path) {
        return path.split(Pattern.quote(PATH_SEPARATOR));
    }
    
    /**
     * Splits a path into the individual components.
     * 
     * @param path
     *      the path
     * @param limit
     *      the maximum number of segments
     * 
     * @return
     *      the path segments
     * 
     * @see String#split(String, int)
     */
    public static String[] pathSegments(String path, int limit) {
        return path.split(Pattern.quote(PATH_SEPARATOR), limit);
    }
}
