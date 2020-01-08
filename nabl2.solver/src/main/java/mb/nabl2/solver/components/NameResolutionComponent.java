package mb.nabl2.solver.components;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.equality.ImmutableCEqual;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.constraints.messages.MessageContent;
import mb.nabl2.constraints.nameresolution.CAssoc;
import mb.nabl2.constraints.nameresolution.CDeclProperty;
import mb.nabl2.constraints.nameresolution.CResolve;
import mb.nabl2.constraints.nameresolution.INameResolutionConstraint;
import mb.nabl2.scopegraph.esop.CriticalEdgeException;
import mb.nabl2.scopegraph.esop.IEsopNameResolution;
import mb.nabl2.scopegraph.esop.IEsopScopeGraph;
import mb.nabl2.scopegraph.path.IResolutionPath;
import mb.nabl2.scopegraph.terms.Label;
import mb.nabl2.scopegraph.terms.Occurrence;
import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.scopegraph.terms.path.Paths;
import mb.nabl2.solver.ASolver;
import mb.nabl2.solver.ISolver.SeedResult;
import mb.nabl2.solver.ISolver.SolveResult;
import mb.nabl2.solver.ImmutableSolveResult;
import mb.nabl2.solver.SolverCore;
import mb.nabl2.solver.TypeException;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.collection.VarMultimap;
import mb.nabl2.util.collections.IProperties;

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
        nameResolution.addCached(solution.resolutionCache());
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
    }

    public NameResolutionResult finish() {
        return ImmutableNameResolutionResult.of(scopeGraph.freeze(), nameResolution.toCache(), properties.freeze());
    }

    public IProperties.Immutable<Occurrence, ITerm, ITerm> finishDeclProperties() {
        return properties.freeze();
    }

    // ------------------------------------------------------------------------------------------------------//

    private Optional<SolveResult> solve(CResolve r) {
        final ITerm refTerm = r.getReference();
        if(!unifier().isGround(refTerm)) {
            return Optional.empty();
        }
        final Occurrence ref = Occurrence.matcher().match(refTerm, unifier())
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + r));
        final java.util.Set<IResolutionPath<Scope, Label, Occurrence>> paths;
        try {
            paths = nameResolution.resolve(ref);
        } catch(CriticalEdgeException e) {
            return Optional.empty();
        }
        final Set<Occurrence> declarations = Sets.newHashSet(Paths.resolutionPathsToDecls(paths));
        final SolveResult result;
        switch(declarations.size()) {
            case 0: {
                IMessageInfo message = r.getMessageInfo()
                        .withDefaultContent(MessageContent.builder().append(ref).append(" does not resolve.").build());
                result = SolveResult.messages(message);
                break;
            }
            case 1: {
                final Occurrence decl = Iterables.getOnlyElement(declarations);
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
        return Optional.of(ImmutableSolveResult.copyOf(result));
    }

    private Optional<SolveResult> solve(CAssoc a) {
        final ITerm declTerm = a.getDeclaration();
        if(!unifier().isGround(declTerm)) {
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
        final ITerm declTerm = c.getDeclaration();
        if(!unifier().isGround(declTerm)) {
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
        return Optional.of(scopeTerm).filter(unifier()::isGround).map(st -> Scope.matcher().match(st, unifier())
                .orElseThrow(() -> new TypeException("Expected a scope, got " + st)));
    }

    private Optional<Occurrence> findOccurrence(ITerm occurrenceTerm) {
        return Optional.of(occurrenceTerm).filter(unifier()::isGround).map(ot -> Occurrence.matcher()
                .match(ot, unifier()).orElseThrow(() -> new TypeException("Expected an occurrence, got " + ot)));
    }

    @Value.Immutable
    @Serial.Version(42l)
    public static abstract class NameResolutionResult {

        @Value.Parameter public abstract IEsopScopeGraph.Immutable<Scope, Label, Occurrence, ITerm> scopeGraph();

        @Value.Parameter public abstract IEsopNameResolution.ResolutionCache<Scope, Label, Occurrence>
                resolutionCache();

        @Value.Parameter public abstract IProperties.Immutable<Occurrence, ITerm, ITerm> declProperties();

    }

}