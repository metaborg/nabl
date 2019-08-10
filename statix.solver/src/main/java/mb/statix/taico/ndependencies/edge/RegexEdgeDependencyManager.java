package mb.statix.taico.ndependencies.edge;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Multimap;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.taico.dependencies.Dependency;
import mb.statix.taico.dependencies.details.QueryDependencyDetail;
import mb.statix.taico.util.MapMultimap;
import mb.statix.taico.util.TOverrides;

public class RegexEdgeDependencyManager implements IEdgeDependencyManager<LabelWF<ITerm>>, Serializable {
    private static final long serialVersionUID = 1L;
    
    private MapMultimap<Scope, LabelWF<ITerm>, Dependency> edgeDependencies = TOverrides.mapSetMultimap();
    
    @Override
    public Collection<Dependency> getDependencies(Scope scope) {
        return edgeDependencies.get2(scope).values();
    }
    
    @Override
    public Set<Dependency> getDependencies(Scope scope, ITerm label) {
        Multimap<LabelWF<ITerm>, Dependency> nmap;
        synchronized (this) {
            nmap = edgeDependencies.get2(scope);
        }
        if (nmap.isEmpty()) return Collections.emptySet();
        
        Set<Dependency> tbr = new HashSet<>();
        for (LabelWF<ITerm> labelWf : nmap.keySet()) {
            try {
                if (!labelWf.canStep(label)) continue;
            } catch (ResolutionException | InterruptedException ex) {
                continue;
            }
            tbr.addAll(nmap.get(labelWf));
        }
        return tbr;
    }
    
    @Override
    public boolean addDependency(Scope scope, LabelWF<ITerm> label, Dependency dependency) {
        return edgeDependencies.put(scope, label, dependency);
    }
    
    @Override
    public void removeDependencies(Collection<Dependency> dependencies) {
        for (Dependency dependency : dependencies) {
            QueryDependencyDetail detail = dependency.getDetails(QueryDependencyDetail.class);
            for (Entry<Scope, LabelWF<ITerm>> entry : detail.getRelevantEdges().entries()) {
                edgeDependencies.remove(entry.getKey(), entry.getValue(), dependency);
            }
        }
    }
}
