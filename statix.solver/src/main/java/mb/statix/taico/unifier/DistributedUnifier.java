package mb.statix.taico.unifier;

import java.util.LinkedList;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.statix.taico.module.IModule;
import mb.statix.taico.util.TOverrides;
import mb.statix.taico.util.Vars;

public class DistributedUnifier {

    public static class Immutable extends PersistentUnifier.Immutable {

        private static final long serialVersionUID = 42L;
        
        private final String owner;
        private final boolean unrestricted;

        Immutable(String owner, boolean finite, Map.Immutable<ITermVar, ITermVar> reps,
                Map.Immutable<ITermVar, Integer> ranks,
                Map.Immutable<ITermVar, ITerm> terms) {
            this(owner, finite, reps, ranks, terms, false);
        }
        
        Immutable(String owner, boolean finite, Map.Immutable<ITermVar, ITermVar> reps,
                Map.Immutable<ITermVar, Integer> ranks,
                Map.Immutable<ITermVar, ITerm> terms,
                boolean unrestricted) {
            super(finite, reps, ranks, terms);
            this.owner = owner;
            this.unrestricted = unrestricted;
        }
        
        // ----------------------------------------------------------------------------------------
        // Term
        // ----------------------------------------------------------------------------------------
        
        @Override
        public ITerm findTerm(ITerm term) {
            return term.match(Terms.<ITerm>cases().var(var -> {
                final ITermVar rep;
                final IModule target;
                if (owner.equals(var.getResource()) || (target = getOwner(var)) == null) {
                    rep = findRepFinal(var);
                    return findTermFinal(rep);
                }
                
                DistributedUnifier.Immutable unifier = target.getCurrentState().unifier();
                rep = unifier.findRepFinal(var);
                return unifier.findTermFinal(rep);
            }).otherwise(t -> t));
        }
        
        protected ITerm findTermFinal(ITermVar rep) {
            return terms().getOrDefault(rep, rep);
        }
        
        // ----------------------------------------------------------------------------------------
        // Rep
        // ----------------------------------------------------------------------------------------

        @Override
        public ITermVar findRep(ITermVar var) {
            //TODO Entails?
            final IModule module;
            if (owner.equals(var.getResource()) || (module = getOwner(var)) == null) {
                return findRepFinal(var);
            }

            return module.getCurrentState().unifier().findRepFinal(var);
        }
        
        protected ITermVar findRepFinal(ITermVar var) {
            return super.findRep(var);
        }
        
        // ----------------------------------------------------------------------------------------
        // Ground
        // ----------------------------------------------------------------------------------------
        
        @Override
        protected boolean isGround(final ITermVar var, final Set<ITermVar> stack,
                final java.util.Map<ITermVar, Boolean> visited) {
            final IModule target;
            if (owner.equals(var.getResource()) || (target = getOwner(var)) == null) {
                return isGroundFinal(var, stack, visited);
            } else {
                return target.getCurrentState().unifier().isGroundFinal(var, stack, visited);
            }
        }
        
        private boolean isGroundFinal(final ITermVar var, final Set<ITermVar> stack,
                final java.util.Map<ITermVar, Boolean> visited) {
            return super.isGround(var, stack, visited);
        }
        
        // ----------------------------------------------------------------------------------------
        // Cyclic
        // ----------------------------------------------------------------------------------------
        
        @Override
        protected boolean isCyclic(final ITermVar var, final Set<ITermVar> stack,
                final java.util.Map<ITermVar, Boolean> visited) {
            final IModule target;
            if (owner.equals(var.getResource()) || (target = getOwner(var)) == null) {
                return isCyclicFinal(var, stack, visited);
            } else {
                return target.getCurrentState().unifier().isCyclicFinal(var, stack, visited);
            }
        }
        
        private boolean isCyclicFinal(final ITermVar var, final Set<ITermVar> stack,
                final java.util.Map<ITermVar, Boolean> visited) {
            return super.isCyclic(var, stack, visited);
        }

