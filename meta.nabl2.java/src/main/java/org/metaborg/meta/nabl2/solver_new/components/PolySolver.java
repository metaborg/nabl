package org.metaborg.meta.nabl2.solver_new.components;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.metaborg.meta.nabl2.constraints.poly.CGeneralize;
import org.metaborg.meta.nabl2.constraints.poly.CInstantiate;
import org.metaborg.meta.nabl2.constraints.poly.IPolyConstraint;
import org.metaborg.meta.nabl2.poly.Forall;
import org.metaborg.meta.nabl2.poly.ImmutableForall;
import org.metaborg.meta.nabl2.poly.ImmutableTypeVar;
import org.metaborg.meta.nabl2.poly.TypeVar;
import org.metaborg.meta.nabl2.solver_new.ASolver;
import org.metaborg.meta.nabl2.solver_new.SolverCore;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.meta.nabl2.util.Unit;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class PolySolver extends ASolver<IPolyConstraint, Unit> {

    private final Set<IPolyConstraint> constraints;

    public PolySolver(SolverCore core) {
        super(core);
        this.constraints = Sets.newHashSet();
    }

    @Override public boolean add(IPolyConstraint constraint) throws InterruptedException {
        return constraint.match(IPolyConstraint.Cases.of(this::add, this::add));
    }

    @Override public boolean iterate() throws InterruptedException {
        return doIterate(constraints, this::solve);
    }

    public Unit finish() {
        return Unit.unit;
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean add(CGeneralize gen) {
        tracker().addActive(gen.getScheme(), gen);
        constraints.add(gen);
        return true;
    }

    private boolean add(CInstantiate inst) {
        tracker().addActive(inst.getType(), inst);
        tracker().addActive(inst.getScheme(), inst);
        constraints.add(inst);
        return true;
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean solve(IPolyConstraint constraint) {
        return constraint.match(IPolyConstraint.Cases.of(this::solve, this::solve));
    }

    private boolean solve(CGeneralize gen) {
        ITerm type = find(gen.getType());
        if(tracker().isActive(type)) {
            return false;
        }
        tracker().freeze(type);
        final Map<ITermVar, TypeVar> subst = Maps.newLinkedHashMap();
        final ITerm scheme;
        {
            int c = 0;
            for(ITermVar var : type.getVars()) {
                subst.put(var, ImmutableTypeVar.of("T" + (++c)));
            }
            scheme = subst.isEmpty() ? type : ImmutableForall.of(subst.values(), subst(type, subst));
        }
        tracker().removeActive(gen.getScheme(), gen);
        unify(gen.getScheme(), scheme, gen.getMessageInfo());
        unify(gen.getGenVars(), TB.newList(subst.keySet()), gen.getMessageInfo());
        return true;
    }

    private boolean solve(CInstantiate inst) {
        ITerm schemeTerm = find(inst.getScheme());
        if(tracker().isActive(schemeTerm, inst)) {
            return false;
        }
        tracker().removeActive(schemeTerm, inst);
        final Optional<Forall> forall = Forall.matcher().match(schemeTerm);
        final ITerm type;
        final Map<TypeVar, ITermVar> subst = Maps.newLinkedHashMap();
        if(forall.isPresent()) {
            final Forall scheme = forall.get();
            scheme.getTypeVars().stream().forEach(v -> {
                subst.put(v, fresh(v.getName()));
            });
            type = subst(scheme.getType(), subst);
        } else {
            type = schemeTerm;
        }
        tracker().removeActive(inst.getType(), inst);
        unify(inst.getType(), type, inst.getMessageInfo());
        unify(inst.getInstVars(), TB.newList(subst.values()), inst.getMessageInfo());
        return true;
    }

    private ITerm subst(ITerm term, Map<? extends ITerm, ? extends ITerm> subst) {
        return M.sometd(
            // @formatter:off
            t -> subst.containsKey(t) ? Optional.of(subst.get(t)) : Optional.empty()
            // @formatter:on
        ).apply(term);
    }

}