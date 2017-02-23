package org.metaborg.meta.nabl2.solver;

import static org.metaborg.meta.nabl2.util.Unit.unit;

import java.util.Collection;
import java.util.Collections;
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
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.unification.UnificationException;
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.meta.nabl2.util.Unit;
import org.metaborg.meta.nabl2.util.functions.Function1;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class PolymorphismSolver extends AbstractSolverComponent<IPolyConstraint> {

    private final Unifier unifier;
    private final Function1<String, ITermVar> fresh;
    private final Set<IPolyConstraint> defered;

    private boolean complete = false;

    public PolymorphismSolver(Unifier unifier, Function1<String, ITermVar> fresh) {
        this.unifier = unifier;
        this.fresh = fresh;
        this.defered = Sets.newHashSet();
    }

    @Override public Class<IPolyConstraint> getConstraintClass() {
        return IPolyConstraint.class;
    }

    // ------------------------------------------------------------------------------------------------------//

    @Override public Unit add(IPolyConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(CheckedCases.of(this::add, this::add));
    }

    @Override public boolean iterate() throws UnsatisfiableException, InterruptedException {
        complete = true;
        return doIterate(defered, this::solve);
    }

    @Override public Iterable<IPolyConstraint> finish() {
        return defered;
    }

    // ------------------------------------------------------------------------------------------------------//

    private Unit add(CGeneralize gen) throws UnsatisfiableException {
        defered.add(gen);
        unifier.addActive(gen.getScheme());
        return unit;
    }

    private Unit add(CInstantiate inst) throws UnsatisfiableException {
        defered.add(inst);
        unifier.addActive(inst.getType());
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
        ITerm type = unifier.find(gen.getType());
        if(unifier.isActive(type)) {
            return false;
        }
        ITerm scheme = generalize(type);
        try {
            unifier.removeActive(gen.getScheme());
            unifier.unify(gen.getScheme(), scheme);
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
        ITerm schemeTerm = unifier.find(inst.getScheme());
        if(unifier.isActive(schemeTerm)) {
            return false;
        }
        ITerm type = Forall.matcher().match(schemeTerm).map(scheme -> instantiate(scheme)).orElse(schemeTerm);
        try {
            unifier.removeActive(inst.getType());
            unifier.unify(inst.getType(), type);
        } catch(UnificationException ex) {
            throw new UnsatisfiableException(inst.getMessageInfo().withDefault(ex.getMessageContent()));
        }
        return true;
    }

    private ITerm instantiate(Forall scheme) {
        Map<TypeVar, ITermVar> mapping = Maps.newHashMap();
        scheme.getTypeVars().stream().forEach(v -> {
            mapping.put(v, fresh.apply(v.getName()));
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

    // ------------------------------------------------------------------------------------------------------//

    @Override public Collection<IPolyConstraint> getNormalizedConstraints(IMessageInfo messageInfo) {
        return Collections.emptySet();
    }

}