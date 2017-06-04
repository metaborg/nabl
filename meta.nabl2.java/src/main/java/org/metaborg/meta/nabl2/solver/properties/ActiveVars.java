package org.metaborg.meta.nabl2.solver.properties;

import java.util.Collections;
import java.util.Set;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.ast.IAstConstraint;
import org.metaborg.meta.nabl2.constraints.base.IBaseConstraint;
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
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

public class ActiveVars implements IConstraintSetProperty {

    private final IUnifier unifier;
    private final Multiset<ITermVar> activeVars;

    public ActiveVars(IUnifier unifier) {
        this.unifier = unifier;
        this.activeVars = HashMultiset.create();
    }

    // ---------------------------------------------

    @Override public boolean add(IConstraint constraint) {
        boolean change = false;
        for(ITermVar var : findActiveVars(constraint)) {
            change |= activeVars.add(var);
        }
        return change;
    }

    public boolean update(final ITermVar var) {
        final int n = activeVars.count(var);
        if(n > 0) {
            activeVars.remove(var, n);
            final Set<ITermVar> newVars = unifier.find(var).getVars();
            for(ITermVar newVar : newVars) {
                activeVars.add(newVar, n);
            }
            return true;
        }
        return false;
    }

    @Override public boolean remove(IConstraint constraint) {
        boolean change = false;
        for(ITermVar var : findActiveVars(constraint)) {
            change |= activeVars.remove(var);
        }
        return change;
    }

    public boolean contains(ITermVar var) {
        return activeVars.contains(var);
    }

    // ---------------------------------------------

    private Set<ITermVar> findActiveVars(IConstraint constraint) {
        final Set<ITermVar> vars = Sets.newHashSet();
        constraint
                .match(IConstraint.Cases.<Set<ITermVar>>of(ActiveVars::getActiveVars, ActiveVars::getActiveVars,
                        ActiveVars::getActiveVars, ActiveVars::getActiveVars, ActiveVars::getActiveVars,
                        ActiveVars::getActiveVars, ActiveVars::getActiveVars, ActiveVars::getActiveVars,
                        ActiveVars::getActiveVars))
                .stream().map(unifier::find).map(ITerm::getVars).forEach(vars::addAll);
        return vars;
    }

    private static Set<ITermVar> getActiveVars(IAstConstraint constraint) {
        return Collections.emptySet();
    }

    private static Set<ITermVar> getActiveVars(IBaseConstraint constraint) {
        return Collections.emptySet();
    }

    private static Set<ITermVar> getActiveVars(IEqualityConstraint constraint) {
        return constraint.match(IEqualityConstraint.Cases.of(
            // @formatter:off
            eq -> eq.getLeft().getVars().__insertAll(eq.getRight().getVars()),
            neq -> Collections.emptySet()
            // @formatter:on
        ));
    }

    private static Set<ITermVar> getActiveVars(INameResolutionConstraint constraint) {
        return constraint.match(INameResolutionConstraint.Cases.of(
            // @formatter:off
            r -> r.getDeclaration().getVars(),
            a -> a.getScope().getVars(),
            dp -> dp.getValue().getVars()
            // @formatter:on
        ));
    }

    private static Set<ITermVar> getActiveVars(IScopeGraphConstraint constraint) {
        return Collections.emptySet();
    }

    private static Set<ITermVar> getActiveVars(IRelationConstraint constraint) {
        return constraint.match(IRelationConstraint.Cases.of(
            // @formatter:off
            br -> Collections.emptySet(),
            cr -> Collections.emptySet(),
            ev -> ev.getResult().getVars()
            // @formatter:on
        ));
    }

    private static Set<ITermVar> getActiveVars(ISetConstraint constraint) {
        return Collections.emptySet();
    }

    private static Set<ITermVar> getActiveVars(IPolyConstraint constraint) {
        return constraint.match(IPolyConstraint.Cases.of(
            // @formatter:off
            gen -> gen.getScheme().getVars().__insert(gen.getGenVars()),
            inst -> inst.getType().getVars().__insert(inst.getInstVars())
            // @formatter:on
        ));
    }

    private static Set<ITermVar> getActiveVars(ISymbolicConstraint constraint) {
        return Collections.emptySet();
    }

}