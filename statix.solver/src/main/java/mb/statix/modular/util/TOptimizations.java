package mb.statix.modular.util;

public class TOptimizations {
    /**
     * If true, queries track scopes only whenever the scope belongs to a different module than
     * the one executing the query.
     */
    public static final boolean QUERY_TRACK_ONLY_OTHER_SCOPES = true;
    
    /**
     * If true, the observer mechanism is used for own critical edges.
     * Otherwise, the"just redo whenever the critical edge MIGHT have changed" variant is used. 
     */
    public static boolean USE_OBSERVER_MECHANISM_FOR_SELF = true;
    
//    public static boolean SCOPE_HASHES = true;
}
