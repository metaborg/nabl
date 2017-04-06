package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.metaborg.meta.nabl2.scopegraph.IScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.meta.nabl2.util.collections.IRelation3;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.Lists;

public abstract class ScopeGraphEdgePrimitive<S extends ITerm> extends ScopeGraphPrimitive {

    public ScopeGraphEdgePrimitive(String name) {
        super(name, 0, 1);
    }

    @Override public Optional<ITerm> call(IScopeGraphContext<?> context, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        if(terms.size() != 1) {
            throw new InterpreterException("Need one term argument: analysis");
        }
        return TermIndex.get(terms.get(0)).flatMap(index -> {
            return context.unit(index.getResource()).solution().flatMap(sol -> {
                final IRelation3<S, Label, ? extends ITerm> edges = getEdges(sol.getScopeGraph());
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
                ).match(term);
            });
        });
    }

    protected abstract IMatcher<S> getSourceMatcher();

    protected abstract IRelation3<S,Label,? extends ITerm> getEdges(IScopeGraph<Scope, Label, Occurrence> scopeGraph);
    
}