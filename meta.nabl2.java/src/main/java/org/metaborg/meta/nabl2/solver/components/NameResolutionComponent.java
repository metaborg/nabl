package org.metaborg.meta.nabl2.solver.components;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.equality.ImmutableCEqual;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.constraints.nameresolution.CAssoc;
import org.metaborg.meta.nabl2.constraints.nameresolution.CDeclProperty;
import org.metaborg.meta.nabl2.constraints.nameresolution.CResolve;
import org.metaborg.meta.nabl2.constraints.nameresolution.INameResolutionConstraint;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Namespace;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.scopegraph.terms.path.Paths;
import org.metaborg.meta.nabl2.sets.IElement;
import org.metaborg.meta.nabl2.solver.ASolver;
import org.metaborg.meta.nabl2.solver.ISolver.SeedResult;
import org.metaborg.meta.nabl2.solver.ISolver.SolveResult;
import org.metaborg.meta.nabl2.solver.ImmutableSolveResult;
import org.metaborg.meta.nabl2.solver.SolverCore;
import org.metaborg.meta.nabl2.solver.TypeException;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.util.collections.IProperties;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Set;

public class NameResolutionComponent extends ASolver {

    private final IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph;
    private final IEsopNameResolution.Transient<Scope, Label, Occurrence> nameResolution;
    private final IProperties.Transient<Occurrence, ITerm, ITerm> properties;

    public NameResolutionComponent(SolverCore core,
            IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph,
            IEsopNameResolution.Transient<Scope, Label, Occurrence> nameResolution,
            IProperties.Transient<Occurrence, ITerm, ITerm> initial) {
        super(core);
        this.scopeGraph = scopeGraph;
        this.nameResolution = nameResolution;
        this.properties = initial;
    }

    // ------------------------------------------------------------------------------------------------------//

    public SeedResult seed(NameResolutionResult solution, IMessageInfo message) throws InterruptedException {
        final java.util.Set<IConstraint> constraints = Sets.newHashSet();
        scopeGraph.addAll(solution.scopeGraph());
        nameResolution.addAll(solution.nameResolution());
        solution.declProperties().stream().forEach(entry -> {
            properties.putValue(entry._1(), entry._2(), entry._3()).ifPresent(prev -> {
                constraints.add(ImmutableCEqual.of(entry._3(), prev, message));
            });
        });
        return SeedResult.constraints(constraints);
    }

    public Optional<SolveResult> solve(INameResolutionConstraint constraint) {
        return constraint.match(INameResolutionConstraint.Cases.of(this::solve, this::solve, this::solve));
    }

    public boolean update() throws InterruptedException {
        boolean change = false;
        change |= scopeGraph.reduce(this::findScope, this::findOccurrence);
        change |= nameResolution.resolve();
        return change;
    }

    public NameResolutionResult finish() {
        return ImmutableNameResolutionResult.of(scopeGraph.freeze(), nameResolution.freeze(), properties.freeze());
    }

    // ------------------------------------------------------------------------------------------------------//

