package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphUnit;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.Lists;

public class SG_get_ast_refs extends AnalysisPrimitive {

    public SG_get_ast_refs() {
        super(SG_get_ast_refs.class.getSimpleName());
    }

    @SuppressWarnings("unlikely-arg-type") @Override public Optional<ITerm> call(IScopeGraphUnit unit, ITerm term,
            List<ITerm> terms) throws InterpreterException {
        return TermIndex.get(term).flatMap(index -> {
            return unit.solution().<ITerm>flatMap(s -> {
                List<ITerm> entries = Lists.newArrayList();
                for(Occurrence ref : s.scopeGraph().getAllRefs()) {
                    if(ref.getIndex().equals(index)) {
                        entries.add(ref);
                    }
                }
                return Optional.of(TB.newList(entries));
            });
        });
    }

}