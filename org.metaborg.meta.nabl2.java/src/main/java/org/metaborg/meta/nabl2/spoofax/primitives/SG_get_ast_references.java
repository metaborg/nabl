package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;

import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.spoofax.IScopeGraphContext;
import org.metaborg.meta.nabl2.stratego.StrategoTermIndex;
import org.metaborg.meta.nabl2.stratego.StrategoTerms;
import org.metaborg.meta.nabl2.terms.ITermIndex;
import org.metaborg.meta.nabl2.terms.generic.ImmutableTermIndex;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.common.collect.Lists;

public class SG_get_ast_references extends ScopeGraphPrimitive {

    public SG_get_ast_references() {
        super(SG_get_ast_references.class.getSimpleName(), 0, 0);
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
            List<IStrategoTerm> decls = Lists.newArrayList();
            for (Occurrence ref : s.getScopeGraph().getAstRefs(index)) {
                for (Occurrence decl : s.getNameResolution().resolve(ref)) {
                    decls.add(strategoTerms.toStratego(decl.getName()));
                }
            }
            IStrategoTerm result;
            if (decls.isEmpty()) {
                return false;
            }
            if (decls.size() == 1) {
                result = decls.get(0);
            } else {
                result = env.getFactory().makeList(decls);
            }
            env.setCurrent(result);
            return true;
        }).orElse(false);
    }

}
