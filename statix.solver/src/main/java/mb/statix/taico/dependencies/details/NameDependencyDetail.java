package mb.statix.taico.dependencies.details;

import org.checkerframework.checker.nullness.qual.Nullable;

import mb.nabl2.terms.ITerm;
import mb.statix.taico.name.Name;
import mb.statix.taico.name.NameAndRelation;

/**
 * Class to represent a the dependency detail of a name and an optional relation.
 */
public class NameDependencyDetail implements IDependencyDetail {
    private static final long serialVersionUID = 1L;
    
    private final Name name;
    private final @Nullable ITerm relation;
    
    public NameDependencyDetail(Name name) {
        this(name, null);
    }
    
    public NameDependencyDetail(NameAndRelation name) {
        this(name, name.getRelation());
    }
    
    public NameDependencyDetail(Name name, @Nullable ITerm relation) {
        this.name = name;
        this.relation = relation;
    }
    
    /**
     * @return
     *      the name
     */
    public Name getName() {
        return name;
    }
    
    /**
     * @return
     *      the relation, if known
     */
    public @Nullable ITerm getRelation() {
        return relation;
    }
    
    /**
     * @return
     *      a NameAndRelation
     * 
     * @throws IllegalStateException
     *      If this name dependency does not have an associated relation.
     */
    public NameAndRelation toNameAndRelation() {
        if (relation == null) throw new IllegalStateException("This name does not have an associated relation");
        if (name instanceof NameAndRelation) return (NameAndRelation) name;
        return name.withRelation(relation);
    }
}
