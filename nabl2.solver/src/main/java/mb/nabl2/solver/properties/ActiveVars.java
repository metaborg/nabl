package mb.nabl2.solver.properties;

import java.util.Collection;

import org.metaborg.util.Ref;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.ast.IAstConstraint;
import mb.nabl2.constraints.base.IBaseConstraint;
import mb.nabl2.constraints.equality.IEqualityConstraint;
import mb.nabl2.constraints.nameresolution.INameResolutionConstraint;
import mb.nabl2.constraints.poly.IPolyConstraint;
import mb.nabl2.constraints.relations.IRelationConstraint;
import mb.nabl2.constraints.sets.ISetConstraint;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.collection.VarMultiset;
import mb.nabl2.terms.unification.IUnifier;

public class ActiveVars implements IConstraintSetProperty {

    private final Ref<? extends IUnifier> unifier;
    private final VarMultiset activeVars;

    public ActiveVars(Ref<? extends IUnifier> unifier) {
        this.unifier = unifier;
        this.activeVars = new VarMultiset();
    }

    // ---------------------------------------------

    @Override public boolean add(IConstraint constraint) {
        final Multiset<ITermVar> vars = findActiveVars(constraint);
        boolean change = false;
        for(ITermVar var : vars) {
            change |= add(var);
        }
        return change;
    }

    public boolean add(ITerm term) {
        return activeVars.addAll(term.getVars(), unifier.get());
    }

    @Override public boolean update(final Collection<ITermVar> vars) {
        return activeVars.update(vars, unifier.get());
    }

    @Override public boolean remove(IConstraint constraint) {
        final Multiset<ITermVar> vars = findActiveVars(constraint);
        return activeVars.removeAll(vars, unifier.get());
    }

    public boolean isNotActive(ITerm term) {
        return term.getVars().elementSet().stream().noneMatch(var -> activeVars.contains(var, unifier.get()));
    }

    // ---------------------------------------------

    private Multiset<ITermVar> findActiveVars(IConstraint constraint) {
        final Multiset<ITermVar> vars = HashMultiset.create();
        getActiveVars(constraint).stream().map(t -> unifier.get().findRecursive(t)).map(ITerm::getVars)
                .forEach(vars::addAll);
        return vars;
    }

    private static Multiset<ITermVar> getActiveVars(IConstraint constraint) {
        final Multiset<ITermVar> vars = HashMultiset.create();
        constraint.match(
                IConstraint.Cases.of(ActiveVars::getActiveVars, ActiveVars::getActiveVars, ActiveVars::getActiveVars,
                        ActiveVars::emptyActiveVars, ActiveVars::getActiveVars, ActiveVars::getActiveVars,
                        ActiveVars::getActiveVars, ActiveVars::emptyActiveVars, ActiveVars::getActiveVars))
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

    private static Multiset<ITermVar> getActiveVars(IRelationConstraint constraint) {
        return constraint.match(IRelationConstraint.Cases.of(
        // @formatter:off
            br -> ImmutableMultiset.of(),
            cr -> ImmutableMultiset.of(),
            ev -> ev.getResult().getVars()
            // @formatter:on
        ));
    }

    private static Multiset<ITermVar> getActiveVars(ISetConstraint constraint) {
        return constraint.match(ISetConstraint.Cases.of(
        // @formatter:off
            subseteq -> ImmutableMultiset.of(),
            distinct -> ImmutableMultiset.of(),
            eval -> eval.getResult().getVars()
            // @formatter:on
        ));
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

    private static Multiset<ITermVar> emptyActiveVars(@SuppressWarnings("unused") IConstraint constraint) {
        return ImmutableMultiset.of();
    }

}
