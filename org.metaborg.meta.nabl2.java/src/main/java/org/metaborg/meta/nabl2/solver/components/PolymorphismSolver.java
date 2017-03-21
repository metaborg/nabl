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
import org.metaborg.meta.nabl2.unification.UnificationException;
import org.metaborg.meta.nabl2.util.Unit;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
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

    @Override protected Iterable<IPolyConstraint> doFinish(IMessageInfo messageInfo) {
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
        ITerm scheme = generalize(type);
        try {
            unifier().removeActive(gen.getScheme(), gen); // before `unify`, so that we don't cause an error chain if
                                                          // that fails
            unifier().unify(gen.getScheme(), scheme);
        } catch(UnificationException ex) {
            throw new UnsatisfiableException(gen.getMessageInfo().withDefaultContent(ex.getMessageContent()));
        }
        return true;
    }

    private ITerm generalize(ITerm type) {
        BiMap<ITermVar, TypeVar> subst = HashBiMap.create();
        int c = 0;
        for(ITermVar var : type.getVars()) {
            subst.put(var, ImmutableTypeVar.of("T" + (++c)));
        }
        ITerm scheme = subst.isEmpty() ? type : ImmutableForall.of(subst.values(), subst(type, subst));
        return scheme;
    }

    private boolean solve(CInstantiate inst) throws UnsatisfiableException {
        if(!complete) {
            return false;
        }
        ITerm scheme = unifier().find(inst.getScheme());
        if(unifier().isActive(scheme, inst)) {
            return false;
        }
        unifier().removeActive(scheme, inst);
        final Optional<Forall> forall = Forall.matcher().match(scheme);
        final ITerm type;
        if(forall.isPresent()) {
            type = instantiate(forall.get());
        } else {
            type = scheme;
        }
        try {
            unifier().removeActive(inst.getType(), inst); // before `unify`, so that we don't cause an error chain if
                                                          // that fails
            unifier().unify(inst.getType(), type);
        } catch(UnificationException ex) {
            throw new UnsatisfiableException(inst.getMessageInfo().withDefaultContent(ex.getMessageContent()));
        }
        return true;
    }

    private ITerm instantiate(Forall scheme) {
        Map<TypeVar, ITermVar> mapping = Maps.newHashMap();
        scheme.getTypeVars().stream().forEach(v -> {
            mapping.put(v, fresh().apply(v.getName()));
        });
        ITerm type = subst(scheme.getType(), mapping);
        return type;
    }

    private ITerm subst(ITerm term, Map<? extends ITerm, ? extends ITerm> subst) {
        return M.sometd(
            // @formatter:off
            t -> subst.containsKey(t) ? Optional.of(subst.get(t)) : Optional.empty()
            // @formatter:on
        ).apply(term);
    }

}