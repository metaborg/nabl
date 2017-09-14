package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphUnit;
import org.metaborg.meta.nabl2.spoofax.analysis.StrategoAnalysis;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

public class SG_get_resource_analysis extends ScopeGraphContextPrimitive {

    public SG_get_resource_analysis() {
        super(SG_get_resource_analysis.class.getSimpleName(), 0, 0);
    }

    @Override public Optional<? extends IStrategoTerm> call(IScopeGraphContext<?> context, IStrategoTerm sterm,
            List<IStrategoTerm> sterms, ITermFactory factory) throws InterpreterException {
        if(!Tools.isTermString(sterm)) {
            throw new InterpreterException("Expect a resource path.");
        }
        String resource = Tools.asJavaString(sterm);
        final IScopeGraphUnit unit = context.unit(resource);
        return Optional.of(new StrategoAnalysis(unit));
    }

}