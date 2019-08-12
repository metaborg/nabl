package mb.statix.modular.dependencies.affect;

public interface IFullQueryAffected extends IDataNameAdditionAffect, IDataNameRemovalOrChangeAffect, IEdgeAdditionAffect, IEdgeRemovalAffect {
    /**
     * @return
     *      the score (lower is better) for how well this predicts the impact on queries
     */
    public int queryAffectScore();
    
    @Override
    default int edgeAdditionAffectScore() {
        return Integer.MAX_VALUE;
    }
    
    @Override
    default int edgeRemovalAffectScore() {
        return Integer.MAX_VALUE;
    }
    
    @Override
    default int dataNameAdditionAffectScore() {
        return Integer.MAX_VALUE;
    }
    
    @Override
    default int dataNameRemovalOrChangeAffectScore() {
        return Integer.MAX_VALUE;
    }
}
