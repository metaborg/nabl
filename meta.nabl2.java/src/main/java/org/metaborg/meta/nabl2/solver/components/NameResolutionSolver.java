package org.metaborg.meta.nabl2.solver.components;

import static org.metaborg.meta.nabl2.util.Unit.unit;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.constraints.nameresolution.CAssoc;
import org.metaborg.meta.nabl2.constraints.nameresolution.CDeclProperty;
import org.metaborg.meta.nabl2.constraints.nameresolution.CResolve;
import org.metaborg.meta.nabl2.constraints.nameresolution.INameResolutionConstraint;
import org.metaborg.meta.nabl2.constraints.nameresolution.INameResolutionConstraint.CheckedCases;
import org.metaborg.meta.nabl2.constraints.nameresolution.ImmutableCDeclProperty;
import org.metaborg.meta.nabl2.scopegraph.INameResolution;
import org.metaborg.meta.nabl2.scopegraph.OpenCounter;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.path.IDeclPath;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Namespace;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.ResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.scopegraph.terms.path.Paths;
import org.metaborg.meta.nabl2.sets.IElement;
import org.metaborg.meta.nabl2.solver.IProperties;
import org.metaborg.meta.nabl2.solver.Properties;
import org.metaborg.meta.nabl2.solver.Solver;
import org.metaborg.meta.nabl2.solver.SolverComponent;
import org.metaborg.meta.nabl2.solver.TypeException;
import org.metaborg.meta.nabl2.solver.UnsatisfiableException;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.util.Unit;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Set.Immutable;

public class NameResolutionSolver extends SolverComponent<INameResolutionConstraint> {
    private static final ILogger logger = LoggerUtils.logger(NameResolutionSolver.class);

    private final ResolutionParameters params;
    private final IEsopScopeGraph.Builder<Scope, Label, Occurrence> scopeGraphBuilder;
    private final IProperties.Mutable<Occurrence> properties;
    private final OpenCounter<Scope, Label> scopeCounter;

    private final Set<INameResolutionConstraint> unsolved;

    private IEsopNameResolution<Scope, Label, Occurrence> nameResolution = null;

    public NameResolutionSolver(Solver solver, ResolutionParameters params,
            IEsopScopeGraph.Builder<Scope, Label, Occurrence> scopeGraph, OpenCounter<Scope, Label> scopeCounter) {
        super(solver);
        this.params = params;
        this.scopeGraphBuilder = scopeGraph;
        this.scopeCounter = scopeCounter;
        this.properties = Properties.Mutable.of();

        this.unsolved = Sets.newHashSet();
    }

    public INameResolution<Scope, Label, Occurrence> getNameResolution() {
        return nameResolution != null ? nameResolution : INameResolution.empty();
    }

    public IProperties<Occurrence> getProperties() {
        return properties.freeze();
    }

    public void addActive(Iterable<Scope> scopes) {
        for(Scope scope : scopes) {
            scopeCounter.addAll(scope, params.getLabels().symbols());
        }
    }

    // ------------------------------------------------------------------------------------------------------//

    @Override protected Unit doAdd(INameResolutionConstraint constraint) throws UnsatisfiableException {
        if(isResolutionStarted()) {
            throw new IllegalStateException("Adding constraints after resolution has started.");
        }
        return constraint.matchOrThrow(CheckedCases.of(this::add, this::add, this::add));
    }

    @Override protected boolean doIterate() throws UnsatisfiableException, InterruptedException {
        boolean progress = false;
        if(!isResolutionStarted() && scopeCounter.isComplete()) {
            progress |= true;
            scopeCounter.setComplete();
            nameResolution = scopeGraphBuilder.result().resolve(params, scopeCounter, new Tracer());
        }
        progress |= doIterate(unsolved, this::solve);
        return progress;
    }

    @Override protected Set<? extends INameResolutionConstraint> doFinish(IMessageInfo messageInfo) {
        Set<INameResolutionConstraint> constraints = Sets.newHashSet();
        if(isPartial()) {
            addNameResolutionConstraints(constraints, messageInfo);
        }
        constraints.addAll(unsolved);
        return constraints;
    }

    // ------------------------------------------------------------------------------------------------------//

    private Unit add(CResolve constraint) throws UnsatisfiableException {
        tracker().addActive(constraint.getDeclaration(), constraint);
        if(!solve(constraint)) {
            unsolved.add(constraint);
        } else {
            work();
        }
        return unit;
    }

    private Unit add(CAssoc constraint) throws UnsatisfiableException {
        tracker().addActive(constraint.getScope(), constraint);
        if(!solve(constraint)) {
            unsolved.add(constraint);
        } else {
            work();
        }
        return unit;
    }

