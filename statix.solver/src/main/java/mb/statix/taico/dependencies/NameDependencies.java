package mb.statix.taico.dependencies;

import java.util.Map.Entry;
import java.util.Set;

import mb.nabl2.terms.ITerm;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.taico.dependencies.details.IDependencyDetail;
import mb.statix.taico.dependencies.details.NameDependencyDetail;
import mb.statix.taico.dependencies.details.QueryDependencyDetail;
import mb.statix.taico.name.Name;
import mb.statix.taico.name.NameAndRelation;

public class NameDependencies extends Dependencies {
    private static final long serialVersionUID = 1L;
    
    //mapping from name -> scope -> edge -> dependency
    /**
     * A mapping from name + relation -> scope -> dependency.
     * The owner of this dependencies object depends on the names in this table because of the
     * dependencies in this table.
     */
    private IRelation3.Transient<NameAndRelation, Scope, Dependency> table = HashTrieRelation3.Transient.of();

    public NameDependencies(String owner) {
        super(owner);
    }

    public Set<? extends Entry<Scope, Dependency>> getByName(NameAndRelation name) {
        return table.get(name);
    }
    
    /**
     * Convenience method.
     * 
     * @param name
     *      the name
     * @param relation
     *      the relation
     * 
     * @return
     *      all the entries corresponding to the given name
     */
    public final Set<? extends Entry<Scope, Dependency>> getByName(Name name, ITerm relation) {
        return getByName(name.withRelation(relation));
    }
    
    public Set<Dependency> get(NameAndRelation name, Scope scope) {
        return table.get(name, scope);
    }
    
    /**
     * Convenience method.
     * 
     * @param name
     *      the name
     * @param relation
     *      the relation
     * @param scope
     *      the scope
     * 
     * @return
     *      the dependencies for the given name, relation and scope
     */
    public final Set<Dependency> get(Name name, ITerm relation, Scope scope) {
        return get(name.withRelation(relation), scope);
    }
    
    public Set<Dependency> values() {
        return table.valueSet();
    }
    
    public Set<NameAndRelation> names() {
        return table.keySet();
    }
    
    @Override
    public Dependency addDependency(String module, IDependencyDetail... details) {
        Dependency dependency = super.addDependency(module, details);
        
        NameDependencyDetail ndetail = dependency.getDetails(NameDependencyDetail.class);
        QueryDependencyDetail qdetail = dependency.getDetails(QueryDependencyDetail.class);
        final NameAndRelation nar = ndetail.toNameAndRelation();
        
        //TODO maybe we don't want to do this for every data scope, but rather find a different way of storing things
        for (Scope scope : qdetail.getDataScopes()) {
            table.put(nar, scope, dependency);
        }
        
        return dependency;
    }
}
