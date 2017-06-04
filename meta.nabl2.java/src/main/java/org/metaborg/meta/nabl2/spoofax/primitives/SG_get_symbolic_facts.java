package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.spoofax.interpreter.core.InterpreterException;

public class SG_get_symbolic_facts extends AnalysisPrimitive {

    public SG_get_symbolic_facts() {
        super(SG_get_symbolic_facts.class.getSimpleName());
    }

    @Override public Optional<? extends ITerm> call(IScopeGraphContext<?> context, TermIndex index, ITerm term)
            throws InterpreterException {
        return context.unit(index.getResource()).solution().map(s -> {
            return TB
                    .newList(s.symbolic().getFacts().stream().map(s.unifier()::find).collect(Collectors.toSet()));
        });
    }

}