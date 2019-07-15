package mb.statix.taico.name;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.solver.Delay;

/**
 * Immutable name like <code>Class{"MyClass"}</code>
 */
public class Name implements Serializable {
    private static final long serialVersionUID = 1L;
    
    protected final String namespace;
    protected final List<ITerm> terms;
    //private final ? position;
    
    public Name(String namespace, List<ITerm> terms) {
        this.namespace = namespace;
        this.terms = terms;
    }
    
    public String getNamespace() {
        return namespace;
    }
    
    public List<ITerm> getTerms() {
        return terms;
    }
    
    /**
     * Checks if the terms in this name are ground according to the given unifier.
     * 
     * @param unifier
     *      the unifier
     * 
     * @throws Delay
     *      If one of the terms is not ground.
     */
    public void checkGround(IUnifier unifier) throws Delay {
        for (ITerm term : terms) {
            if (!unifier.isGround(term)) throw Delay.ofVars(unifier.getVars(term));
        }
    }
    
    /**
     * Applies the given substitution to this name. If the given substitution does not affect this
     * Name, this Name is returned unchanged.
     * 
     * @param subst
     *      the substitution
     * 
     * @return
     *      the name with the given substitution applied
     */
    public Name apply(ISubstitution.Immutable subst) {
        List<ITerm> nTerms = new ArrayList<>(terms);
        
        boolean changed = false;
        ListIterator<ITerm> lit = nTerms.listIterator();
        while (lit.hasNext()) {
            ITerm term = lit.next();
            ITerm nterm = subst.apply(term);
            if (nterm != term) {
                lit.set(nterm);
                changed = true;
            }
        }
        
        return changed ? with(namespace, nTerms) : this;
    }
    
    /**
     * Grounds this name with the given unifier. If this name is already ground,
     * this method returns this name.
     * 
     * @param unifier
     *      the unifier
     * 
     * @return
     *      the ground version of this name
     * 
     * @throws Delay
     *      If this name contains variables that are not ground in the given unifier.
     */
    public Name ground(IUnifier unifier) throws Delay {
        boolean changed = false;
        List<ITerm> nterms = new ArrayList<>(terms.size());
        for (ITerm term : terms) {
            if (!unifier.isGround(term)) throw Delay.ofVars(unifier.getVars(term));
            
            ITerm nterm = unifier.findRecursive(term);
            if (!changed && nterm.equals(term)) changed = true;
            nterms.add(nterm);
        }
        
        return changed ? with(namespace, nterms) : this;
    }
    
    /**
     * Creates a new Name with the given namespace and terms.
     * This method can be overridden by extending classes, to ensure that their fields are copied
     * over.
     * 
     * @param namespace
     *      the namespace
     * @param terms
     *      the terms
     * 
     * @return
     *      the new name
     */
    protected Name with(String namespace, List<ITerm> terms) {
        return new Name(namespace, terms);
    }
    
    //---------------------------------------------------------------------------------------------
    
    @Override
    public int hashCode() {
        return Objects.hash(namespace, terms);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Name)) return false;
        
        Name other = (Name) obj;
        if (!namespace.equals(other.namespace)) return false;
        
        return terms.equals(other.terms);
    }
    
    @Override
    public String toString() {
        return namespace + "{" + terms + "}";
    }
}