        // ----------------------------------------------------------------------------------------
        // Own Variables
        // ----------------------------------------------------------------------------------------
        
        /**
         * Collects all the variables in the given term that are owned by the module owning this
         * unifier. Variables from other modules will not be expanded.
         * The result only contains variables that are not ground.
         * <p>
         * Keep in mind that expanding variables from other modules could yield additional
         * variables that belong to this unifier. These variables are not reported by this method.
         * <p>
         * The variables in this set should be non-ground, otherwise it will have been instantiated
         * or it is cyclic (which might be considered non-ground).
         * 
         * @param term
         *      the term
         * 
         * @return
         *      all the variables in the given term that belong to this unifier
         */
        public Set<ITermVar> getOwnVariables(ITerm term) {
            final Set<ITermVar> vars = Sets.newHashSet();
            getOwnVars(term.getVars().elementSet(), Lists.newLinkedList(), Sets.newHashSet(), vars);
            return vars;
        }

        private void getOwnVars(final Set<ITermVar> tryVars, final LinkedList<ITermVar> stack, final Set<ITermVar> visited,
                Set<ITermVar> vars) {
            tryVars.stream().forEach(var -> getOwnVars(var, stack, visited, vars));
        }

        private void getOwnVars(final ITermVar var, final LinkedList<ITermVar> stack, final Set<ITermVar> visited,
                Set<ITermVar> vars) {
            final ITermVar rep = findRepFinal(var);
            if (!owner.equals(rep.getResource())) return; //Not our own variable
            
            if(!visited.contains(rep)) {
                visited.add(rep);
                stack.push(rep);
                final ITerm term = terms().get(rep);
                if(term != null) {
                    getOwnVars(term.getVars().elementSet(), stack, visited, vars);
                } else {
                    vars.add(rep);
                }
                stack.pop();
            } else {
                final int index = stack.indexOf(rep); // linear
                if(index >= 0) {
                    vars.addAll(stack.subList(0, index + 1));
                }
            }
        }
        
        // ----------------------------------------------------------------------------------------
        // Helper methods
        // ----------------------------------------------------------------------------------------
        
        @Override
        protected Map<ITermVar, ITerm> targetTerms(ITermVar var) {
            final IModule module;
            if (owner.equals(var.getResource()) || (module = getOwner(var)) == null) {
                return this.terms();
            }
            
            return module.getCurrentState().unifier().terms();
        }
        
        private IModule getOwner(ITermVar var) {
            return unrestricted ? Vars.getOwnerUnchecked(var) : Vars.getOwner(var, owner);
        }
        
        // ----------------------------------------------------------------------------------------
        // Other
        // ----------------------------------------------------------------------------------------
        
        @Override public DistributedUnifier.Transient melt() {
            return new DistributedUnifier.Transient(owner, finite, reps.get().asTransient(), ranks.asTransient(),
                    terms.asTransient(), unrestricted);
        }
        
        /**
         * @return
         *      an unrestricted version of this unifier
         */
        @Override
        public DistributedUnifier.Immutable unrestricted() {
            if (unrestricted) return this;
            return new DistributedUnifier.Immutable(owner, finite, reps.get(), ranks, terms, true);
        }
        
        @Override
        public DistributedUnifier.Immutable restricted() {
            if (!unrestricted) return this;
            return new DistributedUnifier.Immutable(owner, finite, reps.get(), ranks, terms, false);
        }
        
        @Override
        public boolean isUnrestricted() {
            return unrestricted;
        }
        
        public static DistributedUnifier.Immutable of(String owner) {
            return of(owner, true);
        }

        public static DistributedUnifier.Immutable of(String owner, boolean finite) {
            return new DistributedUnifier.Immutable(owner, finite, Map.Immutable.of(), Map.Immutable.of(), Map.Immutable.of());
        }
        
