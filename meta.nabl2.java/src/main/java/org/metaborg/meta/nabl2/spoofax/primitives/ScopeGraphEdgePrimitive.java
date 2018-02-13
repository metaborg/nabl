package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.metaborg.meta.nabl2.scopegraph.IScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphUnit;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.build.TB;
import org.metaborg.meta.nabl2.terms.matching.Match.IMatcher;
import org.metaborg.meta.nabl2.terms.matching.Match.M;
import org.metaborg.meta.nabl2.util.collections.IRelation3;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.Lists;

public abstract class ScopeGraphEdgePrimitive<S extends ITerm> extends AnalysisPrimitive {

    public ScopeGraphEdgePrimitive(String name) {
        super(name);
    }

    @Override public Optional<ITerm> call(IScopeGraphUnit unit, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        return unit.solution().flatMap(sol -> {
            final IRelation3<S, Label, ? extends ITerm> edges = getEdges(sol.scopeGraph());
            final IMatcher<S> sourceMatcher = getSourceMatcher();
            return M.<ITerm>cases(
            // @formatter:off
                M.term(sourceMatcher, (t, source) -> {
                    List<ITerm> edgeTerms = Lists.newArrayList();
                    for(Map.Entry<Label, ? extends ITerm> edge : edges.get(source)) {
                        edgeTerms.add(TB.newTuple(edge.getKey(), edge.getValue()));
                    }
                    return TB.newList(edgeTerms);
                }),
                M.tuple2(sourceMatcher, Label.matcher(), (t, source, label) -> {
                    List<ITerm> targetTerms = Lists.newArrayList();
                    for(ITerm target : edges.get(source, label)) {
                        targetTerms.add(target);
                    }
                    return TB.newList(targetTerms);
                })
                // @formatter:on
            ).match(term, sol.unifier());
        });
    }

    protected abstract IMatcher<S> getSourceMatcher();

    protected abstract IRelation3<S, Label, ? extends ITerm> getEdges(IScopeGraph<Scope, Label, Occurrence> scopeGraph);

}