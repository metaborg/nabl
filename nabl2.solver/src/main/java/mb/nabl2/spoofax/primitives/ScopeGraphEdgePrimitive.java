package mb.nabl2.spoofax.primitives;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.metaborg.util.collection.IRelation3;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.Lists;

import mb.nabl2.scopegraph.IScopeGraph;
import mb.nabl2.scopegraph.terms.Label;
import mb.nabl2.scopegraph.terms.Occurrence;
import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.solver.ISolution;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;

public abstract class ScopeGraphEdgePrimitive<S extends ITerm> extends AnalysisPrimitive {

    public ScopeGraphEdgePrimitive(String name) {
        super(name);
    }

    @Override public Optional<ITerm> call(ISolution solution, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        final IRelation3<S, Label, ? extends ITerm> edges = getEdges(solution.scopeGraph());
        final IMatcher<S> sourceMatcher = getSourceMatcher();
        // @formatter:off
        return M.<ITerm>cases(
            M.term(sourceMatcher, (t, source) -> {
                List<ITerm> edgeTerms = Lists.newArrayList();
                for(Map.Entry<Label, ? extends ITerm> edge : edges.get(source)) {
                    edgeTerms.add(B.newTuple(edge.getKey(), edge.getValue()));
                }
                return B.newList(edgeTerms);
            }),
            M.tuple2(sourceMatcher, Label.matcher(), (t, source, label) -> {
                List<ITerm> targetTerms = Lists.newArrayList();
                for(ITerm target : edges.get(source, label)) {
                    targetTerms.add(target);
                }
                return B.newList(targetTerms);
            })
        ).match(term, solution.unifier());
        // @formatter:on
    }

    protected abstract IMatcher<S> getSourceMatcher();

    protected abstract IRelation3<S, Label, ? extends ITerm> getEdges(IScopeGraph<Scope, Label, Occurrence> scopeGraph);

}