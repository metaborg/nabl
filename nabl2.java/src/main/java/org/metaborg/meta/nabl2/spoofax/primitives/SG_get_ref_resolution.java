package org.metaborg.meta.nabl2.spoofax.primitives;

import static org.metaborg.meta.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Optional;

import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.scopegraph.terms.path.Paths;
import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphUnit;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.Lists;

public class SG_get_ref_resolution extends AnalysisPrimitive {

    public SG_get_ref_resolution() {
        super(SG_get_ref_resolution.class.getSimpleName(), 0);
    }

    @Override public Optional<? extends ITerm> call(IScopeGraphUnit unit, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        return unit.solution().flatMap(s -> {
            return Occurrence.matcher().match(term, s.unifier()).<ITerm>flatMap(ref -> {
                return s.nameResolution().resolve(ref).map(paths -> {
                    List<ITerm> pathTerms = Lists.newArrayListWithExpectedSize(paths.size());
                    for(IResolutionPath<Scope, Label, Occurrence> path : paths) {
                        pathTerms.add(B.newTuple(path.getDeclaration(), Paths.toTerm(path)));
                    }
                    ITerm result = B.newList(pathTerms);
                    return result;
                });
            });
        });
    }

}