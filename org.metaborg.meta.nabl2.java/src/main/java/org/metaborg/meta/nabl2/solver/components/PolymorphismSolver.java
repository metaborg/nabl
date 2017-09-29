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
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.solver.Solver;
import org.metaborg.meta.nabl2.solver.SolverComponent;
import org.metaborg.meta.nabl2.solver.TypeException;
import org.metaborg.meta.nabl2.solver.UnsatisfiableException;
import org.metaborg.meta.nabl2.spoofax.analysis.AnalysisTerms;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.meta.nabl2.unification.UnificationException;
import org.metaborg.meta.nabl2.util.Unit;
import org.metaborg.meta.nabl2.util.functions.PartialFunction2;
import org.metaborg.meta.nabl2.util.functions.PartialFunction3;
import org.metaborg.meta.nabl2.util.functions.Predicate0;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

public class PolymorphismSolver extends SolverComponent<IPolyConstraint> {

    private final Predicate0 arePropsDone;
    private final PartialFunction2<Occurrence, ITerm, ITerm> getDeclProp;
    private final PartialFunction3<Occurrence, ITerm, ITerm, ITerm> setDeclProp;

    private final Set<CInstantiate> defered;
    private final Set<IPolyConstraint> determined;

    private final Multiset<IOccurrence> activeDecls;

    public PolymorphismSolver(Solver solver, Predicate0 arePropsDone,
            PartialFunction2<Occurrence, ITerm, ITerm> getDeclProp,
            PartialFunction3<Occurrence, ITerm, ITerm, ITerm> setDeclProp) {
        super(solver);
        this.arePropsDone = arePropsDone;
        this.getDeclProp = getDeclProp;
        this.setDeclProp = setDeclProp;
        this.defered = Sets.newHashSet();
        this.determined = Sets.newHashSet();
        this.activeDecls = HashMultiset.create();
    }

    // ------------------------------------------------------------------------------------------------------//

    @Override protected Unit doAdd(IPolyConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(CheckedCases.of(this::add, this::add));
    }

    @Override protected boolean doIterate() throws UnsatisfiableException, InterruptedException {
        boolean progress = false;
        progress |= doIterate(defered, this::determine);
        progress |= doIterate(determined, this::solve);
        return progress;
    }

    @Override protected Set<? extends IPolyConstraint> doFinish(IMessageInfo messageInfo) {
        return defered;
    }

    // ------------------------------------------------------------------------------------------------------//

    private Unit add(CGeneralize gen) throws UnsatisfiableException {
        unifier().addActive(gen.getGenVars(), gen);
        determined.add(gen);
        return unit;
    }

    private Unit add(CInstantiate inst) throws UnsatisfiableException {
        unifier().addActive(inst.getType(), inst);
        unifier().addActive(inst.getInstVars(), inst);
        if(!determine(inst)) {
            defered.add(inst);
        }
        return unit;
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean determine(CInstantiate inst) throws UnsatisfiableException {
        final ITerm declTerm = unifier().find(inst.getDeclaration());
        if(!declTerm.isGround()) {
            return false;
        }
        final Occurrence decl = Occurrence.matcher().match(declTerm)
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + inst));
        activeDecls.add(decl);
        unifier().addDetermination(inst.getType(), AnalysisTerms.TYPE_KEY, decl);
        determined.add(inst);
        return true;
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean solve(IPolyConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(CheckedCases.of(this::solve, this::solve));
    }

    private boolean solve(CGeneralize gen) throws UnsatisfiableException {
        if(!canSolve()) {
            return false;
        }

        final ITerm declTerm = unifier().find(gen.getDeclaration());
        if(!declTerm.isGround()) {
            return false;
        }
        final Occurrence decl = Occurrence.matcher().match(declTerm)
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + gen));

        final ITerm type = unifier().find(gen.getType());
        if(!isGenSafe(type)) {
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
            unifier().removeActive(gen.getGenVars(), gen);
            unifier().unify(gen.getGenVars(), TB.newList(subst.keySet()));
        } catch(UnificationException ex) {
            throw new UnsatisfiableException(gen.getMessageInfo().withDefaultContent(ex.getMessageContent()));
        }

        final Optional<ITerm> prev = setDeclProp.apply(decl, AnalysisTerms.TYPE_KEY, scheme);
        if(prev.isPresent()) {
            throw new UnsatisfiableException();
        }

        return true;
    }

    private boolean solve(CInstantiate inst) throws UnsatisfiableException {
        if(!canSolve()) {
            return false;
        }

        final ITerm declTerm = unifier().find(inst.getDeclaration());
        if(!declTerm.isGround()) {
            return false;
        }
        final Occurrence decl = Occurrence.matcher().match(declTerm)
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + inst));

        final Optional<ITerm> schemeTerm = getDeclProp.apply(decl, AnalysisTerms.TYPE_KEY).map(unifier()::find);
        if(!schemeTerm.isPresent()) {
            return false;
        }

        final Optional<Forall> forall = Forall.matcher().match(schemeTerm.get());
        final ITerm type;
        final Map<TypeVar, ITermVar> subst = Maps.newLinkedHashMap();
        if(forall.isPresent()) {
            final Forall scheme = forall.get();
            scheme.getTypeVars().stream().forEach(v -> {
                subst.put(v, fresh().apply(v.getName()));
            });
            type = subst(scheme.getType(), subst);
        } else {
            type = schemeTerm.get();
        }

        activeDecls.remove(decl);
        try {
            unifier().removeActive(inst.getType(), inst); // before `unify`, so that we don't cause an error chain if
                                                          // that fails
            unifier().removeActive(inst.getInstVars(), inst);
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

    // ------------------------------------------------------------------------------------------------------//

    private boolean canSolve() {
        return arePropsDone.test() && defered.isEmpty();
    }

    private boolean isGenSafe(ITerm type) {
        if(unifier().isActive(type)) {
            return false;
        }
        for(IOccurrence decl : unifier().isDeterminedBy(type, AnalysisTerms.TYPE_KEY)) {
            if(activeDecls.contains(decl)) {
                return false;
            }
        }
        return true;
    }

}