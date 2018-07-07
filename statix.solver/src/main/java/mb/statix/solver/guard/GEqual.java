package mb.statix.solver.guard;

import java.util.Optional;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.CannotUnifyException;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.nabl2.terms.unification.RigidVarsException;
import mb.statix.solver.Delay;
import mb.statix.solver.GuardContext;
import mb.statix.solver.IGuard;
import mb.statix.solver.State;

public class GEqual implements IGuard {

    private final ITerm term1;
    private final ITerm term2;

    public GEqual(ITerm term1, ITerm term2) {
        this.term1 = term1;
        this.term2 = term2;
    }

    @Override public IGuard apply(ISubstitution.Immutable subst) {
        return new GEqual(subst.apply(term1), subst.apply(term2));
    }

    @Override public Optional<State> solve(State state, GuardContext params) throws Delay {
        IUnifier.Immutable unifier = state.unifier();
        try {
            final IUnifier.Immutable.Result<IUnifier.Immutable> result = unifier.unify(term1, term2, params::isRigid);
            params.debug().info("Unification succeeded: {}", result.result());
            return Optional.of(state.withUnifier(result.unifier()));
        } catch(CannotUnifyException e) {
            params.debug().info("Unification failed: {} != {}", unifier.toString(e.getLeft()),
                    unifier.toString(e.getRight()));
            return Optional.empty();
        } catch(OccursException e) {
            params.debug().info("Unification failed: {} != {}", unifier.toString(term1), unifier.toString(term2));
            return Optional.empty();
        } catch(RigidVarsException e) {
            throw Delay.ofVars(e.vars());
        }
    }

    @Override public String toString(IUnifier unifier) {
        final StringBuilder sb = new StringBuilder();
        sb.append(unifier.toString(term1));
        sb.append(" == ");
        sb.append(unifier.toString(term2));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(PersistentUnifier.Immutable.of());
    }

}