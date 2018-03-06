package org.metaborg.meta.nabl2.solver.properties;

import java.util.Collection;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.base.IBaseConstraint;
import org.metaborg.meta.nabl2.constraints.namebinding.DeclProperties;
import org.metaborg.meta.nabl2.constraints.nameresolution.INameResolutionConstraint;
import org.metaborg.meta.nabl2.constraints.poly.IPolyConstraint;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.collection.TermMultiset;
import org.metaborg.meta.nabl2.terms.unification.IUnifier;
import org.metaborg.util.Ref;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;

/**
 * Track active declaration types.
 */
public class ActiveDeclTypes implements IConstraintSetProperty {

    private final Ref<? extends IUnifier> unifier;
    private final TermMultiset activeDecls;

    public ActiveDeclTypes(Ref<? extends IUnifier> unifier) {
        this.unifier = unifier;
        this.activeDecls = new TermMultiset();
    }

    // ---------------------------------------------

    @Override public boolean add(IConstraint constraint) {
        final Multiset<ITerm> addedDecls = getActiveDecls(constraint);
        for(Entry<ITerm> e : addedDecls.entrySet()) {
            activeDecls.add(e.getElement(), e.getCount(), unifier.get());
        }
        return !addedDecls.isEmpty();
    }

    // ---------------------------------------------

    @Override public boolean update(final Collection<ITermVar> vars) {
        return activeDecls.update(vars, unifier.get());
    }

    // ---------------------------------------------

    @Override public boolean remove(IConstraint constraint) {
        boolean change = false;
        for(Entry<ITerm> e : getActiveDecls(constraint).entrySet()) {
            change |= activeDecls.remove(e.getElement(), e.getCount(), unifier.get()) > 0;
        }
        return change;
    }

    public boolean isNotActive(Occurrence decl) {
        return activeDecls.varSet().isEmpty() && !activeDecls.contains(decl, unifier.get());
    }

    // ---------------------------------------------

    private static Multiset<ITerm> getActiveDecls(IConstraint constraint) {
        return constraint.match(IConstraint.Cases.of(ActiveDeclTypes::emptyActiveDecls, ActiveDeclTypes::getActiveDecls,
                ActiveDeclTypes::emptyActiveDecls, ActiveDeclTypes::emptyActiveDecls, ActiveDeclTypes::getActiveDecls,
                ActiveDeclTypes::emptyActiveDecls, ActiveDeclTypes::emptyActiveDecls, ActiveDeclTypes::emptyActiveDecls,
                ActiveDeclTypes::getActiveDecls, ActiveDeclTypes::emptyActiveDecls));
    }

    private static Multiset<ITerm> emptyActiveDecls(@SuppressWarnings("unused") IConstraint constraint) {
        return ImmutableMultiset.of();
    }

    private static Multiset<ITerm> getActiveDecls(IBaseConstraint constraint) {
        return constraint.match(IBaseConstraint.Cases.of(
        // @formatter:off
            t -> ImmutableMultiset.of(),
            f -> ImmutableMultiset.of(),
            c -> {
                Multiset<ITerm> activeDecls = HashMultiset.create();
                activeDecls.addAll(getActiveDecls(c.getLeft()));
                activeDecls.addAll(getActiveDecls(c.getRight()));
                return ImmutableMultiset.copyOf(activeDecls);
            },
            e -> getActiveDecls(e.getConstraint()),
            n -> ImmutableMultiset.of()
            // @formatter:on
        ));
    }

    private static Multiset<ITerm> getActiveDecls(INameResolutionConstraint constraint) {
        return constraint.match(INameResolutionConstraint.Cases.of(
        // @formatter:off
            r -> ImmutableMultiset.of(),
            a -> ImmutableMultiset.of(),
            dp -> {
                if(dp.getKey().equals(DeclProperties.TYPE_KEY)) {
                    return ImmutableMultiset.of(dp.getDeclaration());
                } else {
                    return ImmutableMultiset.of();
                }
            }
            // @formatter:on
        ));
    }

    private static Multiset<ITerm> getActiveDecls(IPolyConstraint constraint) {
        return constraint.match(IPolyConstraint.Cases.of(
        // @formatter:off
            gen -> ImmutableMultiset.of(gen.getDeclaration()),
            inst -> ImmutableMultiset.of()
            // @formatter:on
        ));
    }

}
