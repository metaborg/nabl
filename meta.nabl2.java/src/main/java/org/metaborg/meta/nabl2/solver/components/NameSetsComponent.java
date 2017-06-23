package org.metaborg.meta.nabl2.solver.components;

import java.util.Optional;

import org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Namespace;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.sets.IElement;
import org.metaborg.meta.nabl2.solver.ASolver;
import org.metaborg.meta.nabl2.solver.SolverCore;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;

import com.google.common.collect.Sets;

public class NameSetsComponent extends ASolver {

    private final IEsopScopeGraph<Scope, Label, Occurrence, ITerm> scopeGraph;
    private final IEsopNameResolution.Transient<Scope, Label, Occurrence> nameResolution;

    public NameSetsComponent(SolverCore core, IEsopScopeGraph<Scope, Label, Occurrence, ITerm> scopeGraph,
            IEsopNameResolution.Transient<Scope, Label, Occurrence> nameResolution) {
        super(core);
        this.scopeGraph = scopeGraph;
        this.nameResolution = nameResolution;
    }

    public IMatcher<java.util.Set<IElement<ITerm>>> nameSets() {
        return term -> {
            return M.<Optional<java.util.Set<IElement<ITerm>>>>cases(
                // @formatter:off
                M.appl2("Declarations", Scope.matcher(), Namespace.matcher(), (t, scope, ns) -> {
                    Iterable<Occurrence> decls = NameSetsComponent.this.scopeGraph.getDecls().inverse().get(scope);
                    return Optional.of(makeSet(decls, ns));
                }),
                M.appl2("References", Scope.matcher(), Namespace.matcher(), (t, scope, ns) -> {
                    Iterable<Occurrence> refs = NameSetsComponent.this.scopeGraph.getRefs().inverse().get(scope);
                    return Optional.of(makeSet(refs, ns));
                }),
                M.appl2("Visibles", Scope.matcher(), Namespace.matcher(), (t, scope, ns) -> {
                    Optional<? extends io.usethesource.capsule.Set<Occurrence>> decls =
                            NameSetsComponent.this.nameResolution.visible(scope);
                    return decls.map(ds -> makeSet(ds, ns));
                }),
                M.appl2("Reachables", Scope.matcher(), Namespace.matcher(), (t, scope, ns) -> {
                    Optional<? extends io.usethesource.capsule.Set<Occurrence>> decls =
                            NameSetsComponent.this.nameResolution.reachable(scope);
                    return decls.map(ds -> makeSet(ds, ns));
                })
                // @formatter:on
            ).match(term).flatMap(o -> o);
        };
    }

    private java.util.Set<IElement<ITerm>> makeSet(Iterable<Occurrence> occurrences, Namespace namespace) {
        java.util.Set<IElement<ITerm>> result = Sets.newHashSet();
        for(Occurrence occurrence : occurrences) {
            if(namespace.getName().isEmpty() || namespace.equals(occurrence.getNamespace())) {
                result.add(new OccurrenceElement(occurrence));
            }
        }
        return result;
    }

    private static class OccurrenceElement implements IElement<ITerm> {

        private final Occurrence occurrence;

        public OccurrenceElement(Occurrence occurrence) {
            this.occurrence = occurrence;
        }

        @Override public ITerm getValue() {
            return occurrence;
        }

        @Override public ITerm getPosition() {
            return occurrence.getIndex();
        }

        @Override public Object project(String name) {
            switch(name) {
                case "name":
                    return occurrence.getName();
                default:
                    throw new IllegalArgumentException("Projection " + name + " undefined for occurrences.");
            }
        }

    }

}