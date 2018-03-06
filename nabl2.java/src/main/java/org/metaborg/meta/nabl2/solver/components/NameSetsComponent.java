package org.metaborg.meta.nabl2.solver.components;

import static org.metaborg.meta.nabl2.terms.matching.TermMatch.M;

import java.util.Optional;
import java.util.Set;

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
import org.metaborg.meta.nabl2.terms.matching.TermMatch.IMatcher;

import com.google.common.collect.Sets;

public class NameSetsComponent extends ASolver {

    private final IEsopScopeGraph<Scope, Label, Occurrence, ITerm> scopeGraph;
    private final IEsopNameResolution<Scope, Label, Occurrence> nameResolution;

    public NameSetsComponent(SolverCore core, IEsopScopeGraph<Scope, Label, Occurrence, ITerm> scopeGraph,
            IEsopNameResolution<Scope, Label, Occurrence> nameResolution) {
        super(core);
        this.scopeGraph = scopeGraph;
        this.nameResolution = nameResolution;
    }

    public IMatcher<java.util.Set<IElement<ITerm>>> nameSets() {
        return IMatcher.flatten(M.<Optional<java.util.Set<IElement<ITerm>>>>cases(
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
                Optional<? extends Set<Occurrence>> decls =
                        NameSetsComponent.this.nameResolution.visible(scope);
                return decls.map(ds -> makeSet(ds, ns));
            }),
            M.appl2("Reachables", Scope.matcher(), Namespace.matcher(), (t, scope, ns) -> {
                Optional<? extends Set<Occurrence>> decls =
                        NameSetsComponent.this.nameResolution.reachable(scope);
                return decls.map(ds -> makeSet(ds, ns));
            })
            // @formatter:on
        ));
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

        @Override public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + occurrence.hashCode();
            return result;
        }

        @Override public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            OccurrenceElement other = (OccurrenceElement) obj;
            if(occurrence == null) {
                if(other.occurrence != null)
                    return false;
            } else if(!occurrence.equals(other.occurrence))
                return false;
            return true;
        }

    }

}
