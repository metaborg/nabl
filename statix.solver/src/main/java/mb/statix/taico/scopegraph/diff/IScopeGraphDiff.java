package mb.statix.taico.scopegraph.diff;

import static mb.statix.taico.util.TPrettyPrinter.prettyPrint;

import java.io.PrintStream;
import java.util.Set;

import com.google.common.collect.Sets;

import mb.nabl2.util.collections.IRelation3;
import mb.statix.taico.name.Name;

/**
 * Interface to represent a diff of scope graphs
 *
 * @param <S>
 *      the type of scopes
 * @param <L>
 *      the type of labels
 * @param <D>
 *      the type of data
 */
public interface IScopeGraphDiff<S extends D, L, D> {
    // --------------------------------------------------------------------------------------------
    // General
    // --------------------------------------------------------------------------------------------
    
    /**
     * @return
     *      if this diff is empty (represents no changes)
     */
    default boolean isEmpty() {
        return getAddedScopes().isEmpty() && getRemovedScopes().isEmpty()
                && getAddedData().isEmpty() && getRemovedData().isEmpty()
                && getAddedEdges().isEmpty() && getRemovedEdges().isEmpty();
    }
    
    // --------------------------------------------------------------------------------------------
    // Scopes
    // --------------------------------------------------------------------------------------------
    
    /**
     * @return
     *      a set of all scopes that have been added
     */
    Set<S> getAddedScopes();
    
    /**
     * @return
     *      a set of all scopes that have been removed
     */
    Set<S> getRemovedScopes();
    
    // --------------------------------------------------------------------------------------------
    // Edges
    // --------------------------------------------------------------------------------------------
    
    /**
     * @return
     *      all the edges that were added
     */
    IRelation3<S, L, S> getAddedEdges();
    
    /**
     * @return
     *      all the edges that were removed
     */
    IRelation3<S, L, S> getRemovedEdges();
    
    // --------------------------------------------------------------------------------------------
    // Data
    // --------------------------------------------------------------------------------------------
    
    /**
     * @return
     *      all the data that has been added
     */
    IRelation3<S, L, D> getAddedData();
    
    /**
     * @return
     *      all the data that has been removed
     */
    IRelation3<S, L, D> getRemovedData();
    
    /**
     * @return
     *      all the names that have been added (new sort with relation)
     */
    IRelation3<S, L, Name> getAddedDataNames();
    
    /**
     * @return
     *      all the names that have been removed (all relations for sort removed)
     */
    IRelation3<S, L, Name> getRemovedDataNames();
    
    /**
     * @return
     *      all the names which have changed
     */
    IRelation3<S, L, Name> getChangedDataNames();
    
    // --------------------------------------------------------------------------------------------
    // Convenience methods
    // --------------------------------------------------------------------------------------------
    
    /**
     * @return
     *      a set of all names that are affected by changes
     */
    default Set<Name> getAffectedNames() {
        return Sets.union(getAddedDataNames().valueSet(),
                Sets.union(getRemovedDataNames().valueSet(), getChangedDataNames().valueSet()));
    }
    
    
    /**
     * Prints this diff to the given stream. Please note that this method can lock the given stream
     * for quite a while.
     * 
     * @param stream
     *      the stream to print to
     * @param indent
     *      the indentation before the bar
     */
    public default void print(PrintStream stream, int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append("| ");
        }
        String base = sb.toString();
        synchronized (stream) {
            stream.print(base);
            stream.print("addedScopes=");
            stream.println(prettyPrint(getAddedScopes()));
            stream.print(base);
            stream.print("removedScopes=");
            stream.println(prettyPrint(getRemovedScopes()));
            
            stream.print(base);
            stream.print("addedEdges=");
            stream.println(prettyPrint(getAddedEdges()));
            stream.print(base);
            stream.print("removedEdges=");
            stream.println(prettyPrint(getRemovedEdges()));
            
            stream.print(base);
            stream.print("addedData=");
            stream.println(prettyPrint(getAddedData()));
            stream.print(base);
            stream.print("removedData=");
            stream.println(prettyPrint(getRemovedData()));
            
            stream.print(base);
            stream.print("addedDataNames=");
            stream.println(prettyPrint(getAddedDataNames()));
            stream.print(base);
            stream.print("removedDataNames=");
            stream.println(prettyPrint(getRemovedDataNames()));
            stream.print(base);
            stream.print("changedDataNames=");
            stream.println(prettyPrint(getChangedDataNames()));
        }
    }
    
    public default String print() {
        return "IScopeGraphDiff [addedScopes=" + prettyPrint(getAddedScopes())
        + ",\n removedScopes=" + prettyPrint(getRemovedScopes())
        + ",\n addedEdges=" + prettyPrint(getAddedEdges())
        + ",\n removedEdges=" + prettyPrint(getRemovedEdges())
        + ",\n addedData=" + prettyPrint(getAddedData())
        + ",\n removedData=" + prettyPrint(getRemovedData())
        + ",\n addedDataNames=" + prettyPrint(getAddedDataNames())
        + ",\n removedDataNames=" + prettyPrint(getRemovedDataNames())
        + ",\n changedDataNames=" + prettyPrint(getChangedDataNames()) + "]";
    }
}
