package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;

import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.spoofax.IScopeGraphContext;
import org.metaborg.meta.nabl2.stratego.StrategoTermIndex;
import org.metaborg.meta.nabl2.stratego.StrategoTerms;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermIndex;
import org.metaborg.meta.nabl2.terms.generic.GenericTerms;
import org.metaborg.meta.nabl2.terms.generic.ImmutableTermIndex;
import org.metaborg.util.iterators.Iterables2;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.common.collect.Lists;

public class SG_get_ast_resolution extends ScopeGraphPrimitive {

    public SG_get_ast_resolution() {
        super(SG_get_ast_resolution.class.getSimpleName(), 0, 0);
    }

    @Override public boolean call(IScopeGraphContext<?> context, IContext env, Strategy[] strategies,
            IStrategoTerm[] terms) throws InterpreterException {
        StrategoTermIndex strategoIndex = StrategoTermIndex.get(env.current());
        if (strategoIndex == null) {
            return false;
        }
        ITermIndex index = ImmutableTermIndex.of(strategoIndex.getResource(), strategoIndex.getId());
        return context.unit(strategoIndex.getResource()).solution().map(s -> {
            StrategoTerms strategoTerms = new StrategoTerms(env.getFactory());
            List<ITerm> entries = Lists.newArrayList();
            for (Occurrence ref : s.getScopeGraph().getAllRefs()) {
                if (ref.getPosition().equals(index)) {
                    for (Occurrence decl : s.getNameResolution().resolve(ref)) {
                        entries.add(GenericTerms.newAppl("", Iterables2.from(ref, decl.getName())));
                    }
                }
            }
            if (entries.isEmpty()) {
                return false;
            }
            ITerm result = GenericTerms.newList(entries);
            env.setCurrent(strategoTerms.toStratego(result));
            return true;
        }).orElse(false);
    }

}