    private Unit add(CDeclProperty constraint) throws UnsatisfiableException {
        tracker().addActive(constraint.getValue(), constraint);
        if(!solve(constraint)) {
            unsolved.add(constraint);
        } else {
            work();
        }
        return unit;
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean solve(INameResolutionConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(CheckedCases.of(this::solve, this::solve, this::solve));
    }

    private boolean solve(CResolve r) throws UnsatisfiableException {
        if(!isResolutionStarted()) {
            return false;
        }
        ITerm refTerm = find(r.getReference());
        if(!refTerm.isGround()) {
            return false;
        }
        Occurrence ref = Occurrence.matcher().match(refTerm)
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + r));
        Optional<? extends io.usethesource.capsule.Set.Immutable<IResolutionPath<Scope, Label, Occurrence>>> paths =
                nameResolution.tryResolve(ref).map(pts -> {
                    logDep(ref.getIndex().getResource(), pts._2());
                    return pts._1();
                });
        if(paths.isPresent()) {
            List<Occurrence> declarations = Paths.resolutionPathsToDecls(paths.get());
            tracker().removeActive(r.getDeclaration(), r); // before `unify`, so that we don't cause an error chain if
                                                           // that fails
            switch(declarations.size()) {
                case 0:
                    throw new UnsatisfiableException(r.getMessageInfo().withDefaultContent(
                            MessageContent.builder().append(ref).append(" does not resolve.").build()));
                case 1:
                    unify(r.getDeclaration(), declarations.get(0), r.getMessageInfo());
                    return true;
                default:
                    throw new UnsatisfiableException(r.getMessageInfo().withDefaultContent(MessageContent.builder()
                            .append("Resolution of ").append(ref).append(" is ambiguous.").build()));
            }
        } else {
            return false;
        }
    }

    private boolean solve(CAssoc a) throws UnsatisfiableException {
        if(!isResolutionStarted()) {
            return false;
        }
        ITerm declTerm = find(a.getDeclaration());
        if(!declTerm.isGround()) {
            return false;
        }
        Occurrence decl = Occurrence.matcher().match(declTerm)
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + a));
        Label label = a.getLabel();
        List<Scope> scopes = Lists.newArrayList(scopeGraphBuilder.getExportEdges().get(decl, label));
        tracker().removeActive(a.getScope(), a); // before `unify`, so that we don't cause an error chain if that fails
        switch(scopes.size()) {
            case 0:
                throw new UnsatisfiableException(a.getMessageInfo().withDefaultContent(MessageContent.builder()
                        .append(decl).append(" has no ").append(label).append(" associated scope.").build()));
            case 1:
                unify(a.getScope(), scopes.get(0), a.getMessageInfo());
                return true;
            default:
                throw new UnsatisfiableException(a.getMessageInfo().withDefaultContent(MessageContent.builder()
                        .append(decl).append(" has multiple ").append(label).append(" associated scope.").build()));
        }
    }

    private boolean solve(CDeclProperty c) throws UnsatisfiableException {
        ITerm declTerm = find(c.getDeclaration());
        if(!declTerm.isGround()) {
            return false;
        }
        Occurrence decl = Occurrence.matcher().match(declTerm)
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + c));
        tracker().removeActive(c.getValue(), c); // before `unify`, so that we don't cause an error chain if that fails
        Optional<ITerm> prev = properties.putValue(decl, c.getKey(), c.getValue());
        if(prev.isPresent()) {
            unify(c.getValue(), prev.get(), c.getMessageInfo());
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean isResolutionStarted() {
        return nameResolution != null;
    }

    private void addNameResolutionConstraints(Set<INameResolutionConstraint> constraints, IMessageInfo messageInfo) {
        for(Occurrence decl : properties.getIndices()) {
            for(ITerm key : properties.getDefinedKeys(decl)) {
                properties.getValue(decl, key).ifPresent(value -> {
                    constraints.add(ImmutableCDeclProperty.of(decl, key, value, 0, messageInfo));
                });
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------//

    public IMatcher<Set<IElement<ITerm>>> nameSets() {
        return term -> {
            if(!isResolutionStarted()) {
                return Optional.empty();
            }
            return M.<Optional<Set<IElement<ITerm>>>>cases(
                // @formatter:off
                M.appl2("Declarations", Scope.matcher(), Namespace.matcher(), (t, scope, ns) -> {
                    Iterable<Occurrence> decls = NameResolutionSolver.this.scopeGraphBuilder.getDecls().inverse().get(scope);
                    return Optional.of(makeSet(decls, ns));
                }),
                M.appl2("References", Scope.matcher(), Namespace.matcher(), (t, scope, ns) -> {
                    Iterable<Occurrence> refs = NameResolutionSolver.this.scopeGraphBuilder.getRefs().inverse().get(scope);
                    return Optional.of(makeSet(refs, ns));
                }),
                M.appl2("Visibles", Scope.matcher(), Namespace.matcher(), (t, scope, ns) -> {
                    Optional<? extends io.usethesource.capsule.Set<IDeclPath<Scope,Label,Occurrence>>> paths =
                            NameResolutionSolver.this.nameResolution.tryVisible(scope).map(pts -> {
                                logDep(scope.getResource(), pts._2());
                                return pts._1();
                            });
                    return paths.map(ps -> makeSet(Paths.declPathsToDecls(ps), ns));
                }),
                M.appl2("Reachables", Scope.matcher(), Namespace.matcher(), (t, scope, ns) -> {
                    Optional<? extends io.usethesource.capsule.Set<IDeclPath<Scope,Label,Occurrence>>> paths =
                            NameResolutionSolver.this.nameResolution.tryReachable(scope).map(pts -> {
                                logDep(scope.getResource(), pts._2());
                                return pts._1();
                            });
                    return paths.map(ps -> makeSet(Paths.declPathsToDecls(ps), ns));
                })
                // @formatter:on
            ).match(term).flatMap(o -> o);
        };
    }

    private Set<IElement<ITerm>> makeSet(Iterable<Occurrence> occurrences, Namespace namespace) {
        Set<IElement<ITerm>> result = Sets.newHashSet();
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

    private static void logDep(String resource, io.usethesource.capsule.Set.Immutable<String> dependencies) {
        final Immutable<String> realDeps = dependencies.__remove(resource);
        if(!realDeps.isEmpty()) {
            logger.info("DEP: {} -> {}", resource, realDeps);
        }
    }

    private static class Tracer implements Function1<Scope, String>, Serializable {

        private static final long serialVersionUID = 42L;

        public String apply(Scope s) {
            return s.getResource();
        }

    }

}