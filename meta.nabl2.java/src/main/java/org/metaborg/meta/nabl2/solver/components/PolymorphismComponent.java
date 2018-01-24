package org.metaborg.meta.nabl2.solver.components;

import java.util.Map;
import java.util.Optional;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.base.ImmutableCConj;
import org.metaborg.meta.nabl2.constraints.base.ImmutableCExists;
import org.metaborg.meta.nabl2.constraints.equality.ImmutableCEqual;
import org.metaborg.meta.nabl2.constraints.messages.MessageInfo;
import org.metaborg.meta.nabl2.constraints.namebinding.DeclProperties;
import org.metaborg.meta.nabl2.constraints.nameresolution.ImmutableCDeclProperty;
import org.metaborg.meta.nabl2.constraints.poly.CGeneralize;
import org.metaborg.meta.nabl2.constraints.poly.CInstantiate;
import org.metaborg.meta.nabl2.constraints.poly.IPolyConstraint;
import org.metaborg.meta.nabl2.poly.Forall;
import org.metaborg.meta.nabl2.poly.ImmutableForall;
import org.metaborg.meta.nabl2.poly.ImmutableTypeVar;
import org.metaborg.meta.nabl2.poly.TypeVar;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.solver.ASolver;
import org.metaborg.meta.nabl2.solver.ISolver.SolveResult;
import org.metaborg.meta.nabl2.solver.SolverCore;
import org.metaborg.meta.nabl2.solver.TypeException;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.util.functions.PartialFunction2;
import org.metaborg.util.functions.Predicate1;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.Maps;

public class PolymorphismComponent extends ASolver {

    private final Predicate1<ITerm> isGenSafe;
    private final Predicate1<Occurrence> isInstSafe;
    private final PartialFunction2<Occurrence, ITerm, ITerm> getDeclProp;

    public PolymorphismComponent(SolverCore core, Predicate1<ITerm> isGenSafe, Predicate1<Occurrence> isInstSafe,
            PartialFunction2<Occurrence, ITerm, ITerm> getDeclProp) {
        super(core);
        this.isGenSafe = isGenSafe;
        this.isInstSafe = isInstSafe;
        this.getDeclProp = getDeclProp;
    }

    public Optional<SolveResult> solve(IPolyConstraint constraint) {
        return constraint.match(IPolyConstraint.Cases.of(this::solve, this::solve));
    }

    public Unit finish() {
        return Unit.unit;
    }

    // ------------------------------------------------------------------------------------------------------//

    private Optional<SolveResult> solve(CGeneralize gen) {
        final ITerm declTerm = find(gen.getDeclaration());
        if(!declTerm.isGround()) {
            return Optional.empty();
        }
        final Occurrence decl = Occurrence.matcher().match(declTerm)
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + gen));

        final ITerm type = find(gen.getType());
        if(!isGenSafe.test(type)) {
            return Optional.empty();
        }

        final Map<ITermVar, TypeVar> subst = Maps.newLinkedHashMap(); // linked map to preserve key order
        final ITerm scheme;
        {
            int c = 0;
            for(ITermVar var : type.getVars()) {
                subst.put(var, ImmutableTypeVar.of("T" + (++c)));
            }
            scheme = subst.isEmpty() ? type : ImmutableForall.of(subst.values(), subst(type, subst));
        }

        SolveResult result = SolveResult.constraints(
                // @formatter:off
                ImmutableCDeclProperty.of(decl, DeclProperties.TYPE_KEY, scheme, 0, gen.getMessageInfo()),
                ImmutableCEqual.of(gen.getGenVars(), TB.newList(subst.keySet()), gen.getMessageInfo())
                // @formatter:on
        );
        return Optional.of(result);
    }

    private Optional<SolveResult> solve(CInstantiate inst) {
        final ITerm declTerm = find(inst.getDeclaration());
        if(!declTerm.isGround()) {
            return Optional.empty();
        }
        final Occurrence decl = Occurrence.matcher().match(declTerm)
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + inst));

        if(!isInstSafe.test(decl)) {
            return Optional.empty();
        }

        final Optional<ITerm> schemeTerm = getDeclProp.apply(decl, DeclProperties.TYPE_KEY);
        if(!schemeTerm.isPresent()) {
            return Optional.empty();
        }

        final Optional<Forall> forall = Forall.matcher().match(schemeTerm.get());
        final ITerm type;
        final Map<TypeVar, ITermVar> subst = Maps.newLinkedHashMap(); // linked map to preserve key order
        if(forall.isPresent()) {
            final Forall scheme = forall.get();
            scheme.getTypeVars().stream().forEach(v -> {
                subst.put(v, TB.newVar("", fresh(v.getName())));
            });
            type = subst(scheme.getType(), subst);
        } else {
            type = schemeTerm.get();
        }

        final IConstraint constraint =
                // @formatter:off
                ImmutableCExists.of(subst.values(),
                        ImmutableCConj.of(
                                ImmutableCEqual.of(inst.getType(), type, inst.getMessageInfo()),
                                ImmutableCEqual.of(inst.getInstVars(), TB.newList(subst.keySet()), inst.getMessageInfo()),
                                MessageInfo.empty()
                        ),
                        inst.getMessageInfo());
                // @formatter:on
        SolveResult result = SolveResult.constraints(constraint);
        return Optional.of(result);
    }

    private ITerm subst(ITerm term, Map<? extends ITerm, ? extends ITerm> subst) {
        return M.sometd(
        // @formatter:off
            t -> subst.containsKey(t) ? Optional.of(subst.get(t)) : Optional.empty()
            // @formatter:on
        ).apply(term);
    }

    // ------------------------------------------------------------------------------------------------------//

}
