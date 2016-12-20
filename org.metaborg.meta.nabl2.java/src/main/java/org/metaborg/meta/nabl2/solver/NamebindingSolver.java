package org.metaborg.meta.nabl2.solver;

import static org.metaborg.meta.nabl2.util.Unit.unit;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.constraints.namebinding.CAssoc;
import org.metaborg.meta.nabl2.constraints.namebinding.CDeclProperty;
import org.metaborg.meta.nabl2.constraints.namebinding.CGAssoc;
import org.metaborg.meta.nabl2.constraints.namebinding.CGDecl;
import org.metaborg.meta.nabl2.constraints.namebinding.CGDirectEdge;
import org.metaborg.meta.nabl2.constraints.namebinding.CGImport;
import org.metaborg.meta.nabl2.constraints.namebinding.CGRef;
import org.metaborg.meta.nabl2.constraints.namebinding.CResolve;
import org.metaborg.meta.nabl2.constraints.namebinding.INamebindingConstraint;
import org.metaborg.meta.nabl2.constraints.namebinding.INamebindingConstraint.CheckedCases;
import org.metaborg.meta.nabl2.scopegraph.INameResolution;
import org.metaborg.meta.nabl2.scopegraph.IPath;
import org.metaborg.meta.nabl2.scopegraph.IScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.esop.EsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.EsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Namespace;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Paths;
import org.metaborg.meta.nabl2.scopegraph.terms.ResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.sets.IElement;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.unification.UnificationException;
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.meta.nabl2.util.Unit;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class NamebindingSolver implements ISolverComponent<INamebindingConstraint> {

    private final Unifier unifier;
    private final ResolutionParameters params;
    private final EsopScopeGraph<Scope,Label,Occurrence> scopeGraph;
    private final Properties<Occurrence> properties;

    private EsopNameResolution<Scope,Label,Occurrence> nameResolution = null;

    private final Set<INamebindingConstraint> deferedBuilds = Sets.newHashSet();
    private final Set<INamebindingConstraint> deferedChecks = Sets.newHashSet();

    public NamebindingSolver(ResolutionParameters params, Unifier unifier) {
        this.unifier = unifier;
        this.params = params;
        this.scopeGraph = new EsopScopeGraph<>();
        this.properties = new Properties<>();
    }

    public IScopeGraph<Scope,Label,Occurrence> getScopeGraph() {
        return scopeGraph;
    }

    public INameResolution<Scope,Label,Occurrence> getNameResolution() {
        return nameResolution != null ? nameResolution : INameResolution.empty();
    }

    public IProperties<Occurrence> getProperties() {
        return properties;
    }

    // ------------------------------------------------------------------------------------------------------//

    @Override public Unit add(INamebindingConstraint constraint) throws UnsatisfiableException {
        if (nameResolution != null) {
            throw new IllegalStateException();
        }
        return constraint.matchOrThrow(CheckedCases.of(this::addBuild, this::addBuild, this::addBuild, this::addBuild,
                this::addBuild, this::addCheck, this::addCheck, this::addCheck));
    }

    private Unit addBuild(INamebindingConstraint constraint) throws UnsatisfiableException {
        if (!solve(constraint)) {
            deferedBuilds.add(constraint);
        }
        return unit;
    }

    private Unit addCheck(INamebindingConstraint constraint) throws UnsatisfiableException {
        if (!solve(constraint)) {
            deferedChecks.add(constraint);
        }
        return unit;
    }

    @Override public boolean iterate() throws UnsatisfiableException {
        boolean progress = false;
        progress |= iterate(deferedBuilds);
        if (nameResolution == null && deferedBuilds.isEmpty()) {
            progress |= true;
            nameResolution = new EsopNameResolution<>(scopeGraph, params);
        }
        progress |= iterate(deferedChecks);
        return progress;
    }

    private boolean iterate(Set<INamebindingConstraint> defered) throws UnsatisfiableException {
        boolean progress = false;
        Iterator<INamebindingConstraint> it = defered.iterator();
        while (it.hasNext()) {
            try {
                if (solve(it.next())) {
                    progress = true;
                    it.remove();
                }
            } catch (UnsatisfiableException e) {
                progress = true;
                it.remove();
                throw e;
            }
        }
        return progress;
    }

    @Override public Iterable<UnsatisfiableException> finish() {
        Set<INamebindingConstraint> unsolved = Sets.newHashSet();
        unsolved.addAll(deferedBuilds);
        unsolved.addAll(deferedChecks);
        return unsolved.stream().map(c -> {
            return c.getMessageInfo().makeException("Unsolved name resolution constraint: " + c.find(unifier),
                    Iterables2.empty(), unifier);
        }).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean solve(INamebindingConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(CheckedCases.of(this::solve, this::solve, this::solve, this::solve, this::solve,
                this::solve, this::solve, this::solve));
    }

    private boolean solve(CGDecl c) throws UnsatisfiableException {
        ITerm declTerm = unifier.find(c.getDeclaration());
        if (!declTerm.isGround()) {
            return false;
        }
        ITerm scopeTerm = unifier.find(c.getScope());
        if (!scopeTerm.isGround()) {
            return false;
        }
        Occurrence decl = Occurrence.matcher().match(declTerm).orElseThrow(() -> new TypeException());
        Scope scope = Scope.matcher().match(scopeTerm).orElseThrow(() -> new TypeException());
        scopeGraph.addDecl(scope, decl);
        return true;
    }

    private boolean solve(CGRef c) throws UnsatisfiableException {
        ITerm refTerm = unifier.find(c.getReference());
        if (!refTerm.isGround()) {
            return false;
        }
        ITerm scopeTerm = unifier.find(c.getScope());
        if (!scopeTerm.isGround()) {
            return false;
        }
        Occurrence ref = Occurrence.matcher().match(refTerm).orElseThrow(() -> new TypeException());
        Scope scope = Scope.matcher().match(scopeTerm).orElseThrow(() -> new TypeException());
        scopeGraph.addRef(ref, scope);
        return true;
    }

    private boolean solve(CGDirectEdge de) throws UnsatisfiableException {
        ITerm sourceScopeTerm = unifier.find(de.getSourceScope());
        if (!sourceScopeTerm.isGround()) {
            return false;
        }
        ITerm targetScopeTerm = de.getTargetScope();
        Scope sourceScope = Scope.matcher().match(sourceScopeTerm).orElseThrow(() -> new TypeException());
        scopeGraph.addDirectEdge(sourceScope, de.getLabel(), () -> Optional.of(unifier.find(targetScopeTerm)).filter(
                ITerm::isGround).map(ts -> Scope.matcher().match(ts).orElseThrow(() -> new TypeException())));
        return true;
    }

    private boolean solve(CGAssoc ee) throws UnsatisfiableException {
        ITerm declTerm = unifier.find(ee.getDeclaration());
        if (!declTerm.isGround()) {
            return false;
        }
        ITerm scopeTerm = unifier.find(ee.getScope());
        if (!scopeTerm.isGround()) {
            return false;
        }
        Occurrence decl = Occurrence.matcher().match(declTerm).orElseThrow(() -> new TypeException());
        Scope scope = Scope.matcher().match(scopeTerm).orElseThrow(() -> new TypeException());
        scopeGraph.addAssoc(decl, ee.getLabel(), scope);
        return true;
    }

    private boolean solve(CGImport ie) throws UnsatisfiableException {
        ITerm scopeTerm = unifier.find(ie.getScope());
        if (!scopeTerm.isGround()) {
            return false;
        }
        ITerm refTerm = unifier.find(ie.getReference());
        Scope scope = Scope.matcher().match(scopeTerm).orElseThrow(() -> new TypeException());
        scopeGraph.addImport(scope, ie.getLabel(), () -> Optional.of(unifier.find(refTerm)).filter(ITerm::isGround).map(
                rt -> Occurrence.matcher().match(rt).orElseThrow(() -> new TypeException())));
        return true;
    }

    private boolean solve(CResolve r) throws UnsatisfiableException {
        if (nameResolution == null) {
            return false;
        }
        ITerm refTerm = unifier.find(r.getReference());
        if (!refTerm.isGround()) {
            return false;
        }
        Occurrence ref = Occurrence.matcher().match(refTerm).orElseThrow(() -> new TypeException());
        Optional<Iterable<IPath<Scope,Label,Occurrence>>> paths = nameResolution.tryResolve(ref);
        if (paths.isPresent()) {
            List<Occurrence> declarations = Paths.pathsToDecls(paths.get());
            switch (declarations.size()) {
            case 0:
                throw r.getMessageInfo().makeException(ref + " does not resolve.", Iterables2.empty(), unifier);
            case 1:
                try {
                    unifier.unify(r.getDeclaration(), declarations.get(0));
                } catch (UnificationException ex) {
                    throw r.getMessageInfo().makeException(ex.getMessage(), Iterables2.empty(), unifier);
                }
                return true;
            default:
                throw r.getMessageInfo().makeException("Resolution of " + ref + " is ambiguous.", Iterables2.empty(),
                        unifier);
            }
        } else {
            return false;
        }
    }

    private boolean solve(CAssoc a) throws UnsatisfiableException {
        if (nameResolution == null) {
            return false;
        }
        ITerm declTerm = unifier.find(a.getDeclaration());
        if (!declTerm.isGround()) {
            return false;
        }
        Occurrence decl = Occurrence.matcher().match(declTerm).orElseThrow(() -> new TypeException());
        Label label = a.getLabel();
        List<Scope> scopes = Lists.newArrayList(scopeGraph.getAssocScopes(decl).get(label));
        switch (scopes.size()) {
        case 0:
            throw a.getMessageInfo().makeException(decl + " has no " + label + " associated scope.", Iterables2.empty(),
                    unifier);
        case 1:
            try {
                unifier.unify(a.getScope(), scopes.get(0));
            } catch (UnificationException ex) {
                throw a.getMessageInfo().makeException(ex.getMessage(), Iterables2.empty(), unifier);
            }
            return true;
        default:
            throw a.getMessageInfo().makeException(decl + " has multiple " + label + " associated scopes.", Iterables2
                    .empty(), unifier);
        }
    }

    private boolean solve(CDeclProperty c) throws UnsatisfiableException {
        ITerm declTerm = unifier.find(c.getDeclaration());
        if (!declTerm.isGround()) {
            return false;
        }
        ITerm keyTerm = unifier.find(c.getKey());
        if (!keyTerm.isGround()) {
            return false;
        }
        Occurrence decl = Occurrence.matcher().match(declTerm).orElseThrow(() -> new TypeException());
        Optional<ITerm> prev = properties.putValue(decl, keyTerm, c.getValue());
        if (prev.isPresent()) {
            try {
                unifier.unify(c.getValue(), prev.get());
            } catch (UnificationException ex) {
                throw c.getMessageInfo().makeException(ex.getMessage(), Iterables2.empty(), unifier);
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------------------//

    public IMatcher<Set<IElement<ITerm>>> nameSets() {
        return term -> {
            if (NamebindingSolver.this.nameResolution == null) {
                return Optional.empty();
            }
            return M.<Optional<Set<IElement<ITerm>>>> cases(
                // @formatter:off
                M.appl2("Declarations", Scope.matcher(), Namespace.matcher(), (t, scope, ns) -> {
                    Iterable<Occurrence> decls = NamebindingSolver.this.scopeGraph.getDecls(scope);
                    return Optional.of(makeSet(decls, ns));
                }),
                M.appl2("References", Scope.matcher(), Namespace.matcher(), (t, scope, ns) -> {
                    Iterable<Occurrence> refs = NamebindingSolver.this.scopeGraph.getRefs(scope);
                    return Optional.of(makeSet(refs, ns));
                }),
                M.appl2("Visibles", Scope.matcher(), Namespace.matcher(), (t, scope, ns) -> {
                    Optional<Iterable<IPath<Scope,Label,Occurrence>>> paths = NamebindingSolver.this.nameResolution.tryVisible(scope);
                    return paths.map(ps -> makeSet(Paths.pathsToDecls(ps), ns));
                }),
                M.appl2("Reachables", Scope.matcher(), Namespace.matcher(), (t, scope, ns) -> {
                    Optional<Iterable<IPath<Scope,Label,Occurrence>>> paths = NamebindingSolver.this.nameResolution.tryReachable(scope);
                    return paths.map(ps -> makeSet(Paths.pathsToDecls(ps), ns));
                })
                // @formatter:on
            ).match(term).flatMap(o -> o);
        };
    }

    private Set<IElement<ITerm>> makeSet(Iterable<Occurrence> occurrences, Namespace namespace) {
        Set<IElement<ITerm>> result = Sets.newHashSet();
        for (Occurrence occurrence : occurrences) {
            if (!namespace.getName().filter(ns -> !occurrence.getNamespace().equals(namespace)).isPresent()) {
                result.add(new OccurrenceElement(occurrence));
            }
        }
        return result;
    }

    private class OccurrenceElement implements IElement<ITerm> {

        private final Occurrence occurrence;

        public OccurrenceElement(Occurrence occurrence) {
            this.occurrence = occurrence;
        }

        @Override public ITerm getValue() {
            return occurrence;
        }

        @Override public ITerm getPosition() {
            return occurrence.getName(); // this should probably be position,
                                         // but origin information is lost.
        }

        @Override public Object project(String name) {
            switch (name) {
            case "name":
                return occurrence.getName();
            default:
                throw new IllegalArgumentException("Projection " + name + " undefined for occurrences.");
            }
        }

    }

}