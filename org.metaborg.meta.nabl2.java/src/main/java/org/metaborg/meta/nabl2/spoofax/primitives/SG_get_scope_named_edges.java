package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.GenericTerms;
import org.metaborg.meta.nabl2.terms.generic.TermIndex;
import org.metaborg.meta.nabl2.util.functions.PartialFunction0;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class SG_get_scope_named_edges extends ScopeGraphPrimitive {

    public SG_get_scope_named_edges() {
        super(SG_get_scope_named_edges.class.getSimpleName(), 0, 1);
    }

    @Override public Optional<ITerm> call(IScopeGraphContext<?> context, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        if (terms.size() != 1) {
            throw new InterpreterException("Need one term argument: analysis");
        }
        TermIndex index = terms.get(0).getAttachments().getInstance(TermIndex.class);
        if (index == null) {
            return Optional.empty();
        }
        return Scope.matcher().match(term).<ITerm> flatMap(scope -> {
            return context.unit(index.getResource()).solution().<ITerm> map(s -> {
                Multimap<Label,PartialFunction0<Occurrence>> edges = s.getScopeGraph().getImportRefs(scope);
                List<ITerm> edgeTerms = Lists.newArrayList();
                for (Map.Entry<Label,PartialFunction0<Occurrence>> edge : edges.entries()) {
                    edge.getValue().apply().ifPresent(ref -> {
                        edgeTerms.add(GenericTerms.newTuple(edge.getKey(), ref));
                    });
                }
                return GenericTerms.newList(edgeTerms);
            });
        });
    }

}