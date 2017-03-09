package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.GenericTerms;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.Lists;

public class SG_get_scope_direct_edges extends ScopeGraphPrimitive {

    public SG_get_scope_direct_edges() {
        super(SG_get_scope_direct_edges.class.getSimpleName(), 0, 1);
    }

    @Override public Optional<ITerm> call(IScopeGraphContext<?> context, ITerm term, List<ITerm> terms)
        throws InterpreterException {
        if(terms.size() != 1) {
            throw new InterpreterException("Need one term argument: analysis");
        }
        return TermIndex.get(terms.get(0)).flatMap(index -> {
            return Scope.matcher().match(term).<ITerm>flatMap(scope -> {
                return context.unit(index.getResource()).solution().<ITerm>map(s -> {
                    List<ITerm> edgeTerms = Lists.newArrayList();
                    for(Map.Entry<Label, Scope> edge : s.getScopeGraph().getDirectEdges().get(scope)) {
                        edgeTerms.add(GenericTerms.newTuple(edge.getKey(), edge.getValue()));
                    }
                    return GenericTerms.newList(edgeTerms);
                });
            });
        });
    }

}