package org.metaborg.meta.nabl2.unification.fast;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.metaborg.meta.nabl2.terms.IListTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.ListTerms;
import org.metaborg.meta.nabl2.terms.Terms;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.util.tuples.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;
import org.metaborg.util.Ref;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;

public abstract class PersistentUnifier {

    public static class Transient implements IUnifier.Transient {

        private final boolean allowRecursive;

        private final Map.Transient<ITermVar, ITermVar> reps;
        private final Map.Transient<ITermVar, Integer> ranks;
        private final Map.Transient<ITermVar, ITerm> terms;

        private Transient(boolean allowRecursive, final Map.Transient<ITermVar, ITermVar> reps,
                final Map.Transient<ITermVar, Integer> ranks, final Map.Transient<ITermVar, ITerm> terms) {
            this.allowRecursive = allowRecursive;
            this.reps = reps;
            this.ranks = ranks;
            this.terms = terms;
        }

        public int size() {
            return reps.size();
        }

        public Set<ITermVar> varSet() {
            return reps.keySet();
        }

        @Override public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("{");
            boolean first = true;
            for(ITermVar var : terms.keySet()) {
                sb.append(first ? " " : ", ");
                first = false;
                sb.append(var);
                sb.append(" |-> ");
                sb.append(terms.get(var));
            }
            for(ITermVar var : reps.keySet()) {
                sb.append(first ? " " : ", ");
                first = false;
                sb.append(var);
                sb.append(" |-> ");
                sb.append(findRep(var));
            }
            sb.append(first ? "}" : " }");
            return sb.toString();
        }

        public static IUnifier.Transient of(boolean allowRecursive) {
            return new PersistentUnifier.Transient(allowRecursive, Map.Transient.of(), Map.Transient.of(),
                    Map.Transient.of());
        }

        ///////////////////////////////////////////
        // unify(ITerm, ITerm)
        ///////////////////////////////////////////

        public Set<ITermVar> unify(final ITerm left, final ITerm right) throws UnificationException {
            final Set<ITermVar> result = Sets.newHashSet();
            final Deque<Tuple2<ITerm, ITerm>> worklist = Lists.newLinkedList();
            worklist.push(ImmutableTuple2.of(left, right));
            while(!worklist.isEmpty()) {
                final Tuple2<ITerm, ITerm> work = worklist.pop();
                if(!unifyTerms(work._1(), work._2(), worklist, result)) {
                    throw new UnificationException(left, right);
                }
            }
            if(!allowRecursive && isCyclic(result)) {
                throw new UnificationException(left, right);
            }
            return result;
        }

        private boolean unifyTerms(final ITerm left, final ITerm right, final Deque<Tuple2<ITerm, ITerm>> worklist,
                Set<ITermVar> result) {
            // @formatter:off
            return left.match(Terms.cases(
                applLeft -> M.cases(
                    M.appl(applRight -> applLeft.getOp().equals(applRight.getOp()) &&
                                        applLeft.getArity() == applRight.getArity() &&
                                        unifys(applLeft.getArgs(), applRight.getArgs(), worklist)),
                    M.var(varRight -> unifyVarTerm(varRight, applLeft, worklist, result))
                ).match(right).orElse(false),
                listLeft -> M.cases(
                    M.list(listRight -> listLeft.match(ListTerms.cases(
                        consLeft -> M.cases(
                            M.cons(consRight -> {
                                worklist.push(ImmutableTuple2.of(consLeft.getHead(), consRight.getHead()));
                                worklist.push(ImmutableTuple2.of(consLeft.getTail(), consRight.getTail()));
                                return true;
                            }),
                            M.var(varRight -> unifyVarTerm(varRight, consLeft, worklist, result))
                        ).match(listRight).orElse(false),
                        nilLeft -> M.cases(
                            M.nil(nilRight -> true),
                            M.var(varRight -> unifyVarTerm(varRight, nilLeft, worklist, result))
                        ).match(listRight).orElse(false),
                        varLeft -> M.cases(
                            M.var(varRight -> unifyVars(varLeft, varRight, worklist, result)),
                            M.term(termRight -> unifyVarTerm(varLeft, termRight, worklist, result))
                        ).match(listRight).orElse(false)
                    ))),
                    M.var(varRight -> unifyVarTerm(varRight, listLeft, worklist, result))
                ).match(right).orElse(false),
                stringLeft -> M.cases(
                    M.string(stringRight -> stringLeft.getValue().equals(stringRight.getValue())),
                    M.var(varRight -> unifyVarTerm(varRight, stringLeft, worklist, result))
                ).match(right).orElse(false),
                integerLeft -> M.cases(
                    M.integer(integerRight -> integerLeft.getValue() != integerRight.getValue()),
                    M.var(varRight -> unifyVarTerm(varRight, integerLeft, worklist, result))
                ).match(right).orElse(false),
                blobLeft -> M.cases(
                    M.blob(blobRight -> blobLeft.getValue().equals(blobRight.getValue())),
                    M.var(varRight -> unifyVarTerm(varRight, blobLeft, worklist, result))
                ).match(right).orElse(false),
                varLeft -> M.cases(
                    // match var before term, or term will always match
                    M.var(varRight -> unifyVars(varLeft, varRight, worklist, result)),
                    M.term(termRight -> unifyVarTerm(varLeft, termRight, worklist, result))
                ).match(right).orElse(false)
            ));
            // @formatter:on
        }

