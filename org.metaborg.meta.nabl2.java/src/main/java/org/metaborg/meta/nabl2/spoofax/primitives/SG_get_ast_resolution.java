package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Paths;
import org.metaborg.meta.nabl2.spoofax.IScopeGraphContext;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.GenericTerms;
import org.metaborg.meta.nabl2.terms.generic.TermIndex;
import org.metaborg.util.iterators.Iterables2;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.Lists;

public class SG_get_ast_resolution extends ScopeGraphPrimitive {

    public SG_get_ast_resolution() {
        super(SG_get_ast_resolution.class.getSimpleName(), 0, 0);
    }

    @Override public Optional<ITerm> call(IScopeGraphContext<?> context, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        TermIndex index = term.getAttachments().getInstance(TermIndex.class);
        if (index == null) {
            return Optional.empty();
        }
        return context.unit(index.getResource()).solution().<ITerm> flatMap(s -> {
            List<ITerm> entries = Lists.newArrayList();
            for (Occurrence ref : s.getScopeGraph().getAllRefs()) {
                if (ref.getPosition().equals(index)) {
                    for (Occurrence decl : Paths.pathsToDecls(s.getNameResolution().resolve(ref))) {
                        entries.add(GenericTerms.newTuple(Iterables2.from(ref, decl.getName())));
                    }
                }
            }
            if (entries.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(GenericTerms.newList(entries));
        });
    }

}