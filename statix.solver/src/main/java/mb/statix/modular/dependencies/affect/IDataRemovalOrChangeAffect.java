package mb.statix.modular.dependencies.affect;

import mb.statix.modular.dependencies.Dependency;
import mb.statix.modular.name.NameAndRelation;
import mb.statix.scopegraph.terms.Scope;

public interface IDataRemovalOrChangeAffect {
    /**
     * @param nameAndRelation
     *      the name and the relation
     * @param scope
     *      the scope
     * 
     * @return
     *      the dependencies that can be affected by the removal/change of the given data
     */
    public Iterable<Dependency> affectedByDataRemovalOrChange(NameAndRelation nameAndRelation, Scope scope);
    
    /**
     * @return
     *      the score (lower is better) for how well this predicts the impact of data removal
     */
    public int dataRemovalOrChangeAffectScore();
}
