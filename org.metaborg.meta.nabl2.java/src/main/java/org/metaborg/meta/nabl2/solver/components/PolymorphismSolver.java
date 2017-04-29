package org.metaborg.meta.nabl2.solver.components;

import static org.metaborg.meta.nabl2.util.Unit.unit;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.poly.CGeneralize;
import org.metaborg.meta.nabl2.constraints.poly.CInstantiate;
import org.metaborg.meta.nabl2.constraints.poly.IPolyConstraint;
import org.metaborg.meta.nabl2.constraints.poly.IPolyConstraint.CheckedCases;
import org.metaborg.meta.nabl2.poly.Forall;
import org.metaborg.meta.nabl2.poly.ImmutableForall;
import org.metaborg.meta.nabl2.poly.ImmutableTypeVar;
import org.metaborg.meta.nabl2.poly.TypeVar;
import org.metaborg.meta.nabl2.solver.Solver;
import org.metaborg.meta.nabl2.solver.SolverComponent;
import org.metaborg.meta.nabl2.solver.UnsatisfiableException;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.meta.nabl2.unification.UnificationException;
import org.metaborg.meta.nabl2.util.Unit;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class PolymorphismSolver extends SolverComponent<IPolyConstraint> {

    private final Set<IPolyConstraint> defered;

    private boolean complete = false;

    public PolymorphismSolver(Solver solver) {
        super(solver);
        this.defered = Sets.newHashSet();
    }

    // ------------------------------------------------------------------------------------------------------//

    @Override protected Unit doAdd(IPolyConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(CheckedCases.of(this::add, this::add));
    }

    @Override protected boolean doIterate() throws UnsatisfiableException, InterruptedException {
        complete = true;
        return doIterate(defered, this::solve);
    }

    @Override protected Set<? extends IPolyConstraint> doFinish(IMessageInfo messageInfo) {
        return defered;
    }

    // ------------------------------------------------------------------------------------------------------//

    private Unit add(CGeneralize gen) throws UnsatisfiableException {
        unifier().addActive(gen.getScheme(), gen);
        defered.add(gen);
        return unit;
    }

    private Unit add(CInstantiate inst) throws UnsatisfiableException {
        unifier().addActive(inst.getType(), inst);
        unifier().addActive(inst.getScheme(), inst);
        defered.add(inst);
        return unit;
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean solve(IPolyConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(CheckedCases.of(this::solve, this::solve));
    }

    private boolean solve(CGeneralize gen) throws UnsatisfiableException {
        if(!complete) {
            return false;
        }
        ITerm type = unifier().find(gen.getType());
        if(unifier().isActive(type)) {
            return false;
        }
        unifier().freeze(type);
        final Map<ITermVar, TypeVar> subst = Maps.newLinkedHashMap();
        final ITerm scheme;
        {
            int c = 0;
            for(ITermVar var : type.getVars()) {
                subst.put(var, ImmutableTypeVar.of("T" + (++c)));
            }
            scheme = subst.isEmpty() ? type : ImmutableForall.of(subst.values(), subst(type, subst));
        }
        try {
            unifier().removeActive(gen.getScheme(), gen); // before `unify`, so that we don't cause an error chain if
                                                          // that fails
            unifier().unify(gen.getScheme(), scheme);
            unifier().unify(gen.getGenVars(), TB.newList(subst.keySet()));
        } catch(UnificationException ex) {
            throw new UnsatisfiableException(gen.getMessageInfo().withDefaultContent(ex.getMessageContent()));
        }
        return true;
    }

    private boolean solve(CInstantiate inst) throws UnsatisfiableException {
        if(!complete) {
            return false;
        }
        ITerm schemeTerm = unifier().find(inst.getScheme());
        if(unifier().isActive(schemeTerm, inst)) {
            return false;
        }
        unifier().removeActive(schemeTerm, inst);
        final Optional<Forall> forall = Forall.matcher().match(schemeTerm);
        final ITerm type;
        final Map<TypeVar, ITermVar> subst = Maps.newLinkedHashMap();
        if(forall.isPresent()) {
            final Forall scheme = forall.get();
            scheme.getTypeVars().stream().forEach(v -> {
                subst.put(v, fresh().apply(v.getName()));
            });
            type = subst(scheme.getType(), subst);
        } else {
            type = schemeTerm;
        }
        try {
            unifier().removeActive(inst.getType(), inst); // before `unify`, so that we don't cause an error chain if
                                                          // that fails
            unifier().unify(inst.getType(), type);
            unifier().unify(inst.getInstVars(), TB.newList(subst.values()));
        } catch(UnificationException ex) {
            throw new UnsatisfiableException(inst.getMessageInfo().withDefaultContent(ex.getMessageContent()));
        }
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