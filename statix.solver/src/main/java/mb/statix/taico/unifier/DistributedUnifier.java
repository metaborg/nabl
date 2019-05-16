package mb.statix.taico.unifier;

import java.util.Set;

import io.usethesource.capsule.Map;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.statix.taico.module.IModule;
import mb.statix.taico.util.Vars;

public class DistributedUnifier {

    public static class Immutable extends PersistentUnifier.Immutable {

        private static final long serialVersionUID = 42L;
        
        private final String owner;

        Immutable(String owner, boolean finite, Map.Immutable<ITermVar, ITermVar> reps,
                Map.Immutable<ITermVar, Integer> ranks,
                Map.Immutable<ITermVar, ITerm> terms) {
            super(finite, reps, ranks, terms);
            this.owner = owner;
        }
        
        @Override
        public ITerm findTerm(ITerm term) {
            return term.match(Terms.<ITerm>cases().var(var -> {
                final ITermVar rep;
                final IModule target;
                if (owner.equals(var.getResource()) || (target = Vars.getOwnerUnchecked(var)) == null) {
                    rep = findRepFinal(var);
                    return findTermFinal(rep);
                }
                
                rep = target.getCurrentState().unifier().findRepFinal(var);
                return target.getCurrentState().unifier().findTermFinal(rep);
            }).otherwise(t -> t));
        }
        
        protected ITerm findTermFinal(ITermVar rep) {
            return terms().getOrDefault(rep, rep);
        }

        @Override
        public ITermVar findRep(ITermVar var) {
            //TODO Entails?
            final IModule module;
            if (owner.equals(var.getResource()) || (module = Vars.getOwnerUnchecked(var)) == null) {
                return findRepFinal(var);
            }

            return module.getCurrentState().unifier().findRepFinal(var);
        }
        
        protected ITermVar findRepFinal(ITermVar var) {
            return super.findRep(var);
        }
        
        @Override
        protected boolean isGround(final ITermVar var, final Set<ITermVar> stack,
                final java.util.Map<ITermVar, Boolean> visited) {
            final IModule target;
            if (owner.equals(var.getResource()) || (target = Vars.getOwnerUnchecked(var)) == null) {
                return isGroundFinal(var, stack, visited);
            } else {
                return target.getCurrentState().unifier().isGroundFinal(var, stack, visited);
            }
        }
        
        private boolean isGroundFinal(final ITermVar var, final Set<ITermVar> stack,
                final java.util.Map<ITermVar, Boolean> visited) {
            return super.isGround(var, stack, visited);
        }
        
        @Override public DistributedUnifier.Transient melt() {
            return new DistributedUnifier.Transient(owner, finite, reps.get().asTransient(), ranks.asTransient(),
                    terms.asTransient());
        }
        
        public static DistributedUnifier.Immutable of(String owner) {
            return of(owner, true);
        }

        public static DistributedUnifier.Immutable of(String owner, boolean finite) {
            return new DistributedUnifier.Immutable(owner, finite, Map.Immutable.of(), Map.Immutable.of(), Map.Immutable.of());
        }
    }

    public static class Transient extends PersistentUnifier.Transient {

        private static final long serialVersionUID = 42L;
        
        private final String owner;

        Transient(String owner, boolean finite, Map.Transient<ITermVar, ITermVar> reps,
                Map.Transient<ITermVar, Integer> ranks,
                Map.Transient<ITermVar, ITerm> terms) {
            super(finite, reps, ranks, terms);
            this.owner = owner;
        }
        
        @Override
        public ITerm findTerm(ITerm term) {
            return term.match(Terms.<ITerm>cases().var(var -> {
                final ITermVar rep;
                final IModule target;
                if (owner.equals(var.getResource()) || (target = Vars.getOwnerUnchecked(var)) == null) {
                    rep = findRepFinal(var);
                    return findTermFinal(rep);
                }
                
                rep = target.getCurrentState().unifier().findRepFinal(var);
                return target.getCurrentState().unifier().findTermFinal(rep);
            }).otherwise(t -> t));
        }
        
        protected ITerm findTermFinal(ITermVar rep) {
            return terms().getOrDefault(rep, rep);
        }

        @Override
        public ITermVar findRep(ITermVar var) {
            final IModule module;
            if (owner.equals(var.getResource()) || (module = Vars.getOwnerUnchecked(var)) == null) {
                return findRepFinal(var);
            }

            return module.getCurrentState().unifier().findRepFinal(var);
        }
        
        protected ITermVar findRepFinal(ITermVar var) {
            return super.findRep(var);
        }
        
        @Override
        protected boolean isGround(final ITermVar var, final Set<ITermVar> stack,
                final java.util.Map<ITermVar, Boolean> visited) {
            final IModule target;
            if (owner.equals(var.getResource()) || (target = Vars.getOwnerUnchecked(var)) == null) {
                return isGroundFinal(var, stack, visited);
            } else {
                return target.getCurrentState().unifier().isGroundFinal(var, stack, visited);
            }
        }
        
        private boolean isGroundFinal(final ITermVar var, final Set<ITermVar> stack,
                final java.util.Map<ITermVar, Boolean> visited) {
            return super.isGround(var, stack, visited);
        }
        
        @Override public DistributedUnifier.Immutable freeze() {
            return new DistributedUnifier.Immutable(owner, finite, reps.freeze(), ranks.freeze(), terms.freeze());
        }

        public static DistributedUnifier.Transient of(String owner) {
            return of(owner, true);
        }

        public static DistributedUnifier.Transient of(String owner, boolean finite) {
            return new DistributedUnifier.Transient(owner, finite, Map.Transient.of(), Map.Transient.of(), Map.Transient.of());
        }
    }
}
