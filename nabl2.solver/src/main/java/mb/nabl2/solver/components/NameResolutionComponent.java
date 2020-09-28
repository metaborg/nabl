package mb.nabl2.solver.components;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.equality.CEqual;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.constraints.messages.MessageContent;
import mb.nabl2.constraints.nameresolution.CAssoc;
import mb.nabl2.constraints.nameresolution.CDeclProperty;
import mb.nabl2.constraints.nameresolution.CResolve;
import mb.nabl2.constraints.nameresolution.INameResolutionConstraint;
import mb.nabl2.scopegraph.CriticalEdgeException;
import mb.nabl2.scopegraph.StuckException;
import mb.nabl2.scopegraph.esop.IEsopNameResolution;
import mb.nabl2.scopegraph.esop.IEsopScopeGraph;
import mb.nabl2.scopegraph.path.IResolutionPath;
import mb.nabl2.scopegraph.terms.Label;
import mb.nabl2.scopegraph.terms.Occurrence;
import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.scopegraph.terms.path.Paths;
import mb.nabl2.solver.ASolver;
import mb.nabl2.solver.SeedResult;
import mb.nabl2.solver.SolveResult;
import mb.nabl2.solver.SolverCore;
import mb.nabl2.solver.TypeException;
import mb.nabl2.solver.exceptions.CriticalEdgeDelayException;
import mb.nabl2.solver.exceptions.DelayException;
import mb.nabl2.solver.exceptions.InterruptedDelayException;
import mb.nabl2.solver.exceptions.VariableDelayException;
import mb.nabl2.terms.ITerm;
import mb.nabl2.util.collections.IProperties;

public class NameResolutionComponent extends ASolver {

    private final IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph;
    private final IEsopNameResolution<Scope, Label, Occurrence> nameResolution;
    private final IProperties.Transient<Occurrence, ITerm, ITerm> properties;

    public NameResolutionComponent(SolverCore core,
            IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph,
            IEsopNameResolution<Scope, Label, Occurrence> nameResolution,
            IProperties.Transient<Occurrence, ITerm, ITerm> initial) {
        super(core);
        this.scopeGraph = scopeGraph;
        this.nameResolution = nameResolution;
        this.properties = initial;
    }

    // ------------------------------------------------------------------------------------------------------//

    public SeedResult seed(NameResolutionResult solution, IMessageInfo message) throws InterruptedException {
        final java.util.Set<IConstraint> constraints = Sets.newHashSet();
        scopeGraph.addAll(solution.scopeGraph(), unifier()::getVars);
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

    public SolveResult solve(INameResolutionConstraint constraint) throws DelayException {
        return constraint
                .matchOrThrow(INameResolutionConstraint.CheckedCases.of(this::solve, this::solve, this::solve));
    }

    public NameResolutionResult finish() {
        return NameResolutionResult.of(scopeGraph.freeze(), nameResolution.toCache(), properties.freeze());
    }

    public IProperties.Immutable<Occurrence, ITerm, ITerm> finishDeclProperties() {
        return properties.freeze();
    }

    // ------------------------------------------------------------------------------------------------------//

    private SolveResult solve(CResolve r) throws DelayException {
        final ITerm refTerm = r.getReference();
        if(!unifier().isGround(refTerm)) {
            throw new VariableDelayException(unifier().getVars(refTerm));
        }
        final Occurrence ref = Occurrence.matcher().match(refTerm, unifier())
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + r));
        final Collection<IResolutionPath<Scope, Label, Occurrence>> paths;
        try {
            paths = nameResolution.resolve(ref);
        } catch(InterruptedException e) {
            throw new InterruptedDelayException(e);
        } catch(CriticalEdgeException e) {
            throw new CriticalEdgeDelayException(e);
        } catch(StuckException e) {
            IMessageInfo message = r.getMessageInfo().withDefaultContent(
                    MessageContent.builder().append("Resolution of ").append(ref).append(" is stuck.").build());
            return SolveResult.messages(message);
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
                result = SolveResult.constraints(CEqual.of(r.getDeclaration(), decl, r.getMessageInfo()));
                break;
            }
            default: {
                IMessageInfo message = r.getMessageInfo().withDefaultContent(
                        MessageContent.builder().append("Resolution of ").append(ref).append(" is ambiguous.").build());
                result = SolveResult.messages(message);
                break;
            }
        }
        return SolveResult.copyOf(result);
    }

    private SolveResult solve(CAssoc a) throws DelayException {
        final ITerm declTerm = a.getDeclaration();
        if(!unifier().isGround(declTerm)) {
            throw new VariableDelayException(unifier().getVars(declTerm));
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
                result = SolveResult.constraints(CEqual.of(a.getScope(), scopes.get(0), a.getMessageInfo()));
                break;
            }
            default: {
                IMessageInfo message = a.getMessageInfo().withDefaultContent(MessageContent.builder().append(decl)
                        .append(" has multiple ").append(label).append(" associated scope.").build());
                result = SolveResult.messages(message);
                break;
            }
        }
        return result;
    }

    private SolveResult solve(CDeclProperty c) throws DelayException {
        final ITerm declTerm = c.getDeclaration();
        if(!unifier().isGround(declTerm)) {
            throw new VariableDelayException(unifier().getVars(declTerm));
        }
        final Occurrence decl = Occurrence.matcher().match(declTerm, unifier())
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + c));
        final SolveResult result = putProperty(decl, c.getKey(), c.getValue(), c.getMessageInfo())
                .map(cc -> SolveResult.constraints(cc)).orElseGet(() -> SolveResult.empty());
        return result;
    }

    private Optional<IConstraint> putProperty(Occurrence decl, ITerm key, ITerm value, IMessageInfo message) {
        Optional<ITerm> prev = properties.getValue(decl, key);
        if(!prev.isPresent()) {
            properties.putValue(decl, key, value);
            return Optional.empty();
        } else {
            return Optional.of(CEqual.of(value, prev.get(), message));
        }

    }

    public Optional<ITerm> getProperty(Occurrence decl, ITerm key) {
        return properties.getValue(decl, key);
    }

    @Value.Immutable
    @Serial.Version(42l)
    public static abstract class ANameResolutionResult {

        @Value.Parameter public abstract IEsopScopeGraph.Immutable<Scope, Label, Occurrence, ITerm> scopeGraph();

        @Value.Parameter public abstract IEsopNameResolution.IResolutionCache<Scope, Label, Occurrence>
                resolutionCache();

        @Value.Parameter public abstract IProperties.Immutable<Occurrence, ITerm, ITerm> declProperties();

    }

}