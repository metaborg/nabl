package mb.statix.taico.scopegraph.reference;

import java.util.Set;

import org.metaborg.util.functions.Predicate2;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.FastNameResolution;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;

public class TrackingNameResolution<S extends D, L, D> extends FastNameResolution<S, L, D> {
    private SetMultimap<S, LabelWF<L>> edgeMap = HashMultimap.create();
    private SetMultimap<S, LabelWF<L>> dataMap = HashMultimap.create();
    
    //if scope x changed, redo the queries in it.
    
    //Split a module into two parts, one part that is context free and one part that has context

    public TrackingNameResolution(IScopeGraph<S, L, D> scopeGraph, L relation, LabelWF<L> labelWF,
            LabelOrder<L> labelOrder, Predicate2<S, L> isEdgeComplete, DataWF<D> dataWF, DataLeq<D> dataEquiv,
            Predicate2<S, L> isDataComplete) {
        super(scopeGraph, relation, labelWF, labelOrder, isEdgeComplete, dataWF, dataEquiv, isDataComplete);
    }
    
    @Override
    protected Set<D> getData(LabelWF<L> re, IScopePath<S, L> path, L l) {
        S scope = path.getTarget(); //the current scope
        dataMap.put(scope, re);
        
        System.out.println("Query hit scope " + scope + ", derivative query=" + re + ", data edge requested=" + l);
        return super.getData(re, path, l);
    }
    
    @Override
    protected Set<S> getEdges(LabelWF<L> re, IScopePath<S, L> path, L l) {
        S scope = path.getTarget(); //the current scope
        edgeMap.put(scope, re);

        System.out.println("Query hit scope " + scope + ", derivative query=" + re + ", edge requested=" + l);
        return super.getEdges(re, path, l);
    }
    
    public SetMultimap<S, LabelWF<L>> getTrackedEdges() {
        return edgeMap;
    }
    
    public SetMultimap<S, LabelWF<L>> getTrackedData() {
        return dataMap;
    }
    
    public DataWF<D> getDataWf() {
        return dataWF;
    }
    
    public static <S extends D, L, D> Builder<S, L, D> builder() {
        return new Builder<>();
    }

    public static class Builder<S extends D, L, D> extends FastNameResolution.Builder<S, L, D> {

        @Override
        public Builder<S, L, D> withLabelWF(LabelWF<L> labelWF) {
            this.labelWF = labelWF;
            return this;
        }

        @Override
        public Builder<S, L, D> withLabelOrder(LabelOrder<L> labelOrder) {
            this.labelOrder = labelOrder;
            return this;
        }

        @Override
        public Builder<S, L, D> withEdgeComplete(Predicate2<S, L> isEdgeComplete) {
            this.isEdgeComplete = isEdgeComplete;
            return this;
        }

        @Override
        public Builder<S, L, D> withDataWF(DataWF<D> dataWF) {
            this.dataWF = dataWF;
            return this;
        }

        @Override
        public Builder<S, L, D> withDataEquiv(DataLeq<D> dataEquiv) {
            this.dataEquiv = dataEquiv;
            return this;
        }

        @Override
        public Builder<S, L, D> withDataComplete(Predicate2<S, L> isDataComplete) {
            this.isDataComplete = isDataComplete;
            return this;
        }
        
        @Override
        public TrackingNameResolution<S, L, D> build(IScopeGraph<S, L, D> scopeGraph, L relation) {
            return new TrackingNameResolution<>(scopeGraph, relation, labelWF, labelOrder, isEdgeComplete, dataWF,
                    dataEquiv, isDataComplete);
        }

    }
}