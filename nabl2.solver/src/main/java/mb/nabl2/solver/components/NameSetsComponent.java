package mb.nabl2.solver.components;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Collection;

import org.metaborg.util.collection.CapsuleUtil;

import io.usethesource.capsule.Set;
import mb.nabl2.sets.IElement;
import mb.nabl2.sets.ISetProducer;
import mb.nabl2.solver.ASolver;
import mb.nabl2.solver.SolverCore;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.scopegraph.pepm16.esop15.IEsopNameResolution;
import mb.scopegraph.pepm16.terms.Label;
import mb.scopegraph.pepm16.terms.Namespace;
import mb.scopegraph.pepm16.terms.Occurrence;
import mb.scopegraph.pepm16.terms.Scope;

public class NameSetsComponent extends ASolver {

    private final IEsopNameResolution<Scope, Label, Occurrence> nameResolution;

    public NameSetsComponent(SolverCore core, IEsopNameResolution<Scope, Label, Occurrence> nameResolution) {
        super(core);
        this.nameResolution = nameResolution;
    }

    public IMatcher<ISetProducer<ITerm>> nameSets() {
        // @formatter:off
        return M.<ISetProducer<ITerm>>cases(
            M.appl2("Declarations", Scope.matcher(), Namespace.matcher(), (t, scope, ns) -> () -> {
                Collection<Occurrence> decls = NameSetsComponent.this.nameResolution.decls(scope);
                return makeSet(decls, ns);
            }),
            M.appl2("References", Scope.matcher(), Namespace.matcher(), (t, scope, ns) -> () -> {
                Collection<Occurrence> refs = NameSetsComponent.this.nameResolution.refs(scope);
                return makeSet(refs, ns);
            }),
            M.appl2("Visibles", Scope.matcher(), Namespace.matcher(), (t, scope, ns) -> () -> {
                Collection<Occurrence> decls = NameSetsComponent.this.nameResolution.visible(scope, cancel, progress);
                return makeSet(decls, ns);
            }),
            M.appl2("Reachables", Scope.matcher(), Namespace.matcher(), (t, scope, ns) -> () -> {
                Collection<Occurrence> decls = NameSetsComponent.this.nameResolution.reachable(scope, cancel, progress);
                return makeSet(decls, ns);
            })
        );
        // @formatter:on
    }

    private Set.Immutable<IElement<ITerm>> makeSet(Iterable<Occurrence> occurrences, Namespace namespace) {
        Set.Transient<IElement<ITerm>> result = CapsuleUtil.transientSet();
        for(Occurrence occurrence : occurrences) {
            if(namespace.getName().isEmpty() || namespace.equals(occurrence.getNamespace())) {
                result.__insert(new OccurrenceElement(occurrence));
            }
        }
        return result.freeze();
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
            return occurrence.getNameOrIndexOrigin();
        }

        @Override public ITerm getName() {
            return occurrence.getName();
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
