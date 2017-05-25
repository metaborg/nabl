package org.metaborg.meta.nabl2.scopegraph.esop.persistent;

import static org.metaborg.meta.nabl2.scopegraph.esop.persistent.IBiSimulation.biSimulate;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.OpenCounter;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.util.collections.IFunction;
import org.metaborg.meta.nabl2.util.collections.IRelation3;
import org.metaborg.meta.nabl2.util.functions.Function1;

import io.usethesource.capsule.Set;

public class BiSimulationScopeGraph<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements IEsopScopeGraph<S, L, O>, IBiSimulation, java.io.Serializable {
    
    private static final long serialVersionUID = 42L;

    private final IEsopScopeGraph<S, L, O> one;
    private final IEsopScopeGraph<S, L, O> two;

    public BiSimulationScopeGraph(final IEsopScopeGraph<S, L, O> one, final IEsopScopeGraph<S, L, O> two) {
        this.one = one;
        this.two = two;
    }   
    
    @Override
    public Set.Immutable<S> getAllScopes() {
        return biSimulate(one::getAllScopes, two::getAllScopes);
    }

    @Override
    public Set.Immutable<O> getAllDecls() {
        return biSimulate(one::getAllDecls, two::getAllDecls);
    }

    @Override
    public Set.Immutable<O> getAllRefs() {
        return biSimulate(one::getAllRefs, two::getAllRefs);

    }

    @Override
    public IFunction<O, S> getDecls() {
        return biSimulate(one::getDecls, two::getDecls);

    }

    @Override
    public IFunction<O, S> getRefs() {
        return biSimulate(one::getRefs, two::getRefs);

    }

    @Override
    public IRelation3<S, L, S> getDirectEdges() {
        return biSimulate(one::getDirectEdges, two::getDirectEdges);
    }

    @Override
    public IRelation3<O, L, S> getExportEdges() {
        return biSimulate(one::getExportEdges, two::getExportEdges);
    }

    @Override
    public IRelation3<S, L, O> getImportEdges() {
        return biSimulate(one::getImportEdges, two::getImportEdges);
    }

    @Override
    public IEsopNameResolution<S, L, O> resolve(IResolutionParameters<L> params, OpenCounter<S, L> scopeCounter,
            Function1<S, String> tracer) {
        return new BiSimulationNameResolution<>(one.resolve(params, scopeCounter, tracer),
                two.resolve(params, scopeCounter, tracer));
    }

    

}
