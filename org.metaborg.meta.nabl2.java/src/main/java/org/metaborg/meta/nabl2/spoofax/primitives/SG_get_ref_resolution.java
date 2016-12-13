package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;

import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.spoofax.IScopeGraphContext;
import org.metaborg.meta.nabl2.stratego.StrategoTerms;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.GenericTerms;
import org.metaborg.util.iterators.Iterables2;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.common.collect.Lists;

public class SG_get_ref_resolution extends ScopeGraphPrimitive {

    public SG_get_ref_resolution() {
        super(SG_get_ref_resolution.class.getSimpleName(), 0, 0);
    }

    @Override public boolean call(IScopeGraphContext<?> context, IContext env, Strategy[] strategies,
            IStrategoTerm[] terms) throws InterpreterException {
        StrategoTerms strategoTerms = new StrategoTerms(env.getFactory());
        ITerm refTerm = strategoTerms.fromStratego(env.current());
        return Occurrence.matcher().match(refTerm).<Boolean> flatMap(ref -> {
            return context.unit(ref.getPosition().getResource()).solution().map(s -> {
                List<Occurrence> decls = Lists.newArrayList(s.getNameResolution().resolve(ref));
                if (decls.size() != 1) {
                    return false;
                }
                Occurrence decl = decls.get(0);
                ITerm result = GenericTerms.newTuple(Iterables2.from(decl, GenericTerms.newNil()));
                env.setCurrent(strategoTerms.toStratego(result));
                return true;
            });
        }).orElse(false);
    }

}