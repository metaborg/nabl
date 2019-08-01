package mb.statix.taico.dependencies.details;

import org.checkerframework.checker.nullness.qual.Nullable;

import mb.nabl2.terms.ITerm;
import mb.statix.taico.name.Name;

/**
 * Class to represent a the dependency detail of a name and an optional relation.
 */
public class NameDependencyDetail implements IDependencyDetail {
    private static final long serialVersionUID = 1L;
    
    private final Name name;
    private final ITerm relation;
    
    public NameDependencyDetail(Name name) {
        this(name, null);
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
}