    private Optional<SolveResult> solve(CResolve r) {
        final ITerm refTerm = find(r.getReference());
        if(!refTerm.isGround()) {
            return Optional.empty();
        }
        final Occurrence ref = Occurrence.matcher().match(refTerm)
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + r));
        final Optional<Set.Immutable<IResolutionPath<Scope, Label, Occurrence>>> maybePathsAndDeps =
                nameResolution.tryResolve(ref);
        if(!maybePathsAndDeps.isPresent()) {
            return Optional.empty();
        }
        final Set.Immutable<IResolutionPath<Scope, Label, Occurrence>> paths = maybePathsAndDeps.get();
        final List<Occurrence> declarations = Paths.resolutionPathsToDecls(paths);
        final Multimap<String, String> deps = HashMultimap.create();
        deps.putAll(ref.getIndex().getResource(),
                declarations.stream().map(d -> d.getIndex().getResource()).collect(Collectors.toSet()));
        final ImmutableSolveResult result;
        switch(declarations.size()) {
            case 0: {
                IMessageInfo message = r.getMessageInfo()
                        .withDefaultContent(MessageContent.builder().append(ref).append(" does not resolve.").build());
                result = SolveResult.messages(message);
                break;
            }
            case 1: {
                final Occurrence decl = declarations.get(0);
                result = SolveResult.constraints(ImmutableCEqual.of(r.getDeclaration(), decl, r.getMessageInfo()));
                break;
            }
            default: {
                IMessageInfo message = r.getMessageInfo().withDefaultContent(
                        MessageContent.builder().append("Resolution of ").append(ref).append(" is ambiguous.").build());
                result = SolveResult.messages(message);
                break;
            }
        }
        return Optional.of(result.withDependencies(deps));
    }

    private Optional<SolveResult> solve(CAssoc a) {
        final ITerm declTerm = find(a.getDeclaration());
        if(!declTerm.isGround()) {
            return Optional.empty();
        }
        final Occurrence decl = Occurrence.matcher().match(declTerm)
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + a));
        final Label label = a.getLabel();
        final List<Scope> scopes = Lists.newArrayList(scopeGraph.getExportEdges().get(decl, label));
        final SolveResult result;
        switch(scopes.size()) {
            case 0: {
                IMessageInfo message = a.getMessageInfo().withDefaultContent(MessageContent.builder().append(decl)
                        .append(" has no ").append(label).append(" associated scope.").build());
                result = SolveResult.messages(message);
                break;
            }
            case 1: {
                result = SolveResult.constraints(ImmutableCEqual.of(a.getScope(), scopes.get(0), a.getMessageInfo()));
                break;
            }
            default: {
                IMessageInfo message = a.getMessageInfo().withDefaultContent(MessageContent.builder().append(decl)
                        .append(" has multiple ").append(label).append(" associated scope.").build());
                result = SolveResult.messages(message);
                break;
            }
        }
        return Optional.of(result);
    }

    private Optional<SolveResult> solve(CDeclProperty c) {
        final ITerm declTerm = find(c.getDeclaration());
        if(!declTerm.isGround()) {
            return Optional.empty();
        }
        final Occurrence decl = Occurrence.matcher().match(declTerm)
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + c));
        Optional<ITerm> prev = properties.putValue(decl, c.getKey(), c.getValue());
        final SolveResult result;
        if(!prev.isPresent()) {
            result = SolveResult.empty();
        } else {
            result = SolveResult.constraints(ImmutableCEqual.of(c.getValue(), prev.get(), c.getMessageInfo()));
        }
        return Optional.of(result);
    }

    // ------------------------------------------------------------------------------------------------------//

    public IMatcher<java.util.Set<IElement<ITerm>>> nameSets() {
        return term -> {
            return M.<Optional<java.util.Set<IElement<ITerm>>>>cases(
                // @formatter:off
                M.appl2("Declarations", Scope.matcher(), Namespace.matcher(), (t, scope, ns) -> {
                    Iterable<Occurrence> decls = NameResolutionComponent.this.scopeGraph.getDecls().inverse().get(scope);
                    return Optional.of(makeSet(decls, ns));
                }),
                M.appl2("References", Scope.matcher(), Namespace.matcher(), (t, scope, ns) -> {
                    Iterable<Occurrence> refs = NameResolutionComponent.this.scopeGraph.getRefs().inverse().get(scope);
                    return Optional.of(makeSet(refs, ns));
                }),
                M.appl2("Visibles", Scope.matcher(), Namespace.matcher(), (t, scope, ns) -> {
                    Optional<? extends io.usethesource.capsule.Set<Occurrence>> decls =
                            NameResolutionComponent.this.nameResolution.tryVisible(scope);
                    return decls.map(ds -> makeSet(ds, ns));
                }),
                M.appl2("Reachables", Scope.matcher(), Namespace.matcher(), (t, scope, ns) -> {
                    Optional<? extends io.usethesource.capsule.Set<Occurrence>> decls =
                            NameResolutionComponent.this.nameResolution.tryReachable(scope);
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

    // ------------------------------------------------------------------------------------------------------//

    private Optional<Scope> findScope(ITerm scopeTerm) {
        return Optional.of(find(scopeTerm)).filter(ITerm::isGround).map(
                st -> Scope.matcher().match(st).orElseThrow(() -> new TypeException("Expected a scope, got " + st)));
    }

    private Optional<Occurrence> findOccurrence(ITerm occurrenceTerm) {
        return Optional.of(find(occurrenceTerm)).filter(ITerm::isGround).map(ot -> Occurrence.matcher().match(ot)
                .orElseThrow(() -> new TypeException("Expected an occurrence, got " + ot)));
    }

    @Value.Immutable
    @Serial.Version(42l)
    public static abstract class NameResolutionResult {

        @Value.Parameter public abstract IEsopScopeGraph.Immutable<Scope, Label, Occurrence, ITerm> scopeGraph();

        @Value.Parameter public abstract IEsopNameResolution.Immutable<Scope, Label, Occurrence> nameResolution();

        @Value.Parameter public abstract IProperties.Immutable<Occurrence, ITerm, ITerm> declProperties();

    }

}