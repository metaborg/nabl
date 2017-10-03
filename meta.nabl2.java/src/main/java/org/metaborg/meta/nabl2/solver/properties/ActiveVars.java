package org.metaborg.meta.nabl2.solver.properties;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.ast.IAstConstraint;
import org.metaborg.meta.nabl2.constraints.base.IBaseConstraint;
import org.metaborg.meta.nabl2.constraints.controlflow.IControlFlowConstraint;
import org.metaborg.meta.nabl2.constraints.equality.IEqualityConstraint;
import org.metaborg.meta.nabl2.constraints.nameresolution.INameResolutionConstraint;
import org.metaborg.meta.nabl2.constraints.poly.IPolyConstraint;
import org.metaborg.meta.nabl2.constraints.relations.IRelationConstraint;
import org.metaborg.meta.nabl2.constraints.scopegraph.IScopeGraphConstraint;
import org.metaborg.meta.nabl2.constraints.sets.ISetConstraint;
import org.metaborg.meta.nabl2.constraints.sym.ISymbolicConstraint;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.unification.IUnifier;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;

public class ActiveVars implements IConstraintSetProperty {

    private final IUnifier unifier;
    private final Multiset<ITermVar> activeVars;

    public ActiveVars(IUnifier unifier) {
        this.unifier = unifier;
        this.activeVars = HashMultiset.create();
    }

    // ---------------------------------------------

    @Override public boolean add(IConstraint constraint) {
        return activeVars.addAll(findActiveVars(constraint));
    }

    public boolean add(ITerm term) {
        return activeVars.addAll(unifier.find(term).getVars());
    }

    @Override public boolean update(final ITermVar var) {
        final int n = activeVars.count(var);
        if(n > 0) {
            activeVars.remove(var);
            final Multiset<ITermVar> newVars = unifier.find(var).getVars();
            for(ITermVar newVar : newVars) {
                activeVars.add(newVar, n);
            }
            return true;
        }
        return false;
    }

    @Override public boolean remove(IConstraint constraint) {
        return Multisets.removeOccurrences(activeVars, findActiveVars(constraint));
    }

    public boolean contains(ITerm term) {
        return unifier.find(term).getVars().stream().anyMatch(activeVars::contains);
    }

    // ---------------------------------------------

    private Multiset<ITermVar> findActiveVars(IConstraint constraint) {
        final Multiset<ITermVar> vars = HashMultiset.create();
        getActiveVars(constraint).stream().map(unifier::find).map(ITerm::getVars).forEach(vars::addAll);
        return vars;
    }

    private static Multiset<ITermVar> getActiveVars(IConstraint constraint) {
        final Multiset<ITermVar> vars = HashMultiset.create();
        constraint.match(
                IConstraint.Cases.of(ActiveVars::getActiveVars, ActiveVars::getActiveVars, ActiveVars::getActiveVars,
                        ActiveVars::getActiveVars, ActiveVars::getActiveVars, ActiveVars::getActiveVars,
                        ActiveVars::getActiveVars, ActiveVars::getActiveVars, ActiveVars::getActiveVars,
                        ActiveVars::getActiveVars))
                .stream().map(ITerm::getVars).forEach(vars::addAll);
        return vars;
    }

    private static Multiset<ITermVar> getActiveVars(IAstConstraint constraint) {
        return constraint.match(IAstConstraint.Cases.of(
            // @formatter:off
            p -> p.getValue().getVars()
            // @formatter:on
        ));
    }

    private static Multiset<ITermVar> getActiveVars(IBaseConstraint constraint) {
        return constraint.match(IBaseConstraint.Cases.of(
            // @formatter:off
            t -> ImmutableMultiset.of(),
            f -> ImmutableMultiset.of(),
            c -> {
                Multiset<ITermVar> activeVars = HashMultiset.create();
                activeVars.addAll(getActiveVars(c.getLeft()));
                activeVars.addAll(getActiveVars(c.getRight()));
                return ImmutableMultiset.copyOf(activeVars);
            },
            e -> {
                Multiset<ITermVar> activeVars = HashMultiset.create(getActiveVars(e.getConstraint()));
                activeVars.removeAll(e.getEVars());
                return ImmutableMultiset.copyOf(activeVars);
            },
            n -> {
                Multiset<ITermVar> activeVars = HashMultiset.create();
                n.getNVars().forEach(v -> activeVars.addAll(v.getVars()));
                return ImmutableMultiset.copyOf(activeVars);
            }
            // @formatter:on
        ));
    }

    private static Multiset<ITermVar> getActiveVars(IEqualityConstraint constraint) {
        return constraint.match(IEqualityConstraint.Cases.of(
            // @formatter:off
            eq -> Multisets.sum(eq.getLeft().getVars(),eq.getRight().getVars()),
            neq -> ImmutableMultiset.of()
            // @formatter:on
        ));
    }

    private static Multiset<ITermVar> getActiveVars(INameResolutionConstraint constraint) {
        return constraint.match(INameResolutionConstraint.Cases.of(
            // @formatter:off
            r -> r.getDeclaration().getVars(),
            a -> a.getScope().getVars(),
            dp -> dp.getValue().getVars()
            // @formatter:on
        ));
    }

    private static Multiset<ITermVar> getActiveVars(@SuppressWarnings("unused") IScopeGraphConstraint constraint) {
        return ImmutableMultiset.of();
    }

    private static Multiset<ITermVar> getActiveVars(IRelationConstraint constraint) {
        return constraint.match(IRelationConstraint.Cases.of(
            // @formatter:off
            br -> ImmutableMultiset.of(),
            cr -> ImmutableMultiset.of(),
            ev -> ev.getResult().getVars()
            // @formatter:on
        ));
    }

    private static Multiset<ITermVar> getActiveVars(@SuppressWarnings("unused") ISetConstraint constraint) {
        return ImmutableMultiset.of();
    }

    private static Multiset<ITermVar> getActiveVars(IPolyConstraint constraint) {
        return constraint.match(IPolyConstraint.Cases.of(
            // @formatter:off
            gen -> Multisets.sum(gen.getDeclaration().getVars(), ImmutableMultiset.of(gen.getGenVars())),
            inst -> {
                Multiset<ITermVar> vars = HashMultiset.create();
                vars.addAll(inst.getType().getVars());
                vars.addAll(inst.getDeclaration().getVars());
                vars.add(inst.getInstVars());
                return vars;
            }
            // @formatter:on
        ));
    }

    private static Multiset<ITermVar> getActiveVars(@SuppressWarnings("unused") IControlFlowConstraint constraint) {
        return ImmutableMultiset.of();
    }

    private static Multiset<ITermVar> getActiveVars(@SuppressWarnings("unused") ISymbolicConstraint constraint) {
        return ImmutableMultiset.of();
    }

}