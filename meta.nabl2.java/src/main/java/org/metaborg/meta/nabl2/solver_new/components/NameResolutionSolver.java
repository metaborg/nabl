package org.metaborg.meta.nabl2.solver_new.components;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.constraints.nameresolution.CAssoc;
import org.metaborg.meta.nabl2.constraints.nameresolution.CDeclProperty;
import org.metaborg.meta.nabl2.constraints.nameresolution.CResolve;
import org.metaborg.meta.nabl2.constraints.nameresolution.INameResolutionConstraint;
import org.metaborg.meta.nabl2.scopegraph.IActiveScopes;
import org.metaborg.meta.nabl2.scopegraph.INameResolution;
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
import org.metaborg.meta.nabl2.solver.TypeException;
import org.metaborg.meta.nabl2.solver_new.ASolver;
import org.metaborg.meta.nabl2.solver_new.IIncompleteScopeGraph;
import org.metaborg.meta.nabl2.solver_new.IncompleteScopeGraph;
import org.metaborg.meta.nabl2.solver_new.SolverCore;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.util.collections.IRelation3;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;

public class NameResolutionSolver extends ASolver<INameResolutionConstraint, NameResolutionSolver.NameResolutionResult>
        implements IActiveScopes<Scope, Label> {
    private static final ILogger logger = LoggerUtils.logger(NameResolutionSolver.class);

    private final Set.Immutable<Scope> openScopes;
    private final ResolutionParameters params;

    private IEsopScopeGraph.Builder<Scope, Label, Occurrence> scopeGraph;
    private final IRelation3.Mutable<Scope, Label, ITerm> incompleteDirectEdges;
    private final IRelation3.Mutable<Scope, Label, ITerm> incompleteImportEdges;
    private IEsopNameResolution<Scope, Label, Occurrence> nameResolution;

    private final Tracer tracer;
    private final SetMultimap.Transient<String, String> strongDeps;
    private final SetMultimap.Transient<String, String> weakDeps;

    private final IProperties.Mutable<Occurrence> properties;
    private final java.util.Set<INameResolutionConstraint> constraints;


    public NameResolutionSolver(SolverCore core, IIncompleteScopeGraph<Scope, Label, Occurrence> scopeGraph,
            ResolutionParameters params, Set.Immutable<Scope> openScopes) {
        super(core);
        this.openScopes = openScopes;
        this.params = params;
        this.tracer = new Tracer();
        this.scopeGraph = IEsopScopeGraph.builder(scopeGraph);
        this.incompleteDirectEdges = scopeGraph.incompleteDirectEdges().copyOf();
        this.incompleteImportEdges = scopeGraph.incompleteImportEdges().copyOf();
        this.nameResolution = scopeGraph.resolve(params, this, tracer);
        this.strongDeps = SetMultimap.Transient.of();
        this.weakDeps = SetMultimap.Transient.of();
        this.properties = Properties.Mutable.of();
        this.constraints = Sets.newHashSet();
    }

    // ------------------------------------------------------------------------------------------------------//

    @Override public boolean add(INameResolutionConstraint constraint) throws InterruptedException {
        return constraint.match(INameResolutionConstraint.Cases.of(this::add, this::add, this::add));
    }

    @Override public boolean iterate() throws InterruptedException {
        boolean progress = false;
        if(doIterate(incompleteDirectEdges, this::solveDirectEdge)
                || doIterate(incompleteImportEdges, this::solveImportEdge)) {
            IEsopScopeGraph<Scope, Label, Occurrence> newGraph = scopeGraph.result();
            nameResolution = newGraph.resolve(params, this, tracer);
            scopeGraph = IEsopScopeGraph.builder(newGraph);
            progress |= true;
        }
        progress |= doIterate(constraints, this::solve);
        return progress;
    }

    @Override public NameResolutionResult finish() {
        return ImmutableNameResolutionResult.of(
                new IncompleteScopeGraph(scopeGraph.result(), incompleteDirectEdges, incompleteImportEdges),
                nameResolution, strongDeps.freeze(), weakDeps.freeze(), properties.freeze(), constraints);
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean add(CResolve constraint) {
        tracker().addActive(constraint.getDeclaration(), constraint);
        if(!solve(constraint)) {
            return constraints.add(constraint);
        } else {
            work();
            return true;
        }
    }

    private boolean add(CAssoc constraint) {
        tracker().addActive(constraint.getScope(), constraint);
        if(!solve(constraint)) {
            return constraints.add(constraint);
        } else {
            work();
            return true;
        }
    }

    private boolean add(CDeclProperty constraint) {
        tracker().addActive(constraint.getValue(), constraint);
        if(!solve(constraint)) {
            return constraints.add(constraint);
        } else {
            work();
            return true;
        }
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean solve(INameResolutionConstraint constraint) {
        return constraint.match(INameResolutionConstraint.Cases.of(this::solve, this::solve, this::solve));
    }

    private boolean solve(CResolve r) {
        ITerm refTerm = find(r.getReference());
        if(!refTerm.isGround()) {
            return false;
        }
        final Occurrence ref = Occurrence.matcher().match(refTerm)
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + r));
        final Optional<Tuple2<Set.Immutable<IResolutionPath<Scope, Label, Occurrence>>, Set.Immutable<String>>> maybePathsAndDeps =
                nameResolution.tryResolve(ref);
        if(maybePathsAndDeps.isPresent()) {
            tracker().removeActive(r.getDeclaration(), r);
            final Set.Immutable<IResolutionPath<Scope, Label, Occurrence>> paths = maybePathsAndDeps.get()._1();
            final String refResource = ref.getIndex().getResource();
            final Set.Immutable<String> deps = maybePathsAndDeps.get()._2().__remove(refResource);
            List<Occurrence> declarations = Paths.resolutionPathsToDecls(paths);
            switch(declarations.size()) {
                case 0:
                    addMessage(r.getMessageInfo().withDefaultContent(
                            MessageContent.builder().append(ref).append(" does not resolve.").build()));
                    break;
                case 1:
                    final Occurrence decl = declarations.get(0);
                    unify(r.getDeclaration(), decl, r.getMessageInfo());
                    final String declResource = decl.getIndex().getResource();
                    if(!refResource.equals(declResource)) {
                        strongDeps.__insert(refResource, declResource);
                    }
                    break;
                default:
                    addMessage(r.getMessageInfo().withDefaultContent(MessageContent.builder().append("Resolution of ")
                            .append(ref).append(" is ambiguous.").build()));
                    break;
            }
            weakDeps.__insert(refResource, deps);
            return true;
        } else {
            return false;
        }
    }

    private boolean solve(CAssoc a) {
        ITerm declTerm = find(a.getDeclaration());
        if(!declTerm.isGround()) {
            return false;
        }
        Occurrence decl = Occurrence.matcher().match(declTerm)
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + a));
        Label label = a.getLabel();
        List<Scope> scopes = Lists.newArrayList(scopeGraph.getExportEdges().get(decl, label));
        tracker().removeActive(a.getScope(), a);
        switch(scopes.size()) {
            case 0:
                addMessage(a.getMessageInfo().withDefaultContent(MessageContent.builder().append(decl)
                        .append(" has no ").append(label).append(" associated scope.").build()));
                break;
            case 1:
                unify(a.getScope(), scopes.get(0), a.getMessageInfo());
                break;
            default:
                addMessage(a.getMessageInfo().withDefaultContent(MessageContent.builder().append(decl)
                        .append(" has multiple ").append(label).append(" associated scope.").build()));
                break;
        }
        return true;
    }

    private boolean solve(CDeclProperty c) {
        ITerm declTerm = find(c.getDeclaration());
        if(!declTerm.isGround()) {
            return false;
        }
        Occurrence decl = Occurrence.matcher().match(declTerm)
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + c));
        tracker().removeActive(c.getValue(), c);
        Optional<ITerm> prev = properties.putValue(decl, c.getKey(), c.getValue());
        if(prev.isPresent()) {
            unify(c.getValue(), prev.get(), c.getMessageInfo());
        }
        return true;
    }

    private boolean solveDirectEdge(Scope sourceScope, Label label, ITerm targetScopeTerm) {
        ITerm targetScopeRep = find(targetScopeTerm);
        if(!targetScopeRep.isGround()) {
            return false;
        }
        Scope targetScope = Scope.matcher().match(targetScopeRep)
                .orElseThrow(() -> new TypeException("Expected a scope but got " + targetScopeRep));
        scopeGraph.addDirectEdge(sourceScope, label, targetScope);
        return true;
    }

    private boolean solveImportEdge(Scope scope, Label label, ITerm refTerm) {
        ITerm refRep = find(refTerm);
        if(!refRep.isGround()) {
            return false;
        }
        Occurrence ref = Occurrence.matcher().match(refRep)
                .orElseThrow(() -> new TypeException("Expected an occurrence, but got " + refRep));
        scopeGraph.addImport(scope, label, ref);
        return true;
    }

    // ------------------------------------------------------------------------------------------------------//

    public IMatcher<java.util.Set<IElement<ITerm>>> nameSets() {
        return term -> {
            return M.<Optional<java.util.Set<IElement<ITerm>>>>cases(
                // @formatter:off
                M.appl2("Declarations", Scope.matcher(), Namespace.matcher(), (t, scope, ns) -> {
                    Iterable<Occurrence> decls = NameResolutionSolver.this.scopeGraph.getDecls().inverse().get(scope);
                    return Optional.of(makeSet(decls, ns));
                }),
                M.appl2("References", Scope.matcher(), Namespace.matcher(), (t, scope, ns) -> {
                    Iterable<Occurrence> refs = NameResolutionSolver.this.scopeGraph.getRefs().inverse().get(scope);
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

    private static class Tracer implements Function1<Scope, String>, Serializable {

        private static final long serialVersionUID = 42L;

        public String apply(Scope s) {
            return s.getResource();
        }

    }

    private void logDep(String string, Iterable<String> resources) {
        logger.info("{} => {}", string, resources);
    }

    public boolean isComplete() {
        throw new IllegalStateException("isComplete should not be used here.");
    }

    public boolean isOpen(Scope scope, Label label) {
        return openScopes.contains(scope) || incompleteDirectEdges.contains(scope, label)
                || incompleteImportEdges.contains(scope, label);
    }

    // ------------------------------------------------------------------------------------------------------//

    @Value.Immutable
    @Serial.Version(42L)
    public static abstract class NameResolutionResult {

        @Value.Parameter public abstract IIncompleteScopeGraph<Scope, Label, Occurrence> scopeGraph();

        @Value.Parameter public abstract INameResolution<Scope, Label, Occurrence> nameResolution();

        @Value.Parameter public abstract SetMultimap.Immutable<String, String> strongDependencies();

        @Value.Parameter public abstract SetMultimap.Immutable<String, String> weakDependencies();

        @Value.Parameter public abstract IProperties.Immutable<Occurrence> properties();

        @Value.Parameter public abstract java.util.Set<INameResolutionConstraint> residualConstraints();

    }

}