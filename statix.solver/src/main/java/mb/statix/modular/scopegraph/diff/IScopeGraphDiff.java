package mb.statix.modular.scopegraph.diff;

import static mb.statix.modular.util.TPrettyPrinter.*;

import java.io.PrintStream;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.collect.Sets;

import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.modular.name.Name;
import mb.statix.modular.solver.Context;

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
    // Other
    // --------------------------------------------------------------------------------------------
    
    /**
     * Creates an effective diff for this diff in the given diff result.
     * 
     * @param target
     *      the diff result to append to
     */
    void toEffectiveDiff(DiffResult target);
    
    /**
     * Creates a new ScopeGraphDiff which only has the addedScopes and removedScopes retained.
     * 
     * @return
     *      the new diff
     */
    ScopeGraphDiff retainScopes();
    
    /**
     * Creates a scope graph diff which is the inverse of this diff. That is, added items become
     * removed items and vice versa.
     * 
     * @return
     *      the new inverse diff
     */
    IScopeGraphDiff<S, L, D> inverse();
    
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
        IUnifier unifier = PersistentUnifier.Immutable.of();
        Context context = Context.context();
        print(stream, indent, unifier, context, unifier, context);
    }
    
    /**
     * Prints this diff to the given stream. Please note that this method can lock the given stream
     * for quite a while.
     * 
     * @param stream
     *      the stream to print to
     * @param indent
     *      the indentation before the bar
     * @param aUnifier
     *      the unifier for added elements
     * @param aContext
     *      the context for the added unifier
     * @param rUnifier
     *      the unifier for removed elements
     * @param rContext
     *      the context for removed unifier
     */
    public default void print(PrintStream stream, int indent, IUnifier aUnifier, @Nullable Context aContext, IUnifier rUnifier, @Nullable Context rContext) {
        if (aContext == null) aContext = Context.context();
        if (rContext == null) rContext = aContext;
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append("| ");
        }
        final String base = sb.toString();
        
        fixScopeNumbers();
        final String as = prettyPrint(getAddedScopes(), aUnifier, aContext);
        final String rs = prettyPrint(getRemovedScopes(), rUnifier, rContext);
        final String ae = prettyPrint(getAddedEdges(), aUnifier, aContext);
        final String re = prettyPrint(getRemovedEdges(), rUnifier, rContext);
        final String ad = prettyPrint(getAddedData(), aUnifier, aContext);
        final String rd = prettyPrint(getRemovedData(), rUnifier, rContext);
        final String adn = prettyPrint(getAddedDataNames(), aUnifier, aContext);
        final String rdn = prettyPrint(getRemovedDataNames(), rUnifier, rContext);
        final String cdn = prettyPrint(getChangedDataNames(), aUnifier, aContext);
        unfixScopeNumbers();
        
        //Print as one large block
        synchronized (stream) {
            stream.print(base);
            stream.print("addedScopes=");
            stream.println(as);
            stream.print(base);
            stream.print("removedScopes=");
            stream.println(rs);
            
            stream.print(base);
            stream.print("addedEdges=");
            stream.println(ae);
            stream.print(base);
            stream.print("removedEdges=");
            stream.println(re);
            
            stream.print(base);
            stream.print("addedData=");
            stream.println(ad);
            stream.print(base);
            stream.print("removedData=");
            stream.println(rd);
            
            stream.print(base);
            stream.print("addedDataNames=");
            stream.println(adn);
            stream.print(base);
            stream.print("removedDataNames=");
            stream.println(rdn);
            stream.print(base);
            stream.print("changedDataNames=");
            stream.println(cdn);
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
    
    public default String print(IUnifier aUnifier, Context cAdded, IUnifier rUnifier, Context cRemoved) {
        return "IScopeGraphDiff [addedScopes=" + prettyPrint(getAddedScopes(), aUnifier, cAdded)
        + ",\n removedScopes=" + prettyPrint(getRemovedScopes(), rUnifier, cRemoved)
        + ",\n addedEdges=" + prettyPrint(getAddedEdges(), aUnifier, cAdded)
        + ",\n removedEdges=" + prettyPrint(getRemovedEdges(), rUnifier, cRemoved)
        + ",\n addedData=" + prettyPrint(getAddedData(), aUnifier, cAdded)
        + ",\n removedData=" + prettyPrint(getRemovedData(), rUnifier, cRemoved)
        + ",\n addedDataNames=" + prettyPrint(getAddedDataNames(), aUnifier, cAdded)
        + ",\n removedDataNames=" + prettyPrint(getRemovedDataNames(), rUnifier, cRemoved)
        + ",\n changedDataNames=" + prettyPrint(getChangedDataNames(), aUnifier, cAdded) + "]";
    }
}
