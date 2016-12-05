package org.metaborg.meta.nabl2.solver;

import static org.metaborg.meta.nabl2.collections.Unit.unit;

import java.util.*;

import org.metaborg.meta.nabl2.collections.Unit;
import org.metaborg.meta.nabl2.constraints.namebinding.*;
import org.metaborg.meta.nabl2.constraints.namebinding.INamebindingConstraint.CheckedCases;
import org.metaborg.meta.nabl2.scopegraph.*;
import org.metaborg.meta.nabl2.scopegraph.esop.EsopScopeGraph;
import org.metaborg.meta.nabl2.terms.ITerm;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class NamebindingSolver implements ISolverComponent<INamebindingConstraint> {

    private static final long serialVersionUID = 7240605942212774052L;

    private final EqualitySolver unifier;
    private final EsopScopeGraph esopScopeGraph;

    private boolean complete = false;
    private final Set<INamebindingConstraint> defered = Sets.newHashSet();

    public NamebindingSolver(EqualitySolver unifier) {
        this.unifier = unifier;
        this.esopScopeGraph = new EsopScopeGraph();
    }

    // ------------------------------------------------------------------------------------------------------//

    @Override public Unit add(INamebindingConstraint constraint) throws UnsatisfiableException {
        if (complete) {
            throw new IllegalStateException();
        }
        if (!solve(constraint)) {
            defered.add(constraint);
        }
        return unit;
    }

    @Override public boolean iterate() throws UnsatisfiableException {
        complete = true;
        Iterator<INamebindingConstraint> it = defered.iterator();
        boolean progress = false;
        while (it.hasNext()) {
            if (solve(it.next())) {
                progress |= true;
                it.remove();
            }
        }
        return progress;
    }

    @Override public void finish() throws UnsatisfiableException {
        for (INamebindingConstraint constraint : defered) {
            throw new UnsatisfiableException(constraint);
        }
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean solve(INamebindingConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(CheckedCases.of(this::solve, this::solve, this::solve, this::solve));
    }

    private boolean solve(Decl c) throws UnsatisfiableException {
        ITerm declTerm = unifier.find(c.getDeclaration());
        if (!declTerm.isGround()) {
            return false;
        }
        ITerm scopeTerm = unifier.find(c.getScope());
        if (!scopeTerm.isGround()) {
            return false;
        }
        Occurrence decl = ScopeGraphTerms.occurrence().apply(declTerm).orElseThrow(() -> new TypeException());
        Scope scope = ScopeGraphTerms.scope().apply(scopeTerm).orElseThrow(() -> new TypeException());
        esopScopeGraph.addDecl(scope, decl);
        return true;
    }

    private boolean solve(Ref c) throws UnsatisfiableException {
        ITerm refTerm = unifier.find(c.getReference());
        if (!refTerm.isGround()) {
            return false;
        }
        ITerm scopeTerm = unifier.find(c.getScope());
        if (!scopeTerm.isGround()) {
            return false;
        }
        Occurrence ref = ScopeGraphTerms.occurrence().apply(refTerm).orElseThrow(() -> new TypeException());
        Scope scope = ScopeGraphTerms.scope().apply(scopeTerm).orElseThrow(() -> new TypeException());
        esopScopeGraph.addRef(ref, scope);
        return true;
    }

    private boolean solve(DirectEdge de) throws UnsatisfiableException {
        ITerm sourceScopeTerm = unifier.find(de.getSourceScope());
        if (!sourceScopeTerm.isGround()) {
            return false;
        }
        ITerm labelTerm = unifier.find(de.getLabel());
        if (!labelTerm.isGround()) {
            return false;
        }
        ITerm targetScopeTerm = unifier.find(de.getTargetScope());
        if (!targetScopeTerm.isGround()) {
            return false;
        }
        Scope sourceScope = ScopeGraphTerms.scope().apply(sourceScopeTerm).orElseThrow(() -> new TypeException());
        Label label = ScopeGraphTerms.label().apply(labelTerm).orElseThrow(() -> new TypeException());
        Scope targetScope = ScopeGraphTerms.scope().apply(targetScopeTerm).orElseThrow(() -> new TypeException());
        esopScopeGraph.addDirectEdge(sourceScope, label, targetScope);
        return true;
    }


    private boolean solve(Resolve c) {
        if (!complete) {
            return false;
        }
        ITerm refTerm = unifier.find(c.getReference());
        if (!refTerm.isGround()) {
            return false;
        }
        return resolve(refTerm).matchThrows(IResolutionResult.CheckedCases.of(decls -> {
            List<ITerm> declarations = Lists.newArrayList(decls);
            if (declarations.size() == 1) {
                unifier.unify(c.getDeclaration(), declarations.get(0));
            } else {
                throw new UnsatisfiableException();
            }
            return true;
        }, var -> {
            return false;
        }));
    }

    // ------------------------------------------------------------------------------------------------------//

    private IResolutionResult resolve(ITerm reference) {
        return null;
    }

}