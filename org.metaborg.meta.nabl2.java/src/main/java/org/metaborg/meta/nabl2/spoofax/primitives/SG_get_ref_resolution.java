package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.metaborg.meta.nabl2.scopegraph.IPath;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Paths;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.GenericTerms;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.Lists;

public class SG_get_ref_resolution extends ScopeGraphPrimitive {

    public SG_get_ref_resolution() {
        super(SG_get_ref_resolution.class.getSimpleName(), 0, 0);
    }

    @Override public Optional<ITerm> call(IScopeGraphContext<?> context, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        return Occurrence.matcher().match(term).<ITerm> flatMap(ref -> {
            return context.unit(ref.getPosition().getResource()).solution().flatMap(s -> {
                List<IPath<Scope,Label,Occurrence>> paths = Lists.newArrayList(s.getNameResolution().resolve(ref));
                if (paths.size() != 1) {
                    return Optional.empty();
                }
                IPath<Scope,Label,Occurrence> path = paths.get(0);
                ITerm result = GenericTerms.newTuple(path.getDeclaration(), Paths.toTerm(path));
                return Optional.of(result);
            });
        });
    }

}