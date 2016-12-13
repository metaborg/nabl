package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.Optional;

import org.metaborg.meta.nabl2.spoofax.IScopeGraphContext;
import org.metaborg.meta.nabl2.stratego.StrategoTermIndex;
import org.metaborg.meta.nabl2.stratego.StrategoTerms;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.ImmutableTermIndex;
import org.metaborg.meta.nabl2.terms.generic.TermIndex;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;

public class SG_get_ast_property extends ScopeGraphPrimitive {

    public SG_get_ast_property() {
        super(SG_get_ast_property.class.getSimpleName(), 0, 1);
    }

    @Override public boolean call(IScopeGraphContext<?> context, IContext env, Strategy[] strategies,
            IStrategoTerm[] terms) throws InterpreterException {
        StrategoTermIndex strategoIndex = StrategoTermIndex.get(env.current());
        if (strategoIndex == null) {
            return false;
        }
        TermIndex index = ImmutableTermIndex.of(strategoIndex.getResource(), strategoIndex.getId());
        if (terms.length != 1) {
            throw new InterpreterException("Need one term argument: key");
        }
        return context.unit(strategoIndex.getResource()).solution().map(s -> {
            StrategoTerms strategoTerms = new StrategoTerms(env.getFactory());
            ITerm key = strategoTerms.fromStratego(terms[0]);
            Optional<ITerm> v = s.getAstProperties().getValue(index, key);
            if (!v.isPresent()) {
                return false;
            }
            ITerm result = s.getUnifier().find(v.get());
            env.setCurrent(strategoTerms.toStratego(result));
            return true;
        }).orElse(false);
    }

}