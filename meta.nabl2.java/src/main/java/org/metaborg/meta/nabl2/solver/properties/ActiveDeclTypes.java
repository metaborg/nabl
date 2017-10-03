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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;

/**
 * Track active declaration types.
 */
public class ActiveDeclTypes implements IConstraintSetProperty {

    private final IUnifier unifier;

    public ActiveDeclTypes(IUnifier unifier) {
        this.unifier = unifier;
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

    public boolean contains(IOccurrence decl) {
        // TODO decl.type is active
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
                if(dp.getKey().equals(AnalysisTerms.TYPE_KEY)) {
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