        private boolean unifyVarTerm(final ITermVar var, final ITerm term, final Deque<Tuple2<ITerm, ITerm>> worklist,
                Set<ITermVar> result) {
            final ITermVar rep = findRep(var);
            if(terms.containsKey(rep)) {
                worklist.push(ImmutableTuple2.of(terms.get(rep), term));
            } else {
                terms.put(var, term);
                result.add(var);
            }
            return true;
        }

        private boolean unifyVars(final ITermVar left, final ITermVar right, final Deque<Tuple2<ITerm, ITerm>> worklist,
                Set<ITermVar> result) {
            final ITermVar leftRep = findRep(left);
            final ITermVar rightRep = findRep(right);
            if(leftRep.equals(rightRep)) {
                return true;
            }
            final int leftRank = ranks.getOrDefault(leftRep, 1);
            final int rightRank = ranks.getOrDefault(rightRep, 1);
            final boolean swap = leftRank > rightRank;
            final ITermVar var = swap ? rightRep : leftRep; // the eliminated variable
            final ITermVar with = swap ? leftRep : rightRep; // the new representative
            ranks.put(with, leftRank + rightRank);
            reps.put(var, with);
            result.add(var);
            final ITerm term = terms.__remove(var); // term for the eliminated var
            if(term != null) {
                worklist.push(ImmutableTuple2.of(term, terms.getOrDefault(with, with)));
            }
            return true;
        }

        private boolean unifys(final Iterable<ITerm> lefts, final Iterable<ITerm> rights,
                final Deque<Tuple2<ITerm, ITerm>> worklist) {
            Iterator<ITerm> itLeft = lefts.iterator();
            Iterator<ITerm> itRight = rights.iterator();
            while(itLeft.hasNext()) {
                if(!itRight.hasNext()) {
                    return false;
                }
                worklist.push(ImmutableTuple2.of(itLeft.next(), itRight.next()));
            }
            if(itRight.hasNext()) {
                return false;
            }
            return true;
        }

        ///////////////////////////////////////////
        // find(ITerm)
        ///////////////////////////////////////////

        @Override public ITerm find(ITerm term) {
            return M.var(var -> {
                final ITermVar rep = findRep(var);
                return terms.getOrDefault(rep, rep);
            }).match(term).orElse(term);
        }

        private ITermVar findRep(ITermVar var) {
            if(!reps.containsKey(var)) {
                return var;
            } else {
                ITermVar rep = findRep(reps.get(var));
                reps.__put(var, rep);
                return rep;
            }
        }

        ///////////////////////////////////////////
        // isCyclic(ITerm)
        ///////////////////////////////////////////

        public boolean isCyclic(final ITerm term) {
            return isCyclic(term.getVars().elementSet(), Sets.newHashSet(), Sets.newHashSet());
        }

        private boolean isCyclic(final Set<ITermVar> vars) {
            return isCyclic(vars, Sets.newHashSet(), Sets.newHashSet());
        }

        private boolean isCyclic(final Set<ITermVar> vars, final Set<ITermVar> stack, final Set<ITermVar> visited) {
            return vars.stream().anyMatch(var -> isCyclic(var, stack, visited));
        }

        private boolean isCyclic(final ITermVar var, final Set<ITermVar> stack, final Set<ITermVar> visited) {
            final boolean cyclic;
            final ITermVar rep = findRep(var);
            if(!visited.contains(rep)) {
                stack.add(rep);
                visited.add(rep);
                final ITerm term = terms.get(rep);
                cyclic = term != null ? isCyclic(term.getVars().elementSet(), stack, visited) : false;
                stack.remove(rep);
            } else if(stack.contains(rep)) {
                cyclic = true;
            } else {
                cyclic = false;
            }
            return cyclic;
        }

        ///////////////////////////////////////////
        // isGround(ITerm)
        ///////////////////////////////////////////

