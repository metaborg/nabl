package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.Optional;

import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.spoofax.IScopeGraphContext;
import org.metaborg.meta.nabl2.stratego.StrategoTerms;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;

public class SG_get_decl_property extends ScopeGraphPrimitive {

    public SG_get_decl_property() {
        super(SG_get_decl_property.class.getSimpleName(), 0, 1);
    }

    @Override public boolean call(IScopeGraphContext<?> context, IContext env, Strategy[] strategies,
            IStrategoTerm[] terms) throws InterpreterException {
        if (terms.length != 1) {
            throw new InterpreterException("Need one term argument: key");
        }
        StrategoTerms strategoTerms = new StrategoTerms(env.getFactory());
        ITerm declTerm = strategoTerms.fromStratego(env.current());
        ITerm key = strategoTerms.fromStratego(terms[0]);
        return Occurrence.matcher().match(declTerm).<Boolean> flatMap(decl -> {
            return context.unit(decl.getPosition().getResource()).solution().map(s -> {
                Optional<ITerm> v = s.getDeclProperties().getValue(decl, key);
                if (!v.isPresent()) {
                    return false;
                }
                ITerm result = s.getUnifier().find(v.get());
                env.setCurrent(strategoTerms.toStratego(result));
                return true;
            });
        }).orElse(false);
    }

}