        @Override
        protected boolean allowCrossModuleUnification() {
            //TODO Base this on more info, e.g. only cross module unification for split modules
            return TOverrides.CROSS_MODULE_UNIFICATION;
        }
    }

    public static class Transient extends PersistentUnifier.Transient {

        private static final long serialVersionUID = 42L;
        
        private final String owner;
        private final boolean unrestricted;

        Transient(String owner, boolean finite, Map.Transient<ITermVar, ITermVar> reps,
                Map.Transient<ITermVar, Integer> ranks,
                Map.Transient<ITermVar, ITerm> terms) {
            this(owner, finite, reps, ranks, terms, false);
        }
        
        Transient(String owner, boolean finite, Map.Transient<ITermVar, ITermVar> reps,
                Map.Transient<ITermVar, Integer> ranks,
                Map.Transient<ITermVar, ITerm> terms,
                boolean unrestricted) {
            super(finite, reps, ranks, terms);
            this.owner = owner;
            this.unrestricted = unrestricted;
        }
        
        // ----------------------------------------------------------------------------------------
        // Term
        // ----------------------------------------------------------------------------------------
        
        @Override
        public ITerm findTerm(ITerm term) {
            return term.match(Terms.<ITerm>cases().var(var -> {
                final ITermVar rep;
                final IModule target;
                if (owner.equals(var.getResource()) || (target = getOwner(var)) == null) {
                    rep = findRepFinal(var);
                    return findTermFinal(rep);
                }
                
                DistributedUnifier.Immutable unifier = target.getCurrentState().unifier();
                rep = unifier.findRepFinal(var);
                return unifier.findTermFinal(rep);
            }).otherwise(t -> t));
        }
        
        protected ITerm findTermFinal(ITermVar rep) {
            return terms().getOrDefault(rep, rep);
        }
        
        // ----------------------------------------------------------------------------------------
        // Rep
        // ----------------------------------------------------------------------------------------

        @Override
        public ITermVar findRep(ITermVar var) {
            //TODO Entails?
            final IModule module;
            if (owner.equals(var.getResource()) || (module = getOwner(var)) == null) {
                return findRepFinal(var);
            }

            return module.getCurrentState().unifier().findRepFinal(var);
        }
        
        protected ITermVar findRepFinal(ITermVar var) {
            return super.findRep(var);
        }
        
        // ----------------------------------------------------------------------------------------
        // Ground
        // ----------------------------------------------------------------------------------------
        
        @Override
        protected boolean isGround(final ITermVar var, final Set<ITermVar> stack,
                final java.util.Map<ITermVar, Boolean> visited) {
            final IModule target;
            if (owner.equals(var.getResource()) || (target = getOwner(var)) == null) {
                return isGroundFinal(var, stack, visited);
            } else {
                return target.getCurrentState().unifier().isGroundFinal(var, stack, visited);
            }
        }
        
        private boolean isGroundFinal(final ITermVar var, final Set<ITermVar> stack,
                final java.util.Map<ITermVar, Boolean> visited) {
            return super.isGround(var, stack, visited);
        }
        
        // ----------------------------------------------------------------------------------------
        // Cyclic
        // ----------------------------------------------------------------------------------------
        
        @Override
        protected boolean isCyclic(final ITermVar var, final Set<ITermVar> stack,
                final java.util.Map<ITermVar, Boolean> visited) {
            final IModule target;
            if (owner.equals(var.getResource()) || (target = getOwner(var)) == null) {
                return isCyclicFinal(var, stack, visited);
            } else {
                return target.getCurrentState().unifier().isCyclicFinal(var, stack, visited);
            }
        }
        
        private boolean isCyclicFinal(final ITermVar var, final Set<ITermVar> stack,
                final java.util.Map<ITermVar, Boolean> visited) {
            return super.isCyclic(var, stack, visited);
        }
        
        // ----------------------------------------------------------------------------------------
        // Get own vars
        // ----------------------------------------------------------------------------------------
        
