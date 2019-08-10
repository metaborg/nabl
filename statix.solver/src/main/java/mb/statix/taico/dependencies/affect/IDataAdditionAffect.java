package mb.statix.taico.dependencies.affect;

import mb.statix.scopegraph.terms.Scope;
import mb.statix.taico.dependencies.Dependency;
import mb.statix.taico.name.NameAndRelation;

public interface IDataAdditionAffect {
    /**
     * @param nameAndRelation
     *      the name and the relation
     * @param scope
     *      the scope
     * 
     * @return
     *      the dependencies that can be affected by the addition of the given data
     */
    public Iterable<Dependency> affectedByDataAddition(NameAndRelation nameAndRelation, Scope scope);
    
    /**
     * @return
     *      the score (lower is better) for how well this predicts the impact of data addition
     */
    public int dataAdditionAffectScore();
}
