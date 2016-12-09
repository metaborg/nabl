package org.metaborg.meta.nabl2.solver;

import static org.metaborg.meta.nabl2.collections.Unit.unit;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.metaborg.meta.nabl2.collections.Unit;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.namebinding.Assoc;
import org.metaborg.meta.nabl2.constraints.namebinding.Decl;
import org.metaborg.meta.nabl2.constraints.namebinding.DirectEdge;
import org.metaborg.meta.nabl2.constraints.namebinding.INamebindingConstraint;
import org.metaborg.meta.nabl2.constraints.namebinding.INamebindingConstraint.CheckedCases;
import org.metaborg.meta.nabl2.constraints.namebinding.Import;
import org.metaborg.meta.nabl2.constraints.namebinding.Ref;
import org.metaborg.meta.nabl2.constraints.namebinding.Resolve;
import org.metaborg.meta.nabl2.scopegraph.INameResolution;
import org.metaborg.meta.nabl2.scopegraph.IScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.esop.EsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.EsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.ResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.terms.ITerm;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class NamebindingSolver implements ISolverComponent<INamebindingConstraint> {

    private static final long serialVersionUID = 7240605942212774052L;

    private final EqualitySolver unifier;
    private final ResolutionParameters params;
    private final EsopScopeGraph<Scope, Label, Occurrence> scopeGraph;

    private EsopNameResolution<Scope, Label, Occurrence> nameResolution = null;

    private final Set<INamebindingConstraint> defered = Sets.newHashSet();

    public NamebindingSolver(ResolutionParameters params, EqualitySolver unifier) {
        this.unifier = unifier;
        this.scopeGraph = new EsopScopeGraph<>();
        this.params = params;
    }

    public IScopeGraph<Scope, Label, Occurrence> getScopeGraph() {
        return scopeGraph;
    }

    public INameResolution<Scope, Label, Occurrence> getNameResolution() {
        return nameResolution != null ? nameResolution : INameResolution.empty();
    }

    // ------------------------------------------------------------------------------------------------------//

    @Override
    public Unit add(INamebindingConstraint constraint) throws UnsatisfiableException {
        if(nameResolution != null) {
            throw new IllegalStateException();
        }
        if(!solve(constraint)) {
            defered.add(constraint);
        }
        return unit;
    }

    @Override
    public boolean iterate() throws UnsatisfiableException {
        if(nameResolution == null) {
            nameResolution = new EsopNameResolution<>(scopeGraph, params);
        }
        Iterator<INamebindingConstraint> it = defered.iterator();
        boolean progress = false;
        while(it.hasNext()) {
            try {
                if(solve(it.next())) {
                    progress |= true;
                    it.remove();
                }
            } catch(UnsatisfiableException e) {
                it.remove();
                throw e;
            }
        }
        return progress;
    }

    @Override
    public void finish() throws UnsatisfiableException {
        if(!defered.isEmpty()) {
            throw new UnsatisfiableException("Unsolved namebinding constraint.", defered.toArray(new IConstraint[0]));
        }
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean solve(INamebindingConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(
                CheckedCases.of(this::solve, this::solve, this::solve, this::solve, this::solve, this::solve));
    }

    private boolean solve(Decl c) throws UnsatisfiableException {
        ITerm declTerm = unifier.find(c.getDeclaration());
        if(!declTerm.isGround()) {
            return false;
        }
        ITerm scopeTerm = unifier.find(c.getScope());
        if(!scopeTerm.isGround()) {
            return false;
        }
        Occurrence decl = Occurrence.matcher().match(declTerm).orElseThrow(() -> new TypeException());
        Scope scope = Scope.matcher().match(scopeTerm).orElseThrow(() -> new TypeException());
        scopeGraph.addDecl(scope, decl);
        return true;
    }

    private boolean solve(Ref c) throws UnsatisfiableException {
        ITerm refTerm = unifier.find(c.getReference());
        if(!refTerm.isGround()) {
            return false;
        }
        ITerm scopeTerm = unifier.find(c.getScope());
        if(!scopeTerm.isGround()) {
            return false;
        }
        Occurrence ref = Occurrence.matcher().match(refTerm).orElseThrow(() -> new TypeException());
        Scope scope = Scope.matcher().match(scopeTerm).orElseThrow(() -> new TypeException());
        scopeGraph.addRef(ref, scope);
        return true;
    }

    private boolean solve(DirectEdge de) throws UnsatisfiableException {
        ITerm sourceScopeTerm = unifier.find(de.getSourceScope());
        if(!sourceScopeTerm.isGround()) {
            return false;
        }
        ITerm labelTerm = unifier.find(de.getLabel());
        if(!labelTerm.isGround()) {
            return false;
        }
        ITerm targetScopeTerm = unifier.find(de.getTargetScope());
        if(!targetScopeTerm.isGround()) {
            return false;
        }
        Scope sourceScope = Scope.matcher().match(sourceScopeTerm).orElseThrow(() -> new TypeException());
        Label label = Label.matcher().match(labelTerm).orElseThrow(() -> new TypeException());
        Scope targetScope = Scope.matcher().match(targetScopeTerm).orElseThrow(() -> new TypeException());
        scopeGraph.addDirectEdge(sourceScope, label, targetScope);
        return true;
    }

    private boolean solve(Assoc ee) throws UnsatisfiableException {
        ITerm declTerm = unifier.find(ee.getDeclaration());
        if(!declTerm.isGround()) {
            return false;
        }
        ITerm labelTerm = unifier.find(ee.getLabel());
        if(!labelTerm.isGround()) {
            return false;
        }
        ITerm scopeTerm = unifier.find(ee.getScope());
        if(!scopeTerm.isGround()) {
            return false;
        }
        Occurrence decl = Occurrence.matcher().match(declTerm).orElseThrow(() -> new TypeException());
        Label label = Label.matcher().match(labelTerm).orElseThrow(() -> new TypeException());
        Scope scope = Scope.matcher().match(scopeTerm).orElseThrow(() -> new TypeException());
        scopeGraph.addAssoc(decl, label, scope);
        return true;
    }

    private boolean solve(Import ie) throws UnsatisfiableException {
        ITerm scopeTerm = unifier.find(ie.getScope());
        if(!scopeTerm.isGround()) {
            return false;
        }
        ITerm labelTerm = unifier.find(ie.getLabel());
        if(!labelTerm.isGround()) {
            return false;
        }
        ITerm refTerm = unifier.find(ie.getReference());
        if(!refTerm.isGround()) {
            return false;
        }
        Scope scope = Scope.matcher().match(scopeTerm).orElseThrow(() -> new TypeException());
        Label label = Label.matcher().match(labelTerm).orElseThrow(() -> new TypeException());
        Occurrence ref = Occurrence.matcher().match(refTerm).orElseThrow(() -> new TypeException());
        scopeGraph.addImport(scope, label, ref);
        return true;
    }

    private boolean solve(Resolve c) throws UnsatisfiableException {
        if(nameResolution == null) {
            return false;
        }
        ITerm refTerm = unifier.find(c.getReference());
        if(!refTerm.isGround()) {
            return false;
        }
        Occurrence ref = Occurrence.matcher().match(refTerm).orElseThrow(() -> new TypeException());
        Optional<Iterable<Occurrence>> r = nameResolution.tryResolve(ref);
        if(r.isPresent()) {
            List<Occurrence> declarations = Lists.newArrayList(r.get());
            switch(declarations.size()) {
            case 0:
                throw new UnsatisfiableException(ref + " does not resolve.", c);
            case 1:
                unifier.unify(c.getDeclaration(), declarations.get(0));
                return true;
            default:
                throw new UnsatisfiableException("Resolution of " + ref + " is ambiguous.", c);
            }
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------------------------------------//

}