        /**
         * Collects all the variables in the given term that are owned by the module owning this
         * unifier. Variables from other modules will not be expanded.
         * The result only contains variables that are not ground.
         * <p>
         * Keep in mind that expanding variables from other modules could yield additional
         * variables that belong to this unifier. These variables are not reported by this method.
         * <p>
         * The variables in this set should be non-ground, otherwise it will have been instantiated
         * or it is cyclic (which might be considered non-ground).
         * 
         * @param term
         *      the term
         * 
         * @return
         *      all the variables in the given term that belong to this unifier
         */
        public Set<ITermVar> getOwnVariables(ITerm term) {
            final Set<ITermVar> vars = Sets.newHashSet();
            getOwnVars(term.getVars().elementSet(), Lists.newLinkedList(), Sets.newHashSet(), vars);
            return vars;
        }

        private void getOwnVars(final Set<ITermVar> tryVars, final LinkedList<ITermVar> stack, final Set<ITermVar> visited,
                Set<ITermVar> vars) {
            tryVars.stream().forEach(var -> getOwnVars(var, stack, visited, vars));
        }

        private void getOwnVars(final ITermVar var, final LinkedList<ITermVar> stack, final Set<ITermVar> visited,
                Set<ITermVar> vars) {
            final ITermVar rep = findRepFinal(var);
            if (!owner.equals(rep.getResource())) return; //Not our own variable
            
            if(!visited.contains(rep)) {
                visited.add(rep);
                stack.push(rep);
                final ITerm term = terms().get(rep);
                if(term != null) {
                    getOwnVars(term.getVars().elementSet(), stack, visited, vars);
                } else {
                    vars.add(rep);
                }
                stack.pop();
            } else {
                final int index = stack.indexOf(rep); // linear
                if(index >= 0) {
                    vars.addAll(stack.subList(0, index + 1));
                }
            }
        }
        
        // ----------------------------------------------------------------------------------------
        // Helper methods
        // ----------------------------------------------------------------------------------------
        
        @Override
        protected Map<ITermVar, ITerm> targetTerms(ITermVar var) {
            final IModule module;
            if (owner.equals(var.getResource()) || (module = getOwner(var)) == null) {
                return this.terms();
            }
            
            return module.getCurrentState().unifier().targetTerms(var);
        }
        
        private IModule getOwner(ITermVar var) {
            return unrestricted ? Vars.getOwnerUnchecked(var) : Vars.getOwner(var, owner);
        }
        
        // ----------------------------------------------------------------------------------------
        // Other
        // ----------------------------------------------------------------------------------------
        
        @Override public DistributedUnifier.Immutable freeze() {
            return new DistributedUnifier.Immutable(owner, finite, reps.freeze(), ranks.freeze(), terms.freeze(), unrestricted);
        }

        /**
         * @return
         *      an unrestricted version of this unifier
         */
        @Override
        public DistributedUnifier.Transient unrestricted() {
            if (unrestricted) return this;
            return new DistributedUnifier.Transient(owner, finite, reps, ranks, terms, true);
        }
        
        /**
         * @return
         *      a restricted version of this unifier
         */
        @Override
        public DistributedUnifier.Transient restricted() {
            if (!unrestricted) return this;
            return new DistributedUnifier.Transient(owner, finite, reps, ranks, terms, false);
        }
        
        @Override
        public boolean isUnrestricted() {
            return unrestricted;
        }
        
        @Override
        protected boolean allowCrossModuleUnification() {
            //TODO Base this on more info, e.g. only cross module unification for split modules
            return TOverrides.CROSS_MODULE_UNIFICATION;
        }
        
        public static DistributedUnifier.Transient of(String owner) {
            return of(owner, true);
        }

        public static DistributedUnifier.Transient of(String owner, boolean finite) {
            return new DistributedUnifier.Transient(owner, finite, Map.Transient.of(), Map.Transient.of(), Map.Transient.of());
        }
    }
}
