package org.metaborg.meta.nabl2.solver.properties;

import java.util.Optional;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.base.IBaseConstraint;
import org.metaborg.meta.nabl2.constraints.nameresolution.INameResolutionConstraint;
import org.metaborg.meta.nabl2.constraints.poly.IPolyConstraint;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.solver.TypeException;
import org.metaborg.meta.nabl2.spoofax.analysis.AnalysisTerms;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.unification.IUnifier;
import org.metaborg.util.functions.Predicate1;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

/**
 * Track dependencies between vars and declaration types.
 */
public class DeclTypeDeps implements IConstraintSetProperty {

    private final IUnifier unifier;
    private final Predicate1<Occurrence> isDeclTypeActive;

    public DeclTypeDeps(IUnifier unifier, Predicate1<Occurrence> isDeclTypeActive) {
        this.unifier = unifier;
        this.isDeclTypeActive = isDeclTypeActive;
    }

    // ---------------------------------------------

    @Override public boolean add(IConstraint constraint) {
        return false;
    }

    // ---------------------------------------------

    @Override public boolean update(final ITermVar var) {
        return false;
    }

    // ---------------------------------------------


    @Override public boolean remove(IConstraint constraint) {
        return false;
    }

    public boolean contains(ITerm term) {
        // TODO: vars(t) is not determined by any unknown or active decl.type
        return true;
    }

    // ---------------------------------------------

    private Optional<IOccurrence> findDeclaration(ITerm term) {
        final ITerm declTerm = unifier.find(term);
        if(!declTerm.isGround()) {
            return Optional.empty();
        }
        IOccurrence decl = Occurrence.matcher().match(declTerm)
                .orElseThrow(() -> new TypeException("Exepected occurrence, got " + declTerm));
        return Optional.of(decl);
    }

    // ---------------------------------------------

    private static Multimap<ITermVar, ITerm> getVarDeps(IConstraint constraint) {
        return constraint.match(
                IConstraint.Cases.of(DeclTypeDeps::emptyVarDeps, DeclTypeDeps::getVarDeps, DeclTypeDeps::emptyVarDeps,
                        DeclTypeDeps::emptyVarDeps, DeclTypeDeps::getVarDeps, DeclTypeDeps::emptyVarDeps,
                        DeclTypeDeps::emptyVarDeps, DeclTypeDeps::emptyVarDeps, DeclTypeDeps::getVarDeps,
                        DeclTypeDeps::emptyVarDeps));
    }

    private static Multimap<ITermVar, ITerm> emptyVarDeps(@SuppressWarnings("unused") IConstraint constraint) {
        return ImmutableMultimap.of();
    }

    private static Multimap<ITermVar, ITerm> getVarDeps(IBaseConstraint constraint) {
        return constraint.match(IBaseConstraint.Cases.of(
        // @formatter:off
            t -> ImmutableMultimap.of(),
            f -> ImmutableMultimap.of(),
            c -> {
                Multimap<ITermVar, ITerm> determinedVars = HashMultimap.create();
                determinedVars.putAll(getVarDeps(c.getLeft()));
                determinedVars.putAll(getVarDeps(c.getRight()));
                return ImmutableMultimap.copyOf(determinedVars);
            },
            e -> {
                final Multimap<ITermVar, ITerm> determinedVars =
                        HashMultimap.create(getVarDeps(e.getConstraint()));
                determinedVars.removeAll(e.getEVars());
                return ImmutableMultimap.copyOf(determinedVars);
            },
            n -> ImmutableMultimap.of()
            // @formatter:on
        ));
    }

    private static Multimap<ITermVar, ITerm> getVarDeps(INameResolutionConstraint constraint) {
        return constraint.match(INameResolutionConstraint.Cases.of(
        // @formatter:off
            r -> ImmutableMultimap.of(),
            a -> ImmutableMultimap.of(),
            dp -> {
                if(dp.getKey().equals(AnalysisTerms.TYPE_KEY)) {
                    final Multimap<ITermVar, ITerm> determinedVars = HashMultimap.create();
                    for(ITermVar v : dp.getValue().getVars()) {
                        determinedVars.put(v, dp.getDeclaration());
                    }
                    return ImmutableMultimap.copyOf(determinedVars);
                } else {
                    return ImmutableMultimap.of();
                }
            }
            // @formatter:on
        ));
    }

    private static Multimap<ITermVar, ITerm> getVarDeps(IPolyConstraint constraint) {
        return constraint.match(IPolyConstraint.Cases.of(
        // @formatter:off
            gen -> ImmutableMultimap.of(),
            inst -> {
                final Multimap<ITermVar, ITerm> determinedVars = HashMultimap.create();
                for(ITermVar v : inst.getType().getVars()) {
                    determinedVars.put(v, inst.getDeclaration());
                }
                return ImmutableMultimap.copyOf(determinedVars);
            }
            // @formatter:on
        ));
    }

}