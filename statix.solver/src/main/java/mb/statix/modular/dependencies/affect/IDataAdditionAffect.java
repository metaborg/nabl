package mb.statix.modular.dependencies.affect;

import java.util.Comparator;

import com.google.common.collect.Iterables;

import mb.nabl2.terms.ITerm;
import mb.statix.modular.dependencies.Dependency;
import mb.statix.scopegraph.terms.Scope;

public interface IDataAdditionAffect {
    public static final Comparator<IDataAdditionAffect> COMPARATOR = (a, b) -> -Integer.compare(a.dataAdditionAffectScore(), b.dataAdditionAffectScore());
    
    /**
     * @param scope
     *      the (source) scope of the data edge
     * @param relation
     *      the relation of the data
     * 
     * @return
     *      the dependencies that can be affected by the addition of the given data
     */
    public Iterable<Dependency> affectedByDataAddition(Scope scope, ITerm relation);
    
    /**
     * @return
     *      the score (lower is better) for how well this predicts the impact of data addition
     */
    public int dataAdditionAffectScore();
    
    /**
     * @param other
     *      the other {@link IDataAdditionAffect} to be composed with
     * 
     * @return
     *      a new {@link IDataAdditionAffect} composed of this affect and the given one 
     */
    default IDataAdditionAffect compose(IDataAdditionAffect other) {
        return new IDataAdditionAffect() {
            
            @Override
            public int dataAdditionAffectScore() {
                return IDataAdditionAffect.this.dataAdditionAffectScore();
            }
            
            @Override
            public Iterable<Dependency> affectedByDataAddition(Scope scope, ITerm relation) {
                return Iterables.concat(
                        IDataAdditionAffect.this.affectedByDataAddition(scope, relation),
                        other.affectedByDataAddition(scope, relation));
            }
        };
    }
}
