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
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.scopegraph.terms.path.Paths;
import org.metaborg.meta.nabl2.solver.ASolver;
import org.metaborg.meta.nabl2.solver.ISolver.SeedResult;
import org.metaborg.meta.nabl2.solver.ISolver.SolveResult;
import org.metaborg.meta.nabl2.solver.ImmutableSolveResult;
import org.metaborg.meta.nabl2.solver.SolverCore;
import org.metaborg.meta.nabl2.solver.TypeException;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.collection.VarMultimap;
import org.metaborg.meta.nabl2.util.collections.IProperties;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class NameResolutionComponent extends ASolver {

    private final IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph;
    private final IEsopNameResolution<Scope, Label, Occurrence> nameResolution;
    private final IProperties.Transient<Occurrence, ITerm, ITerm> properties;
    private final VarMultimap<Occurrence> varDeps;

    public NameResolutionComponent(SolverCore core,
            IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph,
            IEsopNameResolution<Scope, Label, Occurrence> nameResolution,
            IProperties.Transient<Occurrence, ITerm, ITerm> initial) {
        super(core);
        this.scopeGraph = scopeGraph;
        this.nameResolution = nameResolution;
        this.properties = initial;
        this.varDeps = new VarMultimap<>();
    }

    // ------------------------------------------------------------------------------------------------------//

    public SeedResult seed(NameResolutionResult solution, IMessageInfo message) throws InterruptedException {
        final java.util.Set<IConstraint> constraints = Sets.newHashSet();
        scopeGraph.addAll(solution.scopeGraph());
        solution.resolutionCache().ifPresent(nameResolution::addAll);
        constraints.addAll(seed(solution.declProperties(), message).constraints());
        return SeedResult.constraints(constraints);
    }

    public SeedResult seed(IProperties<Occurrence, ITerm, ITerm> solution, IMessageInfo message)
            throws InterruptedException {
        final java.util.Set<IConstraint> constraints = Sets.newHashSet();
        solution.stream().forEach(entry -> {
            putProperty(entry._1(), entry._2(), entry._3(), message).ifPresent(constraints::add);
        });
        return SeedResult.constraints(constraints);
    }

    public Optional<SolveResult> solve(INameResolutionConstraint constraint) {
        return constraint.match(INameResolutionConstraint.Cases.of(this::solve, this::solve, this::solve));
    }

    public void update() throws InterruptedException {
        scopeGraph.reduce(this::findScope, this::findOccurrence);
        nameResolution.resolveAll(scopeGraph.getAllRefs());
    }

    public NameResolutionResult finish() {
        return ImmutableNameResolutionResult.of(scopeGraph.freeze(), properties.freeze())
                .withResolutionCache(nameResolution.toCache());
    }

    public IProperties.Immutable<Occurrence, ITerm, ITerm> finishDeclProperties() {
        return properties.freeze();
    }

    // ------------------------------------------------------------------------------------------------------//

    private Optional<SolveResult> solve(CResolve r) {
        final ITerm refTerm = unifier().findRecursive(r.getReference());
        if(!refTerm.isGround()) {
            return Optional.empty();
        }
        final Occurrence ref = Occurrence.matcher().match(refTerm, unifier())
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + r));
        final Optional<java.util.Set<IResolutionPath<Scope, Label, Occurrence>>> maybePathsAndDeps =
                nameResolution.resolve(ref);
        if(!maybePathsAndDeps.isPresent()) {
            return Optional.empty();
        }
        final java.util.Set<IResolutionPath<Scope, Label, Occurrence>> paths = maybePathsAndDeps.get();
        final List<Occurrence> declarations = Paths.resolutionPathsToDecls(paths);
        final Multimap<String, String> deps = HashMultimap.create();
        deps.putAll(ref.getIndex().getResource(),
                declarations.stream().map(d -> d.getIndex().getResource()).collect(Collectors.toSet()));
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
        return Optional.of(ImmutableSolveResult.copyOf(result).withDependencies(deps));
    }

    private Optional<SolveResult> solve(CAssoc a) {
        final ITerm declTerm = unifier().findRecursive(a.getDeclaration());
        if(!declTerm.isGround()) {
            return Optional.empty();
        }
        final Occurrence decl = Occurrence.matcher().match(declTerm, unifier())
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
        final ITerm declTerm = unifier().findRecursive(c.getDeclaration());
        if(!declTerm.isGround()) {
            return Optional.empty();
        }
        final Occurrence decl = Occurrence.matcher().match(declTerm, unifier())
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + c));
        final SolveResult result = putProperty(decl, c.getKey(), c.getValue(), c.getMessageInfo())
                .map(cc -> SolveResult.constraints(cc)).orElseGet(() -> SolveResult.empty());
        return Optional.of(result);
    }

    private Optional<IConstraint> putProperty(Occurrence decl, ITerm key, ITerm value, IMessageInfo message) {
        Optional<ITerm> prev = properties.getValue(decl, key);
        if(!prev.isPresent()) {
            properties.putValue(decl, key, value);
            value.getVars().elementSet().stream().forEach(var -> varDeps.put(var, decl, unifier()));
            return Optional.empty();
        } else {
            return Optional.of(ImmutableCEqual.of(value, prev.get(), message));
        }

    }

    public Optional<ITerm> getProperty(Occurrence decl, ITerm key) {
        return properties.getValue(decl, key);
    }

    public java.util.Set<Occurrence> getDeps(ITerm term) {
        return term.getVars().stream().flatMap(var -> varDeps.get(var, unifier()).stream()).collect(Collectors.toSet());
    }

    // ------------------------------------------------------------------------------------------------------//

    private Optional<Scope> findScope(ITerm scopeTerm) {
        return Optional.of(unifier().findRecursive(scopeTerm)).filter(ITerm::isGround).map(st -> Scope.matcher()
                .match(st, unifier()).orElseThrow(() -> new TypeException("Expected a scope, got " + st)));
    }

    private Optional<Occurrence> findOccurrence(ITerm occurrenceTerm) {
        return Optional.of(unifier().findRecursive(occurrenceTerm)).filter(ITerm::isGround)
                .map(ot -> Occurrence.matcher().match(ot, unifier())
                        .orElseThrow(() -> new TypeException("Expected an occurrence, got " + ot)));
    }

    @Value.Immutable
    @Serial.Version(42l)
    public static abstract class NameResolutionResult {

        @Value.Parameter public abstract IEsopScopeGraph.Immutable<Scope, Label, Occurrence, ITerm> scopeGraph();

        @Value.Auxiliary public abstract Optional<IEsopNameResolution.ResolutionCache<Scope, Label, Occurrence>>
                resolutionCache();

        @Value.Parameter public abstract IProperties.Immutable<Occurrence, ITerm, ITerm> declProperties();

    }

}