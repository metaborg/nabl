package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.GenericTerms;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.Lists;

public class SG_get_decl_assocs extends ScopeGraphPrimitive {

    public SG_get_decl_assocs() {
        super(SG_get_decl_assocs.class.getSimpleName(), 0, 1);
    }

    @Override public Optional<ITerm> call(IScopeGraphContext<?> context, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        if (terms.size() != 1) {
            throw new InterpreterException("Need one term argument: analysis");
        }
        return TermIndex.get(terms.get(0)).flatMap(index -> {
            return Occurrence.matcher().match(term).<ITerm> flatMap(decl -> {
                return context.unit(index.getResource()).solution().<ITerm> map(s -> {
                    List<ITerm> assocTerms = Lists.newArrayList();
                    for (Map.Entry<Label,Scope> assoc : s.getScopeGraph().getAssocEdges().get(decl)) {
                        assocTerms.add(GenericTerms.newTuple(assoc.getKey(), assoc.getValue()));
                    }
                    return GenericTerms.newList(assocTerms);
                });
            });
        });
    }

}