package mb.statix.taico.unifier;

import java.util.LinkedList;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.matching.MaybeNotInstantiatedBool;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.nabl2.util.Set2;
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
        
        // ----------------------------------------------------------------------------------------
        // Term
        // ----------------------------------------------------------------------------------------
        
        @Override
        public ITerm findTerm(ITerm term) {
            return term.match(Terms.<ITerm>cases().var(var -> {
                final ITermVar rep;
                final IModule target;
                if (owner.equals(var.getResource()) || (target = Vars.getOwner(var, owner)) == null) {
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
        // Recursive
        // ----------------------------------------------------------------------------------------
        
        @Override
        protected ITerm findVarRecursive(final ITermVar var, final Set<ITermVar> stack,
                final java.util.Map<ITermVar, ITerm> visited) {
            final ITermVar rep = findRep(var);
            final ITerm instance;
            if(!visited.containsKey(rep)) {
                stack.add(rep);
                visited.put(rep, null);
                //Modification: get the term from the target
                final ITerm term = targetTerms(rep).get(rep);
                instance = term != null ? findTermRecursive(term, stack, visited) : rep;
                visited.put(rep, instance);
                stack.remove(rep);
                return instance;
            } else if(stack.contains(rep)) {
                throw new IllegalArgumentException("Recursive terms cannot be fully instantiated.");
            } else {
                instance = visited.get(rep);
            }
            return instance;
        }
        
        // ----------------------------------------------------------------------------------------
        // Rep
        // ----------------------------------------------------------------------------------------

        @Override
        public ITermVar findRep(ITermVar var) {
            //TODO Entails?
            final IModule module;
            if (owner.equals(var.getResource()) || (module = Vars.getOwner(var, owner)) == null) {
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
            if (owner.equals(var.getResource()) || (target = Vars.getOwner(var, owner)) == null) {
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
            if (owner.equals(var.getResource()) || (target = Vars.getOwner(var, owner)) == null) {
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
        // Get Vars
        // ----------------------------------------------------------------------------------------
        
        @Override
        protected void getVars(final ITermVar var, final LinkedList<ITermVar> stack, final Set<ITermVar> visited,
                Set<ITermVar> vars) {
            final ITermVar rep = findRep(var);
            if(!visited.contains(rep)) {
                visited.add(rep);
                stack.push(rep);
                final ITerm term = targetTerms(rep).get(rep);
                if(term != null) {
                    getVars(term.getVars().elementSet(), stack, visited, vars);
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
        // Var equality
        // ----------------------------------------------------------------------------------------
        
        @Override
        protected MaybeNotInstantiatedBool equalVarTerm(ITermVar var, ITerm term, Set<Set2<ITermVar>> stack,
                java.util.Map<Set2<ITermVar>, Boolean> visited) {
            final ITermVar rep = findRep(var);
            //Modified: Get the term from the target unifier
            Map<ITermVar, ITerm> terms = targetTerms(rep);
            if(terms.containsKey(rep)) {
                return equalTerms(terms.get(rep), term, stack, visited);
            } else {
                return MaybeNotInstantiatedBool.ofNotInstantiated(rep);
            }
        }
        
        @Override
        protected MaybeNotInstantiatedBool equalVars(final ITermVar left, final ITermVar right,
                final Set<Set2<ITermVar>> stack, final java.util.Map<Set2<ITermVar>, Boolean> visited) {
            final ITermVar leftRep = findRep(left);
            final ITermVar rightRep = findRep(right);
            if(leftRep.equals(rightRep)) {
                return MaybeNotInstantiatedBool.ofResult(true);
            }
            final Set2<ITermVar> pair = Set2.of(leftRep, rightRep);
            final MaybeNotInstantiatedBool equal;
            if(!visited.containsKey(pair)) {
                stack.add(pair);
                visited.put(pair, null);
                //Modified: Get the term from the target unifier
                final ITerm leftTerm = targetTerms(leftRep).get(leftRep);
                final ITerm rightTerm = targetTerms(rightRep).get(rightRep);
                if(leftTerm == null && rightTerm == null) {
                    return MaybeNotInstantiatedBool.ofNotInstantiated(leftRep, rightRep);
                } else if(leftTerm == null) {
                    return MaybeNotInstantiatedBool.ofNotInstantiated(leftRep);
                } else if(rightTerm == null) {
                    return MaybeNotInstantiatedBool.ofNotInstantiated(rightRep);
                }
                equal = equalTerms(leftTerm, rightTerm, stack, visited);
                equal.onResult(eq -> {
                    visited.put(pair, eq);
                });
                stack.remove(pair);
            } else if(stack.contains(pair)) {
                equal = MaybeNotInstantiatedBool.ofResult(false);
            } else {
                equal = MaybeNotInstantiatedBool.ofResult(visited.get(pair));
            }
            return equal;
        }
        
        // ----------------------------------------------------------------------------------------
        // Helper methods
        // ----------------------------------------------------------------------------------------
        
        private Map<ITermVar, ITerm> targetTerms(ITermVar var) {
            final IModule module;
            if (owner.equals(var.getResource()) || (module = Vars.getOwner(var, owner)) == null) {
                return this.terms();
            }
            
            return module.getCurrentState().unifier().terms();
        }
        
        // ----------------------------------------------------------------------------------------
        // Other
        // ----------------------------------------------------------------------------------------
        
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
        
        // ----------------------------------------------------------------------------------------
        // Term
        // ----------------------------------------------------------------------------------------
        
        @Override
        public ITerm findTerm(ITerm term) {
            return term.match(Terms.<ITerm>cases().var(var -> {
                final ITermVar rep;
                final IModule target;
                if (owner.equals(var.getResource()) || (target = Vars.getOwner(var, owner)) == null) {
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
        protected ITerm findVarRecursive(final ITermVar var, final Set<ITermVar> stack,
                final java.util.Map<ITermVar, ITerm> visited) {
            final ITermVar rep = findRep(var);
            final ITerm instance;
            if(!visited.containsKey(rep)) {
                stack.add(rep);
                visited.put(rep, null);
                //Modification: get the term from the target
                final ITerm term = targetTerms(rep).get(rep);
                instance = term != null ? findTermRecursive(term, stack, visited) : rep;
                visited.put(rep, instance);
                stack.remove(rep);
                return instance;
            } else if(stack.contains(rep)) {
                throw new IllegalArgumentException("Recursive terms cannot be fully instantiated.");
            } else {
                instance = visited.get(rep);
            }
            return instance;
        }
        
        // ----------------------------------------------------------------------------------------
        // Rep
        // ----------------------------------------------------------------------------------------

        @Override
        public ITermVar findRep(ITermVar var) {
            final IModule module;
            if (owner.equals(var.getResource()) || (module = Vars.getOwner(var, owner)) == null) {
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
            if (owner.equals(var.getResource()) || (target = Vars.getOwner(var, owner)) == null) {
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
            if (owner.equals(var.getResource()) || (target = Vars.getOwner(var, owner)) == null) {
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
        // Get Vars
        // ----------------------------------------------------------------------------------------
        
        @Override
        protected void getVars(final ITermVar var, final LinkedList<ITermVar> stack, final Set<ITermVar> visited,
                Set<ITermVar> vars) {
            final ITermVar rep = findRep(var);
            if(!visited.contains(rep)) {
                visited.add(rep);
                stack.push(rep);
                final ITerm term = targetTerms(rep).get(rep);
                if(term != null) {
                    getVars(term.getVars().elementSet(), stack, visited, vars);
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
        // Var Equality
        // ----------------------------------------------------------------------------------------
        
        @Override
        protected MaybeNotInstantiatedBool equalVarTerm(ITermVar var, ITerm term, Set<Set2<ITermVar>> stack,
                java.util.Map<Set2<ITermVar>, Boolean> visited) {
            final ITermVar rep = findRep(var);
            //Modified: Get the term from the target unifier
            Map<ITermVar, ITerm> terms = targetTerms(rep);
            if(terms.containsKey(rep)) {
                return equalTerms(terms.get(rep), term, stack, visited);
            } else {
                return MaybeNotInstantiatedBool.ofNotInstantiated(rep);
            }
        }
        
        @Override
        protected MaybeNotInstantiatedBool equalVars(final ITermVar left, final ITermVar right,
                final Set<Set2<ITermVar>> stack, final java.util.Map<Set2<ITermVar>, Boolean> visited) {
            final ITermVar leftRep = findRep(left);
            final ITermVar rightRep = findRep(right);
            if(leftRep.equals(rightRep)) {
                return MaybeNotInstantiatedBool.ofResult(true);
            }
            final Set2<ITermVar> pair = Set2.of(leftRep, rightRep);
            final MaybeNotInstantiatedBool equal;
            if(!visited.containsKey(pair)) {
                stack.add(pair);
                visited.put(pair, null);
                //Modified: Get the term from the target unifier
                final ITerm leftTerm = targetTerms(leftRep).get(leftRep);
                final ITerm rightTerm = targetTerms(rightRep).get(rightRep);
                if(leftTerm == null && rightTerm == null) {
                    return MaybeNotInstantiatedBool.ofNotInstantiated(leftRep, rightRep);
                } else if(leftTerm == null) {
                    return MaybeNotInstantiatedBool.ofNotInstantiated(leftRep);
                } else if(rightTerm == null) {
                    return MaybeNotInstantiatedBool.ofNotInstantiated(rightRep);
                }
                equal = equalTerms(leftTerm, rightTerm, stack, visited);
                equal.onResult(eq -> {
                    visited.put(pair, eq);
                });
                stack.remove(pair);
            } else if(stack.contains(pair)) {
                equal = MaybeNotInstantiatedBool.ofResult(false);
            } else {
                equal = MaybeNotInstantiatedBool.ofResult(visited.get(pair));
            }
            return equal;
        }
        
        // ----------------------------------------------------------------------------------------
        // Helper methods
        // ----------------------------------------------------------------------------------------
        
        private Map<ITermVar, ITerm> targetTerms(ITermVar var) {
            final IModule module;
            if (owner.equals(var.getResource()) || (module = Vars.getOwner(var, owner)) == null) {
                return this.terms();
            }
            
            return module.getCurrentState().unifier().targetTerms(var);
        }
        
        // ----------------------------------------------------------------------------------------
        // Other
        // ----------------------------------------------------------------------------------------
        
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
