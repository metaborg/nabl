package org.metaborg.meta.nabl2.solver;

import static org.metaborg.meta.nabl2.collections.Unit.unit;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.metaborg.meta.nabl2.collections.Unit;
import org.metaborg.meta.nabl2.constraints.namebinding.Assoc;
import org.metaborg.meta.nabl2.constraints.namebinding.Decl;
import org.metaborg.meta.nabl2.constraints.namebinding.DirectEdge;
import org.metaborg.meta.nabl2.constraints.namebinding.INamebindingConstraint;
import org.metaborg.meta.nabl2.constraints.namebinding.INamebindingConstraint.CheckedCases;
import org.metaborg.meta.nabl2.constraints.namebinding.Import;
import org.metaborg.meta.nabl2.constraints.namebinding.Ref;
import org.metaborg.meta.nabl2.constraints.namebinding.Resolve;
import org.metaborg.meta.nabl2.regexp.FiniteAlphabet;
import org.metaborg.meta.nabl2.regexp.IAlphabet;
import org.metaborg.meta.nabl2.regexp.IRegExp;
import org.metaborg.meta.nabl2.regexp.RegExpBuilder;
import org.metaborg.meta.nabl2.scopegraph.INameResolution;
import org.metaborg.meta.nabl2.scopegraph.IScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.esop.EsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.EsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.terms.ImmutableLabel;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.transitiveclosure.SymmetryException;
import org.metaborg.meta.nabl2.transitiveclosure.TransitiveClosure;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class NamebindingSolver implements ISolverComponent<INamebindingConstraint> {

    private static final long serialVersionUID = 7240605942212774052L;

    private final EqualitySolver unifier;

    private final EsopScopeGraph<Scope,Label,Occurrence> scopeGraph;
    private final IAlphabet<Label> labels;
    private final IRegExp<Label> wf;
    private final TransitiveClosure<Label> order;

    private EsopNameResolution<Scope,Label,Occurrence> nameResolution = null;

    private final Set<INamebindingConstraint> defered = Sets.newHashSet();


    public NamebindingSolver(EqualitySolver unifier) {
        this.unifier = unifier;
        this.scopeGraph = new EsopScopeGraph<>();

        Label D = ImmutableLabel.of("D");
        Label P = ImmutableLabel.of("P");
        Label I = ImmutableLabel.of("I");
        this.labels = new FiniteAlphabet<>(ImmutableSet.of(D, P, I));
        RegExpBuilder<Label> R = new RegExpBuilder<>(this.labels);
        this.wf = R.concat(R.closure(R.symbol(P)), R.closure(R.symbol(I)));
        try {
            this.order = new TransitiveClosure<Label>().add(D, I).add(I, P);
        } catch (SymmetryException e) {
            throw new IllegalStateException(e);
        }
    }

    public IScopeGraph<Scope,Label,Occurrence> getScopeGraph() {
        return scopeGraph;
    }

    public INameResolution<Scope,Label,Occurrence> getNameResolution() {
        return nameResolution;
    }

    // ------------------------------------------------------------------------------------------------------//

    @Override public Unit add(INamebindingConstraint constraint) throws UnsatisfiableException {
        if (nameResolution != null) {
            throw new IllegalStateException();
        }
        if (!solve(constraint)) {
            defered.add(constraint);
        }
        return unit;
    }

    @Override public boolean iterate() throws UnsatisfiableException {
        if (nameResolution == null) {
            nameResolution = new EsopNameResolution<>(scopeGraph, labels, wf, order);
        }
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
            throw new UnsatisfiableException("Unsolved namebinding constraint.", constraint);
        }
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean solve(INamebindingConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(CheckedCases.of(this::solve, this::solve, this::solve, this::solve, this::solve,
                this::solve));
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
        Occurrence decl = Occurrence.matcher().match(declTerm).orElseThrow(() -> new TypeException());
        Scope scope = Scope.matcher().match(scopeTerm).orElseThrow(() -> new TypeException());
        scopeGraph.addDecl(scope, decl);
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
        Occurrence ref = Occurrence.matcher().match(refTerm).orElseThrow(() -> new TypeException());
        Scope scope = Scope.matcher().match(scopeTerm).orElseThrow(() -> new TypeException());
        scopeGraph.addRef(ref, scope);
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
        Scope sourceScope = Scope.matcher().match(sourceScopeTerm).orElseThrow(() -> new TypeException());
        Label label = Label.matcher().match(labelTerm).orElseThrow(() -> new TypeException());
        Scope targetScope = Scope.matcher().match(targetScopeTerm).orElseThrow(() -> new TypeException());
        scopeGraph.addDirectEdge(sourceScope, label, targetScope);
        return true;
    }

    private boolean solve(Assoc ee) throws UnsatisfiableException {
        ITerm declTerm = unifier.find(ee.getDeclaration());
        if (!declTerm.isGround()) {
            return false;
        }
        ITerm labelTerm = unifier.find(ee.getLabel());
        if (!labelTerm.isGround()) {
            return false;
        }
        ITerm scopeTerm = unifier.find(ee.getScope());
        if (!scopeTerm.isGround()) {
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
        if (!scopeTerm.isGround()) {
            return false;
        }
        ITerm labelTerm = unifier.find(ie.getLabel());
        if (!labelTerm.isGround()) {
            return false;
        }
        ITerm refTerm = unifier.find(ie.getReference());
        if (!refTerm.isGround()) {
            return false;
        }
        Scope scope = Scope.matcher().match(scopeTerm).orElseThrow(() -> new TypeException());
        Label label = Label.matcher().match(labelTerm).orElseThrow(() -> new TypeException());
        Occurrence ref = Occurrence.matcher().match(refTerm).orElseThrow(() -> new TypeException());
        scopeGraph.addImport(scope, label, ref);
        return true;
    }

    private boolean solve(Resolve c) throws UnsatisfiableException {
        if (nameResolution == null) {
            return false;
        }
        ITerm refTerm = unifier.find(c.getReference());
        if (!refTerm.isGround()) {
            return false;
        }
        Occurrence ref = Occurrence.matcher().match(refTerm).orElseThrow(() -> new TypeException());
        Optional<Iterable<Occurrence>> r = nameResolution.tryResolve(ref);
        if (r.isPresent()) {
            List<Occurrence> declarations = Lists.newArrayList(r.get());
            if (declarations.size() == 1) {
                unifier.unify(c.getDeclaration(), declarations.get(0));
            } else {
                throw new UnsatisfiableException("Resolution failed.", c);
            }
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------------------------------------//

}