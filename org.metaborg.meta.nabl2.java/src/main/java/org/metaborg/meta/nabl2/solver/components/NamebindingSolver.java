package org.metaborg.meta.nabl2.solver.components;

import static org.metaborg.meta.nabl2.util.Unit.unit;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.constraints.namebinding.CAssoc;
import org.metaborg.meta.nabl2.constraints.namebinding.CDeclProperty;
import org.metaborg.meta.nabl2.constraints.namebinding.CGDecl;
import org.metaborg.meta.nabl2.constraints.namebinding.CGDirectEdge;
import org.metaborg.meta.nabl2.constraints.namebinding.CGExportEdge;
import org.metaborg.meta.nabl2.constraints.namebinding.CGImportEdge;
import org.metaborg.meta.nabl2.constraints.namebinding.CGRef;
import org.metaborg.meta.nabl2.constraints.namebinding.CResolve;
import org.metaborg.meta.nabl2.constraints.namebinding.INamebindingConstraint;
import org.metaborg.meta.nabl2.constraints.namebinding.INamebindingConstraint.CheckedCases;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCDeclProperty;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCGDecl;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCGDirectEdge;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCGExportEdge;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCGImportEdge;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCGRef;
import org.metaborg.meta.nabl2.scopegraph.INameResolution;
import org.metaborg.meta.nabl2.scopegraph.IScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.OpenCounter;
import org.metaborg.meta.nabl2.scopegraph.esop.EsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.EsopScopeGraph;
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
import org.metaborg.meta.nabl2.unification.UnificationException;
import org.metaborg.meta.nabl2.util.Unit;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class NamebindingSolver extends SolverComponent<INamebindingConstraint> {

    private final ResolutionParameters params;
    private final EsopScopeGraph<Scope, Label, Occurrence> scopeGraph;
    private final Properties<Occurrence> properties;

    private final Set<INamebindingConstraint> unsolvedBuilds;
    private final Set<CGDirectEdge<Scope>> incompleteDirectEdges;
    private final Set<CGImportEdge<Scope>> incompleteImportEdges;
    private final Set<INamebindingConstraint> unsolvedChecks;

    private final OpenCounter<Scope, Label> scopeCounter;

    private EsopNameResolution<Scope, Label, Occurrence> nameResolution = null;

    public NamebindingSolver(Solver solver, ResolutionParameters params) {
        super(solver);
        this.params = params;
        this.scopeGraph = new EsopScopeGraph<>();
        this.properties = new Properties<>();

        this.unsolvedBuilds = Sets.newHashSet();
        this.incompleteDirectEdges = Sets.newHashSet();
        this.incompleteImportEdges = Sets.newHashSet();
        this.unsolvedChecks = Sets.newHashSet();

        this.scopeCounter = new OpenCounter<>();
    }

    public IScopeGraph<Scope, Label, Occurrence> getScopeGraph() {
        return scopeGraph;
    }

    public INameResolution<Scope, Label, Occurrence> getNameResolution() {
        return nameResolution != null ? nameResolution : INameResolution.empty();
    }

    public IProperties<Occurrence> getProperties() {
        return properties;
    }

    public void addActive(Iterable<Scope> scopes) {
        for(Scope scope : scopes) {
            scopeCounter.addAll(scope, params.getLabels().symbols());
        }
    }

    // ------------------------------------------------------------------------------------------------------//

    @Override protected Unit doAdd(INamebindingConstraint constraint) throws UnsatisfiableException {
        if(isResolutionStarted()) {
            throw new IllegalStateException("Adding constraints after resolution has started.");
        }
        return constraint.matchOrThrow(CheckedCases.of(this::addBuild, this::addBuild, this::addBuild, this::addBuild,
                this::addBuild, this::add, this::add, this::add));
    }

    @Override protected boolean doIterate() throws UnsatisfiableException, InterruptedException {
        boolean progress = false;
        progress |= doIterate(unsolvedBuilds, this::solve);
        progress |= doIterate(incompleteDirectEdges, this::solveDirectEdge);
        progress |= doIterate(incompleteImportEdges, this::solveImportEdge);
        if(!isResolutionStarted() && unsolvedBuilds.isEmpty()) {
            progress |= true;
            scopeCounter.setComplete();
            nameResolution = new EsopNameResolution<>(scopeGraph, params, scopeCounter);
        }
        progress |= doIterate(unsolvedChecks, this::solve);
        return progress;
    }

    @Override protected Set<? extends INamebindingConstraint> doFinish(IMessageInfo messageInfo) {
        Set<INamebindingConstraint> constraints = Sets.newHashSet();
        if(isPartial()) {
            addScopeGraphConstraints(constraints, messageInfo);
        }
        constraints.addAll(unsolvedBuilds);
        constraints.addAll(unsolvedChecks);
        constraints.addAll(incompleteDirectEdges);
        constraints.addAll(incompleteImportEdges);
        return constraints;
    }

    // ------------------------------------------------------------------------------------------------------//

    private Unit addBuild(INamebindingConstraint constraint) throws UnsatisfiableException {
        if(!solve(constraint)) {
            unsolvedBuilds.add(constraint);
        } else {
            work();
        }
        return unit;
    }

    private Unit add(CResolve constraint) throws UnsatisfiableException {
        unifier().addActive(constraint.getDeclaration(), constraint);
        if(!solve(constraint)) {
            unsolvedChecks.add(constraint);
        } else {
            work();
        }
        return unit;
    }

    private Unit add(CAssoc constraint) throws UnsatisfiableException {
        unifier().addActive(constraint.getScope(), constraint);
        if(!solve(constraint)) {
            unsolvedChecks.add(constraint);
        } else {
            work();
        }
        return unit;
    }

    private Unit add(CDeclProperty constraint) throws UnsatisfiableException {
        unifier().addActive(constraint.getValue(), constraint);
        if(!solve(constraint)) {
            unsolvedChecks.add(constraint);
        } else {
            work();
        }
        return unit;
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean solve(INamebindingConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(CheckedCases.of(this::solve, this::solve, this::solve, this::solve, this::solve,
                this::solve, this::solve, this::solve));
    }

    private boolean solve(CGDecl c) {
        ITerm scopeTerm = unifier().find(c.getScope());
        ITerm declTerm = unifier().find(c.getDeclaration());
        if(!(scopeTerm.isGround() && declTerm.isGround())) {
            return false;
        }
        Scope scope = Scope.matcher().match(scopeTerm)
                .orElseThrow(() -> new TypeException("Expected a scope as first agument to " + c));
        Occurrence decl = Occurrence.matcher().match(declTerm)
                .orElseThrow(() -> new TypeException("Expected an occurrence as second argument to " + c));
        scopeGraph.addDecl(scope, decl);
        return true;
    }

    private boolean solve(CGRef c) {
        ITerm scopeTerm = unifier().find(c.getScope());
        ITerm refTerm = unifier().find(c.getReference());
        if(!(scopeTerm.isGround() && refTerm.isGround())) {
            return false;
        }
        Occurrence ref = Occurrence.matcher().match(refTerm)
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + c));
        Scope scope = Scope.matcher().match(scopeTerm)
                .orElseThrow(() -> new TypeException("Expected a scope as second argument to " + c));
        scopeGraph.addRef(ref, scope);
        return true;
    }

    private boolean solve(CGDirectEdge<?> c) {
        ITerm sourceScopeTerm = unifier().find(c.getSourceScope());
        if(!sourceScopeTerm.isGround()) {
            return false;
        }
        Scope sourceScope = Scope.matcher().match(sourceScopeTerm)
                .orElseThrow(() -> new TypeException("Expected a scope as first argument to " + c));
        scopeCounter.add(sourceScope, c.getLabel());
        CGDirectEdge<Scope> cc =
                ImmutableCGDirectEdge.of(sourceScope, c.getLabel(), c.getTargetScope(), c.getMessageInfo());
        if(!solveDirectEdge(cc)) {
            incompleteDirectEdges.add(cc);
        }
        return true;
    }

    private boolean solve(CGImportEdge<?> c) {
        ITerm scopeTerm = unifier().find(c.getScope());
        if(!scopeTerm.isGround()) {
            return false;
        }
        Scope scope = Scope.matcher().match(scopeTerm)
                .orElseThrow(() -> new TypeException("Expected a scope as first argument to " + c));
        scopeCounter.add(scope, c.getLabel());
        CGImportEdge<Scope> cc = ImmutableCGImportEdge.of(scope, c.getLabel(), c.getReference(), c.getMessageInfo());
        if(!solveImportEdge(cc)) {
            incompleteImportEdges.add(cc);

        }
        return true;
    }

    private boolean solve(CGExportEdge c) {
        ITerm scopeTerm = unifier().find(c.getScope());
        ITerm declTerm = unifier().find(c.getDeclaration());
        if(!(scopeTerm.isGround() && declTerm.isGround())) {
            return false;
        }
        Scope scope = Scope.matcher().match(scopeTerm)
                .orElseThrow(() -> new TypeException("Expected a scope as third argument to " + c));
        Occurrence decl = Occurrence.matcher().match(declTerm)
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + c));
        scopeGraph.addAssoc(decl, c.getLabel(), scope);
        return true;
    }


    private boolean solveDirectEdge(CGDirectEdge<Scope> c) {
        ITerm targetScopeTerm = unifier().find(c.getTargetScope());
        if(!targetScopeTerm.isGround()) {
            return false;
        }
        Scope targetScope = Scope.matcher().match(targetScopeTerm)
                .orElseThrow(() -> new TypeException("Expected a scope as third argument to " + c));
        scopeGraph.addDirectEdge(c.getSourceScope(), c.getLabel(), targetScope);
        scopeCounter.remove(c.getSourceScope(), c.getLabel());
        return true;
    }

    private boolean solveImportEdge(CGImportEdge<Scope> c) {
        ITerm refTerm = unifier().find(c.getReference());
        if(!refTerm.isGround()) {
            return false;
        }
        Occurrence ref = Occurrence.matcher().match(refTerm)
                .orElseThrow(() -> new TypeException("Expected an occurrence as third argument to " + c));
        scopeGraph.addImport(c.getScope(), c.getLabel(), ref);
        scopeCounter.remove(c.getScope(), c.getLabel());
        return true;
    }


    private boolean solve(CResolve r) throws UnsatisfiableException {
        if(!isResolutionStarted()) {
            return false;
        }
        ITerm refTerm = unifier().find(r.getReference());
        if(!refTerm.isGround()) {
            return false;
        }
        Occurrence ref = Occurrence.matcher().match(refTerm)
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + r));
        Optional<Set<IResolutionPath<Scope, Label, Occurrence>>> paths = nameResolution.tryResolve(ref);
        if(paths.isPresent()) {
            List<Occurrence> declarations = Paths.resolutionPathsToDecls(paths.get());
            unifier().removeActive(r.getDeclaration(), r); // before `unify`, so that we don't cause an error chain if
                                                           // that fails
            switch(declarations.size()) {
                case 0:
                    throw new UnsatisfiableException(r.getMessageInfo().withDefaultContent(
                            MessageContent.builder().append(ref).append(" does not resolve.").build()));
                case 1:
                    try {
                        unifier().unify(r.getDeclaration(), declarations.get(0));
                    } catch(UnificationException ex) {
                        throw new UnsatisfiableException(r.getMessageInfo().withDefaultContent(ex.getMessageContent()));
                    }
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
        ITerm declTerm = unifier().find(a.getDeclaration());
        if(!declTerm.isGround()) {
            return false;
        }
        Occurrence decl = Occurrence.matcher().match(declTerm)
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + a));
        Label label = a.getLabel();
        List<Scope> scopes = Lists.newArrayList(scopeGraph.getExportEdges().get(decl, label));
        unifier().removeActive(a.getScope(), a); // before `unify`, so that we don't cause an error chain if that fails
        switch(scopes.size()) {
            case 0:
                throw new UnsatisfiableException(a.getMessageInfo().withDefaultContent(MessageContent.builder()
                        .append(decl).append(" has no ").append(label).append(" associated scope.").build()));
            case 1:
                try {
                    unifier().unify(a.getScope(), scopes.get(0));
                } catch(UnificationException ex) {
                    throw new UnsatisfiableException(a.getMessageInfo().withDefaultContent(ex.getMessageContent()));
                }
                return true;
            default:
                throw new UnsatisfiableException(a.getMessageInfo().withDefaultContent(MessageContent.builder()
                        .append(decl).append(" has multiple ").append(label).append(" associated scope.").build()));
        }
    }

    private boolean solve(CDeclProperty c) throws UnsatisfiableException {
        ITerm declTerm = unifier().find(c.getDeclaration());
        if(!declTerm.isGround()) {
            return false;
        }
        Occurrence decl = Occurrence.matcher().match(declTerm)
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + c));
        unifier().removeActive(c.getValue(), c); // before `unify`, so that we don't cause an error chain if that fails
        Optional<ITerm> prev = properties.putValue(decl, c.getKey(), c.getValue());
        if(prev.isPresent()) {
            try {
                unifier().unify(c.getValue(), prev.get());
            } catch(UnificationException ex) {
                throw new UnsatisfiableException(c.getMessageInfo().withDefaultContent(ex.getMessageContent()));
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean isResolutionStarted() {
        return nameResolution != null;
    }

    private void addScopeGraphConstraints(Set<INamebindingConstraint> constraints, IMessageInfo messageInfo) {
        for(Scope scope : scopeGraph.getAllScopes()) {
            for(Occurrence decl : scopeGraph.getDecls().inverse().get(scope)) {
                constraints.add(ImmutableCGDecl.of(scope, decl, messageInfo));
            }
            for(Occurrence ref : scopeGraph.getRefs().inverse().get(scope)) {
                constraints.add(ImmutableCGRef.of(ref, scope, messageInfo));
            }
            for(Map.Entry<Label, Scope> edge : scopeGraph.getDirectEdges().get(scope)) {
                constraints.add(ImmutableCGDirectEdge.of(scope, edge.getKey(), edge.getValue(), messageInfo));
            }
            for(Map.Entry<Label, Occurrence> edge : scopeGraph.getImportEdges().get(scope)) {
                constraints.add(ImmutableCGImportEdge.of(scope, edge.getKey(), edge.getValue(), messageInfo));
            }
            for(Map.Entry<Label, Occurrence> edge : scopeGraph.getExportEdges().inverse().get(scope)) {
                constraints.add(ImmutableCGExportEdge.of(edge.getValue(), edge.getKey(), scope, messageInfo));
            }
            for(Occurrence decl : properties.getIndices()) {
                for(ITerm key : properties.getDefinedKeys(decl)) {
                    properties.getValue(decl, key).ifPresent(value -> {
                        constraints.add(ImmutableCDeclProperty.of(decl, key, value, 0, messageInfo));
                    });
                }
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
                    Iterable<Occurrence> decls = NamebindingSolver.this.scopeGraph.getDecls().inverse().get(scope);
                    return Optional.of(makeSet(decls, ns));
                }),
                M.appl2("References", Scope.matcher(), Namespace.matcher(), (t, scope, ns) -> {
                    Iterable<Occurrence> refs = NamebindingSolver.this.scopeGraph.getRefs().inverse().get(scope);
                    return Optional.of(makeSet(refs, ns));
                }),
                M.appl2("Visibles", Scope.matcher(), Namespace.matcher(), (t, scope, ns) -> {
                    Optional<Set<IDeclPath<Scope,Label,Occurrence>>> paths = NamebindingSolver.this.nameResolution.tryVisible(scope);
                    return paths.map(ps -> makeSet(Paths.declPathsToDecls(ps), ns));
                }),
                M.appl2("Reachables", Scope.matcher(), Namespace.matcher(), (t, scope, ns) -> {
                    Optional<Set<IDeclPath<Scope,Label,Occurrence>>> paths = NamebindingSolver.this.nameResolution.tryReachable(scope);
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


}