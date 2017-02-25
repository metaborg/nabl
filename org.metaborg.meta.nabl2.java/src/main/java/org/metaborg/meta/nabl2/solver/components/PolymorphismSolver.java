package org.metaborg.meta.nabl2.solver.components;

import static org.metaborg.meta.nabl2.util.Unit.unit;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.poly.CGeneralize;
import org.metaborg.meta.nabl2.constraints.poly.CInstantiate;
import org.metaborg.meta.nabl2.constraints.poly.IPolyConstraint;
import org.metaborg.meta.nabl2.constraints.poly.IPolyConstraint.CheckedCases;
import org.metaborg.meta.nabl2.constraints.poly.ImmutableCGeneralize;
import org.metaborg.meta.nabl2.constraints.poly.ImmutableCInstantiate;
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
        return defered.stream().map(this::find).collect(Collectors.toList());
    }

    private IPolyConstraint find(IPolyConstraint constraint) {
        return constraint.match(IPolyConstraint.Cases.of(
            // @formatter:off
            gen -> ImmutableCGeneralize.of(
                        unifier().find(gen.getScheme()),
                        unifier().find(gen.getType()),
                        gen.getMessageInfo().apply(unifier()::find)),
            inst -> ImmutableCInstantiate.of(
                        unifier().find(inst.getType()),
                        unifier().find(inst.getScheme()),
                        inst.getMessageInfo().apply(unifier()::find))
            // @formatter:on
        ));
    }

    // ------------------------------------------------------------------------------------------------------//

    private Unit add(CGeneralize gen) throws UnsatisfiableException {
        defered.add(gen);
        unifier().addActive(gen.getScheme());
        return unit;
    }

    private Unit add(CInstantiate inst) throws UnsatisfiableException {
        defered.add(inst);
        unifier().addActive(inst.getType());
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
        ITerm scheme = generalize(type);
        try {
            unifier().removeActive(gen.getScheme());
            unifier().unify(gen.getScheme(), scheme);
        } catch(UnificationException ex) {
            throw new UnsatisfiableException(gen.getMessageInfo().withDefault(ex.getMessageContent()));
        }
        return true;
    }

    private ITerm generalize(ITerm type) {
        Map<ITermVar, TypeVar> subst = Maps.newHashMap();
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
        ITerm schemeTerm = unifier().find(inst.getScheme());
        if(unifier().isActive(schemeTerm)) {
            return false;
        }
        ITerm type = Forall.matcher().match(schemeTerm).map(scheme -> instantiate(scheme)).orElse(schemeTerm);
        try {
            unifier().removeActive(inst.getType());
            unifier().unify(inst.getType(), type);
        } catch(UnificationException ex) {
            throw new UnsatisfiableException(inst.getMessageInfo().withDefault(ex.getMessageContent()));
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