package org.metaborg.meta.nabl2.solver.components;

import java.util.List;
import java.util.Optional;

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
import org.metaborg.meta.nabl2.scopegraph.path.IDeclPath;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Namespace;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.scopegraph.terms.path.Paths;
import org.metaborg.meta.nabl2.sets.IElement;
import org.metaborg.meta.nabl2.solver.ASolver;
import org.metaborg.meta.nabl2.solver.ImmutableSolveResult;
import org.metaborg.meta.nabl2.solver.SolverCore;
import org.metaborg.meta.nabl2.solver.TypeException;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.util.collections.IProperties;
import org.metaborg.meta.nabl2.util.functions.Predicate0;
import org.metaborg.meta.nabl2.util.functions.Predicate2;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Set;

public class NameResolutionComponent
        extends ASolver<INameResolutionConstraint, IProperties.Immutable<Occurrence, ITerm, ITerm>> {
    private static final ILogger logger = LoggerUtils.logger(NameResolutionComponent.class);

    private final Predicate0 isGraphComplete;
    private final IEsopScopeGraph<Scope, Label, Occurrence, ?> scopeGraph;
    private final IEsopNameResolution<Scope, Label, Occurrence> nameResolution;
    private final IProperties.Transient<Occurrence, ITerm, ITerm> properties;

    public NameResolutionComponent(SolverCore core, Predicate0 isGraphComplete, Predicate2<Scope, Label> isEdgeClosed,
            IEsopScopeGraph<Scope, Label, Occurrence, ?> scopeGraph,
            IProperties.Transient<Occurrence, ITerm, ITerm> initial) {
        super(core);
        this.isGraphComplete = isGraphComplete;
        this.scopeGraph = scopeGraph;
        this.nameResolution = scopeGraph.resolve(config().getResolutionParams(), isEdgeClosed);
        this.properties = initial;
    }

    // ------------------------------------------------------------------------------------------------------//

    @Override public SeedResult seed(IProperties.Immutable<Occurrence, ITerm, ITerm> solution, IMessageInfo message)
            throws InterruptedException {
        final java.util.Set<IConstraint> constraints = Sets.newHashSet();
        solution.stream().forEach(entry -> {
            properties.putValue(entry._1(), entry._2(), entry._3()).ifPresent(prev -> {
                constraints.add(ImmutableCEqual.of(entry._3(), prev, message));
            });
        });
        return SeedResult.constraints(constraints);
    }

    @Override public Optional<SolveResult> solve(INameResolutionConstraint constraint) {
        return constraint.match(INameResolutionConstraint.Cases.of(this::solve, this::solve, this::solve));
    }

    @Override public IProperties.Immutable<Occurrence, ITerm, ITerm> finish() {
        return properties.freeze();
    }

    // ------------------------------------------------------------------------------------------------------//

    private Optional<SolveResult> solve(CResolve r) {
        if(!isGraphComplete.test()) {
            return Optional.empty();
        }
        final ITerm refTerm = find(r.getReference());
        if(!refTerm.isGround()) {
            return Optional.empty();
        }
        final Occurrence ref = Occurrence.matcher().match(refTerm)
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + r));
        final Optional<Tuple2<Set.Immutable<IResolutionPath<Scope, Label, Occurrence>>, Set.Immutable<String>>> maybePathsAndDeps =
                nameResolution.tryResolve(ref);
        if(!maybePathsAndDeps.isPresent()) {
            return Optional.empty();
        }
        final Set.Immutable<IResolutionPath<Scope, Label, Occurrence>> paths = maybePathsAndDeps.get()._1();
        final String refResource = ref.getIndex().getResource();
        final Multimap<String, String> weakDeps = ImmutableMultimap.<String, String>builder()
                .putAll(refResource, maybePathsAndDeps.get()._2().__remove(refResource)).build();
        final List<Occurrence> declarations = Paths.resolutionPathsToDecls(paths);
        final SolveResult result;
        switch(declarations.size()) {
            case 0: {
                IMessageInfo message = r.getMessageInfo()
                        .withDefaultContent(MessageContent.builder().append(ref).append(" does not resolve.").build());
                result = SolveResult.messages(message);
                break;
            }
            case 1: {
                final Occurrence decl = declarations.get(0);
                final String declResource = decl.getIndex().getResource();
                final Multimap<String, String> strongDeps = !declResource.equals(refResource)
                        ? ImmutableMultimap.of(refResource, declResource) : ImmutableMultimap.of();
                result = ImmutableSolveResult.builder()
                        // @formatter:off
                        .addConstraints(ImmutableCEqual.of(r.getDeclaration(), decl, r.getMessageInfo()))
                        .strongDependencies(strongDeps)
                        .weakDependencies(weakDeps)
                        // @formatter:on
                        .build();
                break;
            }
            default: {
                IMessageInfo message = r.getMessageInfo().withDefaultContent(
                        MessageContent.builder().append("Resolution of ").append(ref).append(" is ambiguous.").build());
                result = SolveResult.messages(message);
                break;
            }
        }
        return Optional.of(result);
    }

    private Optional<SolveResult> solve(CAssoc a) {
        if(!isGraphComplete.test()) {
            return Optional.empty();
        }
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
            if(!isGraphComplete.test()) {
                return Optional.empty();
            }
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
                    Optional<? extends io.usethesource.capsule.Set<IDeclPath<Scope,Label,Occurrence>>> paths =
                            NameResolutionComponent.this.nameResolution.tryVisible(scope).map(pts -> {
                                logger.warn("Possibly ignoring dependencies.");
                                return pts._1();
                            });
                    return paths.map(ps -> makeSet(Paths.declPathsToDecls(ps), ns));
                }),
                M.appl2("Reachables", Scope.matcher(), Namespace.matcher(), (t, scope, ns) -> {
                    Optional<? extends io.usethesource.capsule.Set<IDeclPath<Scope,Label,Occurrence>>> paths =
                            NameResolutionComponent.this.nameResolution.tryReachable(scope).map(pts -> {
                                logger.warn("Possibly ignoring dependencies.");
                                return pts._1();
                            });
                    return paths.map(ps -> makeSet(Paths.declPathsToDecls(ps), ns));
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