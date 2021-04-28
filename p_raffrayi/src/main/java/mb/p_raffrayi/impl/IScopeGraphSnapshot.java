package mb.p_raffrayi.impl;

import org.immutables.value.Value;

import com.google.common.collect.Multimap;

import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.reference.EdgeOrData;

@Value.Immutable
public interface IScopeGraphSnapshot<S, L, D> {

    @Value.Parameter IScopeGraph.Immutable<S, L, D> scopeGraph();

    @Value.Parameter Multimap<S, EdgeOrData<L>> openEdges();

}
