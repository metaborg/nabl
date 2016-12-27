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
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class SG_get_scope_assocs extends ScopeGraphPrimitive {

    public SG_get_scope_assocs() {
        super(SG_get_scope_assocs.class.getSimpleName(), 0, 1);
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
                Multimap<Label,Occurrence> assocs = s.getScopeGraph().getAssocDecls(scope);
                List<ITerm> assocTerms = Lists.newArrayList();
                for (Map.Entry<Label,Occurrence> assoc : assocs.entries()) {
                    assocTerms.add(GenericTerms.newTuple(assoc.getKey(), assoc.getValue()));
                }
                return GenericTerms.newList(assocTerms);
            });
        });
    }

}