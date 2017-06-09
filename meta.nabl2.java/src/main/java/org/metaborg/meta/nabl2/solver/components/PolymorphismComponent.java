package org.metaborg.meta.nabl2.solver.components;

import java.util.Map;
import java.util.Optional;

import org.metaborg.meta.nabl2.constraints.equality.ImmutableCEqual;
import org.metaborg.meta.nabl2.constraints.poly.CGeneralize;
import org.metaborg.meta.nabl2.constraints.poly.CInstantiate;
import org.metaborg.meta.nabl2.constraints.poly.IPolyConstraint;
import org.metaborg.meta.nabl2.poly.Forall;
import org.metaborg.meta.nabl2.poly.ImmutableForall;
import org.metaborg.meta.nabl2.poly.ImmutableTypeVar;
import org.metaborg.meta.nabl2.poly.TypeVar;
import org.metaborg.meta.nabl2.solver.ASolver;
import org.metaborg.meta.nabl2.solver.ISolver.SolveResult;
import org.metaborg.meta.nabl2.solver.SolverCore;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.util.functions.Predicate1;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.Maps;

public class PolymorphismComponent extends ASolver {

    private final Predicate1<ITerm> isTermInactive;

    public PolymorphismComponent(SolverCore core, Predicate1<ITerm> isTermInactive) {
        super(core);
        this.isTermInactive = isTermInactive;
    }

    public Optional<SolveResult> solve(IPolyConstraint constraint) {
        return constraint.match(IPolyConstraint.Cases.of(this::solve, this::solve));
    }

    public Unit finish() {
        return Unit.unit;
    }

    // ------------------------------------------------------------------------------------------------------//

    private Optional<SolveResult> solve(CGeneralize gen) {
        final ITerm type = find(gen.getType());
        if(!isTermInactive.test(type)) {
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
                ImmutableCEqual.of(gen.getScheme(), scheme, gen.getMessageInfo()),
                ImmutableCEqual.of(gen.getGenVars(), TB.newList(subst.keySet()), gen.getMessageInfo())
                // @formatter:on
        );
        return Optional.of(result);
    }

    private Optional<SolveResult> solve(CInstantiate inst) {
        final ITerm schemeTerm = find(inst.getScheme());
        if(!isTermInactive.test(schemeTerm)) {
            return Optional.empty();
        }
        final Optional<Forall> forall = Forall.matcher().match(schemeTerm);
        final ITerm type;
        final Map<TypeVar, ITermVar> subst = Maps.newLinkedHashMap(); // linked map to preserve key order
        if(forall.isPresent()) {
            final Forall scheme = forall.get();
            scheme.getTypeVars().stream().forEach(v -> {
                subst.put(v, fresh(v.getName()));
            });
            type = subst(scheme.getType(), subst);
        } else {
            type = schemeTerm;
        }
        SolveResult result = SolveResult.constraints(
                // @formatter:off
                ImmutableCEqual.of(inst.getType(), type, inst.getMessageInfo()),
                ImmutableCEqual.of(inst.getInstVars(), TB.newList(subst.keySet()), inst.getMessageInfo())
                // @formatter:on
        );
        return Optional.of(result);
    }

    private ITerm subst(ITerm term, Map<? extends ITerm, ? extends ITerm> subst) {
        return M.sometd(
            // @formatter:off
            t -> subst.containsKey(t) ? Optional.of(subst.get(t)) : Optional.empty()
            // @formatter:on
        ).apply(term);
    }

}