        public boolean isGround(final ITerm term) {
            return isGround(term.getVars().elementSet(), Sets.newHashSet(), Sets.newHashSet());
        }

        private boolean isGround(final Set<ITermVar> vars, final Set<ITermVar> stack, final Set<ITermVar> visited) {
            return vars.stream().anyMatch(var -> isGround(var, stack, visited));
        }

        private boolean isGround(final ITermVar var, final Set<ITermVar> stack, final Set<ITermVar> visited) {
            final boolean ground;
            final ITermVar rep = findRep(var);
            if(!visited.contains(rep)) {
                stack.add(rep);
                visited.add(rep);
                final ITerm term = terms.get(rep);
                ground = term != null ? isGround(term.getVars().elementSet(), stack, visited) : false;
                stack.remove(rep);
            } else if(stack.contains(rep)) {
                ground = false;
            } else {
                ground = true;
            }
            return ground;
        }

        ///////////////////////////////////////////
        // getVars(ITerm)
        ///////////////////////////////////////////

        public Set<ITermVar> getVars(final ITerm term) {
            final Set<ITermVar> vars = Sets.newHashSet();
            getVars(term.getVars().elementSet(), Lists.newLinkedList(), Sets.newHashSet(), vars);
            return vars;
        }

        private void getVars(final Set<ITermVar> tryVars, final LinkedList<ITermVar> stack, final Set<ITermVar> visited,
                Set<ITermVar> vars) {
            tryVars.stream().forEach(var -> getVars(var, stack, visited, vars));
        }

        private void getVars(final ITermVar var, final LinkedList<ITermVar> stack, final Set<ITermVar> visited,
                Set<ITermVar> vars) {
            final ITermVar rep = findRep(var);
            if(!visited.contains(rep)) {
                visited.add(rep);
                stack.push(rep);
                final ITerm term = terms.get(rep);
                if(term != null) {
                    getVars(term.getVars().elementSet(), stack, visited, vars);
                } else {
                    vars.add(rep);
                }
                stack.pop();
            } else {
                final int index = stack.indexOf(rep); // linear
                if(index >= 0) {
                    List<ITermVar> repVars = stack.subList(0, index + 1);
                    vars.addAll(repVars);
                }
            }
        }

        ///////////////////////////////////////////
        // size(ITerm)
        ///////////////////////////////////////////

        public TermSize size(final ITerm term) {
            return size(term, Sets.newHashSet(), Maps.newHashMap());
        }

        private TermSize size(final ITerm term, final Set<ITermVar> stack,
                final java.util.Map<ITermVar, TermSize> visited) {
            return term.match(Terms.<TermSize>cases(
            // @formatter:off
                appl -> TermSize.ONE.add(sizes(appl.getArgs(), stack, visited)),
                list -> size(list, stack, visited),
                string -> TermSize.ONE,
                integer -> TermSize.ONE,
                blob -> TermSize.ONE,
                var -> size(var, stack, visited)
                // @formatter:on
            ));
        }

        private TermSize size(IListTerm list, final Set<ITermVar> stack,
                final java.util.Map<ITermVar, TermSize> visited) {
            final Ref<TermSize> size = new Ref<>(TermSize.ZERO);
            while(list != null) {
                list = list.match(ListTerms.cases(
                // @formatter:off
                    cons -> {
                        size.set(size.get().add(TermSize.ONE).add(size(cons.getHead(), stack, visited)));
                        return cons.getTail();
                    },
                    nil -> {
                        size.set(size.get().add(TermSize.ONE));
                        return null;
                    },
                    var -> {
                        size.set(size.get().add(size(var, stack, visited)));
                        return null;
                    }
                    // @formatter:on
                ));
            }
            return size.get();
        }

        private TermSize size(final ITermVar var, final Set<ITermVar> stack,
                final java.util.Map<ITermVar, TermSize> visited) {
            final ITermVar rep = findRep(var);
            final TermSize size;
            if(!visited.containsKey(rep)) {
                stack.add(var);
                visited.put(rep, TermSize.ZERO);
                final ITerm term = terms.get(rep);
                size = term != null ? size(term, stack, visited) : TermSize.ZERO;
                visited.put(rep, size);
                stack.remove(rep);
                return size;
            } else if(stack.contains(rep)) {
                size = TermSize.INF;
            } else {
                size = visited.get(rep);
            }
            return size;
        }

        private TermSize sizes(final Iterable<ITerm> terms, final Set<ITermVar> stack,
                final java.util.Map<ITermVar, TermSize> visited) {
            TermSize size = TermSize.ZERO;
            for(ITerm term : terms) {
                size = size.add(size(term, stack, visited));
            }
            return size;
        }

    }

}