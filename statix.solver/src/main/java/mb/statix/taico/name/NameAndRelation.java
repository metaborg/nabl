package mb.statix.taico.name;

import java.util.List;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution.Immutable;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.solver.Delay;

public class NameAndRelation extends Name {
    private static final long serialVersionUID = 1L;
    
    protected final ITerm relation;
    
    public NameAndRelation(String namespace, List<ITerm> terms, ITerm relation) {
        super(namespace, terms);
        this.relation = relation;
    }

    public ITerm getRelation() {
        return relation;
    }
    
    //---------------------------------------------------------------------------------------------
    
    @Override
    public NameAndRelation apply(Immutable subst) {
        return (NameAndRelation) super.apply(subst);
    }
    
    @Override
    public NameAndRelation ground(IUnifier unifier) throws Delay {
        return (NameAndRelation) super.ground(unifier);
    }
    
    @Override
    protected NameAndRelation with(String namespace, List<ITerm> terms) {
        return new NameAndRelation(namespace, terms, relation);
    }
    
    /**
     * @return
     *      a name representing this NameAndRelation without the relation
     */
    public Name toName() {
        return new Name(namespace, terms);
    }
    
    // --------------------------------------------------------------------------------------------
    // Object methods
    // --------------------------------------------------------------------------------------------
    
    @Override
    public int hashCode() {
        if (hashCode != 0) return hashCode;
        int result = 1;
        result = 31 * result + namespace.hashCode();
        result = 31 * result + terms.hashCode();
        result = 31 * result + relation.hashCode();
        if (result == 0) result = 1;
        return hashCode = result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof NameAndRelation)) return false;
        
        NameAndRelation other = (NameAndRelation) obj;
        if (!namespace.equals(other.namespace)) return false;
        if (!relation.equals(other.relation)) return false;
        
        return terms.equals(other.terms);
    }
    
    @Override
    public String toString() {
        return relation + ": " + super.toString();
    }
}
