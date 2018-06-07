package mb.nabl2.solver.components;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Map;
import java.util.Optional;

import org.metaborg.util.functions.PartialFunction2;
import org.metaborg.util.functions.Predicate1;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.Maps;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.base.ImmutableCConj;
import mb.nabl2.constraints.base.ImmutableCExists;
import mb.nabl2.constraints.equality.ImmutableCEqual;
import mb.nabl2.constraints.messages.MessageInfo;
import mb.nabl2.constraints.namebinding.DeclProperties;
import mb.nabl2.constraints.nameresolution.ImmutableCDeclProperty;
import mb.nabl2.constraints.poly.CGeneralize;
import mb.nabl2.constraints.poly.CInstantiate;
import mb.nabl2.constraints.poly.IPolyConstraint;
import mb.nabl2.poly.Forall;
import mb.nabl2.poly.ImmutableForall;
import mb.nabl2.poly.ImmutableTypeVar;
import mb.nabl2.poly.TypeVar;
import mb.nabl2.scopegraph.terms.Occurrence;
import mb.nabl2.solver.ASolver;
import mb.nabl2.solver.ISolver.SolveResult;
import mb.nabl2.solver.SolverCore;
import mb.nabl2.solver.TypeException;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.Transform.T;

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
        final ITerm declTerm = gen.getDeclaration();
        if(!unifier().isGround(declTerm)) {
            return Optional.empty();
        }
        final Occurrence decl = Occurrence.matcher().match(declTerm, unifier())
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + gen));

        final ITerm type = gen.getType();
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
            ImmutableCEqual.of(gen.getGenVars(), B.newList(subst.keySet()), gen.getMessageInfo())
            // @formatter:on
        );
        return Optional.of(result);
    }

    private Optional<SolveResult> solve(CInstantiate inst) {
        final ITerm declTerm = inst.getDeclaration();
        if(!unifier().isGround(declTerm)) {
            return Optional.empty();
        }
        final Occurrence decl = Occurrence.matcher().match(declTerm, unifier())
                .orElseThrow(() -> new TypeException("Expected an occurrence as first argument to " + inst));

        if(!isInstSafe.test(decl)) {
            return Optional.empty();
        }

        final Optional<ITerm> schemeTerm = getDeclProp.apply(decl, DeclProperties.TYPE_KEY);
        if(!schemeTerm.isPresent()) {
            return Optional.empty();
        }

        final Optional<Forall> forall = Forall.matcher().match(schemeTerm.get(), unifier());
        final ITerm type;
        final Map<TypeVar, ITermVar> subst = Maps.newLinkedHashMap(); // linked map to preserve key order
        if(forall.isPresent()) {
            final Forall scheme = forall.get();
            scheme.getTypeVars().stream().forEach(v -> {
                subst.put(v, B.newVar("", fresh(v.getName())));
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
                                ImmutableCEqual.of(inst.getInstVars(), B.newList(subst.keySet()), inst.getMessageInfo()),
                                MessageInfo.empty()
                        ),
                        inst.getMessageInfo());
                // @formatter:on
        SolveResult result = SolveResult.constraints(constraint);
        return Optional.of(result);
    }

    private ITerm subst(ITerm term, Map<? extends ITerm, ? extends ITerm> subst) {
        return T.sometd(
        // @formatter:off
            t -> subst.containsKey(t) ? Optional.of(subst.get(t)) : Optional.empty()
            // @formatter:on
        ).apply(term);
    }

    // ------------------------------------------------------------------------------------------------------//

}
