package org.metaborg.meta.nabl2.solver;

import static org.metaborg.meta.nabl2.util.Unit.unit;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.constraints.poly.CGeneralize;
import org.metaborg.meta.nabl2.constraints.poly.CInstantiate;
import org.metaborg.meta.nabl2.constraints.poly.IPolyConstraint;
import org.metaborg.meta.nabl2.constraints.poly.IPolyConstraint.CheckedCases;
import org.metaborg.meta.nabl2.poly.Forall;
import org.metaborg.meta.nabl2.poly.ImmutableForall;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.GenericTerms;
import org.metaborg.meta.nabl2.unification.UnificationException;
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.meta.nabl2.util.Unit;
import org.metaborg.meta.nabl2.util.functions.Function2;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class PolymorphismSolver implements ISolverComponent<IPolyConstraint> {

    private final Unifier unifier;
    private final Function2<String, String, String> fresh;
    private final Set<IPolyConstraint> defered;

    public PolymorphismSolver(Unifier unifier, Function2<String, String, String> fresh) {
        this.unifier = unifier;
        this.fresh = fresh;
        this.defered = Sets.newHashSet();
    }

    // ------------------------------------------------------------------------------------------------------//

    @Override public Unit add(IPolyConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(CheckedCases.of(this::add, this::add));
    }

    @Override public boolean iterate() throws UnsatisfiableException {
        Iterator<IPolyConstraint> it = defered.iterator();
        boolean progress = false;
        while(it.hasNext()) {
            try {
                if(solve(it.next())) {
                    progress = true;
                    it.remove();
                }
            } catch(UnsatisfiableException e) {
                progress = true;
                it.remove();
                throw e;
            }
        }
        return progress;
    }

    @Override public Iterable<IMessageInfo> finish() {
        return defered.stream().map(
            c -> c.getMessageInfo().withDefault(MessageContent.builder().append("Unsolved: ").append(c.pp()).build()))
            .collect(Collectors.toList());
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
        ITerm type = unifier.find(gen.getType());
        if(unifier.isActive(type)) {
            return false;
        }
        ITerm scheme = type.getVars().isEmpty() ? type : ImmutableForall.of(type.getVars(), type);
        try {
            unifier.removeActive(gen.getScheme());
            unifier.unify(gen.getScheme(), scheme);
        } catch(UnificationException ex) {
            throw new UnsatisfiableException(gen.getMessageInfo().withDefault(ex.getMessageContent()));
        }
        return true;
    }

    private boolean solve(CInstantiate inst) throws UnsatisfiableException {
        ITerm schemeTerm = unifier.find(inst.getScheme());
        if(M.var(v -> {
            return unifier.isActive(v);
        }).match(schemeTerm).orElse(false)) {
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
        Map<ITermVar, ITermVar> mapping = Maps.newHashMap();
        scheme.getTypeVars().stream().forEach(v -> {
            mapping.put(v, GenericTerms.newVar(v.getResource(), fresh.apply(v.getResource(), v.getName())));
        });
        ITerm type = subst(scheme.getType(), mapping);
        return type;
    }

    private ITerm subst(ITerm term, Map<ITermVar, ITermVar> mapping) {
        return M.sometd(
            // @formatter:off
            M.var(var -> mapping.getOrDefault(var, var).setAttachments(var.getAttachments()))
            // @formatter:on
        ).apply(term);
    }

}