package mb.nabl2.terms.unification;

import static mb.nabl2.terms.build.TermBuild.B;

import java.io.Serializable;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.metaborg.util.Ref;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.nabl2.util.CapsuleUtil;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Set2;
import mb.nabl2.util.Tuple2;

public abstract class PersistentUnifier implements IUnifier, Serializable {

    private static final long serialVersionUID = 42L;

    protected abstract Map<ITermVar, ITermVar> reps();

    protected abstract Map<ITermVar, ITerm> terms();

    ///////////////////////////////////////////
    // unifier functions
    ///////////////////////////////////////////

    @Override public boolean isEmpty() {
        return reps().isEmpty() && terms().isEmpty();
    }

    @Override public int size() {
        return reps().size() + terms().size();
    }

    @Override public boolean contains(ITermVar var) {
        return reps().containsKey(var) || terms().containsKey(var);
    }

    @Override public Set<ITermVar> repSet() {
        return ImmutableSet.copyOf(reps().values());
    }

    @Override public Set<ITermVar> varSet() {
        return Sets.union(reps().keySet(), terms().keySet());
    }

    @Override public Set<ITermVar> freeVarSet() {
        final Set<ITermVar> freeVars = Sets.newHashSet();
        reps().values().stream().filter(var -> !contains(var)).forEach(freeVars::add);
        terms().values().stream().flatMap(term -> term.getVars().elementSet().stream()).filter(var -> !contains(var))
                .forEach(freeVars::add);
        return freeVars;
    }

    @Override public boolean isCyclic() {
        return isCyclic(varSet());
    }

    ///////////////////////////////////////////
    // equals
    ///////////////////////////////////////////

    @Override public boolean equals(Object other) {
        if(other == null) {
            return false;
        }
        if(other == this) {
            return true;
        }
        if(!(other instanceof IUnifier)) {
            return false;
        }
        final IUnifier that = (IUnifier) other;
        return equals(that);
    }

    public boolean equals(IUnifier other) {
        final boolean equal;
        if(isFinite() != other.isFinite()) {
            equal = false;
        } else {
            final BiMap<ITermVar, ITermVar> freeMap = HashBiMap.create();
            final Multimap<ITermVar, ITermVar> instMap = HashMultimap.create();
            final Set<ITermVar> vars = Sets.union(varSet(), other.varSet());
            equal = vars.stream().allMatch(v -> equalVars(v, v, other, freeMap, instMap));
        }
        return equal;
    }

    private boolean equalTerms(ITerm thisTerm, ITerm thatTerm, IUnifier other, BiMap<ITermVar, ITermVar> freeMap,
            Multimap<ITermVar, ITermVar> instMap) {
        // @formatter:off
        return thisTerm.match(Terms.cases(
            applThis -> thatTerm.match(Terms.<Boolean>cases()
                .appl(applThat -> applThis.getOp().equals(applThat.getOp()) &&
                                   applThis.getArity() == applThat.getArity() &&
                                   equals(applThis.getArgs(), applThat.getArgs(), other, freeMap, instMap))
                .var(varThat -> equalTermVar(applThis, varThat, other, freeMap, instMap))
                .otherwise(t -> false)
            ),
            listThis -> thatTerm.match(Terms.<Boolean>cases()
                .list(listThat -> listThis.match(ListTerms.cases(
                    consThis -> listThat.match(ListTerms.<Boolean>cases()
                        .cons(consThat -> {
                            return equalTerms(consThis.getHead(), consThat.getHead(), other, freeMap, instMap) && 
                            equalTerms(consThis.getTail(), consThat.getTail(), other, freeMap, instMap);
                        })
                        .var(varThat -> equalTermVar(consThis, varThat, other, freeMap, instMap))
                        .otherwise(l -> false)
                    ),
                    nilThis -> listThat.match(ListTerms.<Boolean>cases()
                        .nil(nilThat -> true)
                        .var(varThat -> equalTermVar(nilThis, varThat, other, freeMap, instMap))
                        .otherwise(l -> false)
                    ),
                    varThis -> listThat.match(ListTerms.<Boolean>cases()
                        .var(varThat -> equalVars(varThis, varThat, other, freeMap, instMap))
                        .otherwise(termThat -> equalVarTerm(varThis, termThat, other, freeMap, instMap))
                    )
                )))
                .var(varThat -> equalTermVar(listThis, varThat, other, freeMap, instMap))
                .otherwise(t -> false)
            ),
            stringThis -> thatTerm.match(Terms.<Boolean>cases()
                .string(stringThat -> stringThis.getValue().equals(stringThat.getValue()))
                .var(varThat -> equalTermVar(stringThis, varThat, other, freeMap, instMap))
                .otherwise(t -> false)
            ),
            integerThis -> thatTerm.match(Terms.<Boolean>cases()
                .integer(integerThat -> integerThis.getValue() == integerThat.getValue())
                .var(varThat -> equalTermVar(integerThis, varThat, other, freeMap, instMap))
                .otherwise(t -> false)
            ),
            blobThis -> thatTerm.match(Terms.<Boolean>cases()
                .blob(blobThat -> blobThis.getValue().equals(blobThat.getValue()))
                .var(varThat -> equalTermVar(blobThis, varThat, other, freeMap, instMap))
                .otherwise(t -> false)
            ),
            varThis -> thatTerm.match(Terms.<Boolean>cases()
                // match var before term, or term will always match
                .var(varThat -> equalVars(varThis, varThat, other, freeMap, instMap))
                .otherwise(termThat -> equalVarTerm(varThis, termThat, other, freeMap, instMap))
            )
        ));
        // @formatter:on
    }

    private boolean equalVarTerm(final ITermVar thisVar, final ITerm thatTerm, final IUnifier other,
            BiMap<ITermVar, ITermVar> freeMap, Multimap<ITermVar, ITermVar> instMap) {
        if(hasTerm(thisVar)) {
            return equalTerms(findTerm(thisVar), thatTerm, other, freeMap, instMap);
        }
        return false;
    }

    private boolean equalTermVar(final ITerm thisTerm, final ITermVar thatVar, final IUnifier other,
            BiMap<ITermVar, ITermVar> freeMap, Multimap<ITermVar, ITermVar> instMap) {
        if(other.hasTerm(thatVar)) {
            return equalTerms(thisTerm, other.findTerm(thatVar), other, freeMap, instMap);
        }
        return false;
    }

    private boolean equalVars(ITermVar thisVar, ITermVar thatVar, IUnifier other, BiMap<ITermVar, ITermVar> freeMap,
            Multimap<ITermVar, ITermVar> instMap) {
        final ITermVar thisRep = findRep(thisVar);
        final ITermVar thatRep = other.findRep(thatVar);
        final boolean result;
        if(hasTerm(thisRep) && other.hasTerm(thatRep)) {
            if(instMap.containsEntry(thisRep, thatRep)) {
                result = true;
            } else {
                instMap.put(thisRep, thatRep);
                result = equalTerms(findTerm(thisRep), other.findTerm(thatRep), other, freeMap, instMap);
            }
        } else if(!hasTerm(thisRep) && !other.hasTerm(thatRep)) {
            if(freeMap.containsKey(thisRep) && freeMap.containsValue(thatRep)) {
                result = freeMap.get(thisRep).equals(thatRep);
            } else if(freeMap.containsKey(thisRep) || freeMap.containsValue(thatRep)) {
                result = false;
            } else {
                freeMap.put(thisRep, thatRep);
                result = true;
            }
        } else {
            result = false;
        }
        return result;
    }

    private boolean equals(final Iterable<ITerm> thisTerms, final Iterable<ITerm> thatTerms, final IUnifier other,
            BiMap<ITermVar, ITermVar> freeMap, Multimap<ITermVar, ITermVar> instMap) {
        Iterator<ITerm> itLeft = thisTerms.iterator();
        Iterator<ITerm> itRight = thatTerms.iterator();
        while(itLeft.hasNext()) {
            if(!itRight.hasNext()) {
                return false;
            }
            if(!equalTerms(itLeft.next(), itRight.next(), other, freeMap, instMap)) {
                return false;
            }
        }
        if(itRight.hasNext()) {
            return false;
        }
        return true;
    }

    @Override public int hashCode() {
        return Objects.hash(isFinite(), reps(), terms()); // FIXME: not exactly equivalent to equals implementation
    }

    ///////////////////////////////////////////
    // toString
    ///////////////////////////////////////////

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for(ITermVar var : terms().keySet()) {
            sb.append(first ? " " : ", ");
            first = false;
            sb.append(var);
            sb.append(" |-> ");
            sb.append(terms().get(var));
        }
        for(ITermVar var : reps().keySet()) {
            sb.append(first ? " " : ", ");
            first = false;
            sb.append(var);
            sb.append(" |-> ");
            sb.append(reps().get(var));
        }
        sb.append(first ? "}" : " }");
        return sb.toString();
    }

    ///////////////////////////////////////////
    // findTerm(ITerm) / findRep(ITerm)
    ///////////////////////////////////////////

    @Override public boolean hasTerm(ITermVar var) {
        return terms().containsKey(findRep(var));
    }

    @Override public ITerm findTerm(ITerm term) {
        return term.match(Terms.<ITerm>cases().var(var -> {
            final ITermVar rep = findRep(var);
            return terms().getOrDefault(rep, rep);
        }).otherwise(t -> t));
    }

    protected static ITermVar findRep(ITermVar var, Map.Transient<ITermVar, ITermVar> reps) {
        ITermVar rep = reps.get(var);
        if(rep == null) {
            return var;
        } else {
            rep = findRep(rep, reps);
            reps.__put(var, rep);
            return rep;
        }
    }

    ///////////////////////////////////////////
    // findRecursive(ITerm)
    ///////////////////////////////////////////

    @Override public ITerm findRecursive(final ITerm term) {
        return findTermRecursive(term, Sets.newHashSet(), Maps.newHashMap());
    }

    private ITerm findTermRecursive(final ITerm term, final Set<ITermVar> stack,
            final java.util.Map<ITermVar, ITerm> visited) {
        return term.match(Terms.cases(
        // @formatter:off
            appl -> B.newAppl(appl.getOp(), findRecursiveTerms(appl.getArgs(), stack, visited), appl.getAttachments()),
            list -> findListTermRecursive(list, stack, visited),
            string -> string,
            integer -> integer,
            blob -> blob,
            var -> findVarRecursive(var, stack, visited)
            // @formatter:on
        ));
    }

    private IListTerm findListTermRecursive(IListTerm list, final Set<ITermVar> stack,
            final java.util.Map<ITermVar, ITerm> visited) {
        Deque<IListTerm> elements = Lists.newLinkedList();
        while(list != null) {
            list = list.match(ListTerms.cases(
            // @formatter:off
                cons -> {
                    elements.push(cons);
                    return cons.getTail();
                },
                nil -> {
                    elements.push(nil);
                    return null;
                },
                var -> {
                    elements.push(var);
                    return null;
                }
                // @formatter:on
            ));
        }
        Ref<IListTerm> instance = new Ref<>();
        while(!elements.isEmpty()) {
            instance.set(elements.pop().match(ListTerms.<IListTerm>cases(
            // @formatter:off
                cons -> B.newCons(findTermRecursive(cons.getHead(), stack, visited), instance.get(), cons.getAttachments()),
                nil -> nil,
                var -> (IListTerm) findVarRecursive(var, stack, visited)
                // @formatter:on
            )));
        }
        return instance.get();
    }

    private ITerm findVarRecursive(final ITermVar var, final Set<ITermVar> stack,
            final java.util.Map<ITermVar, ITerm> visited) {
        final ITermVar rep = findRep(var);
        final ITerm instance;
        if(!visited.containsKey(rep)) {
            stack.add(rep);
            visited.put(rep, null);
            final ITerm term = terms().get(rep);
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

    private Iterable<ITerm> findRecursiveTerms(final Iterable<ITerm> terms, final Set<ITermVar> stack,
            final java.util.Map<ITermVar, ITerm> visited) {
        List<ITerm> instances = Lists.newArrayList();
        for(ITerm term : terms) {
            instances.add(findTermRecursive(term, stack, visited));
        }
        return instances;
    }

    ///////////////////////////////////////////
    // areEqual(ITerm, ITerm)
    ///////////////////////////////////////////

    @Override public boolean areEqual(final ITerm left, final ITerm right) {
        return equalTerms(left, right, Sets.newHashSet(), Maps.newHashMap());
    }

    private boolean equalTerms(final ITerm left, final ITerm right, final Set<Set2<ITermVar>> stack,
            final java.util.Map<Set2<ITermVar>, Boolean> visited) {
        // @formatter:off
        return left.match(Terms.cases(
            applLeft -> right.match(Terms.<Boolean>cases()
                .appl(applRight -> applLeft.getOp().equals(applRight.getOp()) &&
                                   applLeft.getArity() == applRight.getArity() &&
                                   equals(applLeft.getArgs(), applRight.getArgs(), stack, visited))
                .var(varRight -> equalVarTerm(varRight, applLeft, stack, visited))
                .otherwise(t -> false)
            ),
            listLeft -> right.match(Terms.<Boolean>cases()
                .list(listRight -> listLeft.match(ListTerms.cases(
                    consLeft -> listRight.match(ListTerms.<Boolean>cases()
                        .cons(consRight -> {
                            return equalTerms(consLeft.getHead(), consRight.getHead(), stack, visited) && 
                            equalTerms(consLeft.getTail(), consRight.getTail(), stack, visited);
                        })
                        .var(varRight -> equalVarTerm(varRight, consLeft, stack, visited))
                        .otherwise(l -> false)
                    ),
                    nilLeft -> listRight.match(ListTerms.<Boolean>cases()
                        .nil(nilRight -> true)
                        .var(varRight -> equalVarTerm(varRight, nilLeft, stack, visited))
                        .otherwise(l -> false)
                    ),
                    varLeft -> listRight.match(ListTerms.<Boolean>cases()
                        .var(varRight -> equalVars(varLeft, varRight, stack, visited))
                        .otherwise(termRight -> equalVarTerm(varLeft, termRight, stack, visited))
                    )
                )))
                .var(varRight -> equalVarTerm(varRight, listLeft, stack, visited))
                .otherwise(t -> false)
            ),
            stringLeft -> right.match(Terms.<Boolean>cases()
                .string(stringRight -> stringLeft.getValue().equals(stringRight.getValue()))
                .var(varRight -> equalVarTerm(varRight, stringLeft, stack, visited))
                .otherwise(t -> false)
            ),
            integerLeft -> right.match(Terms.<Boolean>cases()
                .integer(integerRight -> integerLeft.getValue() == integerRight.getValue())
                .var(varRight -> equalVarTerm(varRight, integerLeft, stack, visited))
                .otherwise(t -> false)
            ),
            blobLeft -> right.match(Terms.<Boolean>cases()
                .blob(blobRight -> blobLeft.getValue().equals(blobRight.getValue()))
                .var(varRight -> equalVarTerm(varRight, blobLeft, stack, visited))
                .otherwise(t -> false)
            ),
            varLeft -> right.match(Terms.<Boolean>cases()
                // match var before term, or term will always match
                .var(varRight -> equalVars(varLeft, varRight, stack, visited))
                .otherwise(termRight -> equalVarTerm(varLeft, termRight, stack, visited))
            )
        ));
        // @formatter:on
    }

    private boolean equalVarTerm(final ITermVar var, final ITerm term, final Set<Set2<ITermVar>> stack,
            final java.util.Map<Set2<ITermVar>, Boolean> visited) {
        final ITermVar rep = findRep(var);
        if(terms().containsKey(rep)) {
            return equalTerms(terms().get(rep), term, stack, visited);
        }
        return false;
    }

    private boolean equalVars(final ITermVar left, final ITermVar right, final Set<Set2<ITermVar>> stack,
            final java.util.Map<Set2<ITermVar>, Boolean> visited) {
        final ITermVar leftRep = findRep(left);
        final ITermVar rightRep = findRep(right);
        if(leftRep.equals(rightRep)) {
            return true;
        }
        final Set2<ITermVar> pair = Set2.of(leftRep, rightRep);
        final boolean equal;
        if(!visited.containsKey(pair)) {
            stack.add(pair);
            visited.put(pair, null);
            final ITerm leftTerm = terms().get(leftRep);
            final ITerm rightTerm = terms().get(rightRep);
            equal = (leftTerm != null && rightTerm != null) ? equalTerms(leftTerm, rightTerm, stack, visited) : false;
            visited.put(pair, equal);
            stack.remove(pair);
        } else if(stack.contains(pair)) {
            equal = false;
        } else {
            equal = visited.get(pair);
        }
        return equal;
    }

    private boolean equals(final Iterable<ITerm> lefts, final Iterable<ITerm> rights, final Set<Set2<ITermVar>> stack,
            final java.util.Map<Set2<ITermVar>, Boolean> visited) {
        Iterator<ITerm> itLeft = lefts.iterator();
        Iterator<ITerm> itRight = rights.iterator();
        while(itLeft.hasNext()) {
            if(!itRight.hasNext()) {
                return false;
            }
            if(!equalTerms(itLeft.next(), itRight.next(), stack, visited)) {
                return false;
            }
        }
        if(itRight.hasNext()) {
            return false;
        }
        return true;
    }

    ///////////////////////////////////////////
    // areUnequal(ITerm, ITerm)
    ///////////////////////////////////////////

    @Override public boolean areUnequal(final ITerm left, final ITerm right) {
        return unequalTerms(left, right, Sets.newHashSet(), Maps.newHashMap());
    }

    private boolean unequalTerms(final ITerm left, final ITerm right, Set<Set2<ITermVar>> stack,
            final java.util.Map<Set2<ITermVar>, Boolean> visited) {
        // @formatter:off
        return left.match(Terms.cases(
            applLeft -> right.match(Terms.<Boolean>cases()
                .appl(applRight -> !applLeft.getOp().equals(applRight.getOp()) ||
                                    applLeft.getArity() != applRight.getArity() ||
                                    unequals(applLeft.getArgs(), applRight.getArgs(), stack, visited))
                .var(varRight -> unequalVarTerm(varRight, applLeft, stack, visited))
                .otherwise(t -> true)
            ),
            listLeft -> right.match(Terms.<Boolean>cases()
                .list(listRight -> listLeft.match(ListTerms.cases(
                    consLeft -> listRight.match(ListTerms.<Boolean>cases()
                        .cons(consRight -> {
                            return unequalTerms(consLeft.getHead(), consRight.getHead(), stack, visited) || 
                                   unequalTerms(consLeft.getTail(), consRight.getTail(), stack, visited);
                        })
                        .var(varRight -> unequalVarTerm(varRight, consLeft, stack, visited))
                        .otherwise(t -> true)
                    ),
                    nilLeft -> listRight.match(ListTerms.<Boolean>cases()
                        .nil(nilRight -> false)
                        .var(varRight -> unequalVarTerm(varRight, nilLeft, stack, visited))
                        .otherwise(t -> true)
                    ),
                    varLeft -> listRight.match(ListTerms.<Boolean>cases()
                        .var(varRight -> unequalVars(varLeft, varRight, stack, visited))
                        .otherwise(termRight -> unequalVarTerm(varLeft, termRight, stack, visited))
                    )
                )))
                .var(varRight -> unequalVarTerm(varRight, listLeft, stack, visited))
                .otherwise(t -> true)
            ),
            stringLeft -> right.match(Terms.<Boolean>cases()
                .string(stringRight -> !stringLeft.getValue().equals(stringRight.getValue()))
                .var(varRight -> unequalVarTerm(varRight, stringLeft, stack, visited))
                .otherwise(t -> true)
            ),
            integerLeft -> right.match(Terms.<Boolean>cases()
                .integer(integerRight -> integerLeft.getValue() != integerRight.getValue())
                .var(varRight -> unequalVarTerm(varRight, integerLeft, stack, visited))
                .otherwise(t -> true)
            ),
            blobLeft -> right.match(Terms.<Boolean>cases()
                .blob(blobRight -> !blobLeft.getValue().equals(blobRight.getValue()))
                .var(varRight -> unequalVarTerm(varRight, blobLeft, stack, visited))
                .otherwise(t -> true)
            ),
            varLeft -> right.match(Terms.<Boolean>cases()
                // match var before term, or term will always match
                .var(varRight -> unequalVars(varLeft, varRight, stack, visited))
                .otherwise(termRight -> unequalVarTerm(varLeft, termRight, stack, visited))
            )
        ));
        // @formatter:on
    }

    private boolean unequalVarTerm(final ITermVar var, final ITerm term, Set<Set2<ITermVar>> stack,
            final java.util.Map<Set2<ITermVar>, Boolean> visited) {
        final ITermVar rep = findRep(var);
        if(terms().containsKey(rep)) {
            return unequalTerms(terms().get(rep), term, stack, visited);
        }
        return false;
    }

    private boolean unequalVars(final ITermVar left, final ITermVar right, Set<Set2<ITermVar>> stack,
            final java.util.Map<Set2<ITermVar>, Boolean> visited) {
        final ITermVar leftRep = findRep(left);
        final ITermVar rightRep = findRep(right);
        if(leftRep.equals(rightRep)) {
            return false;
        }
        final Set2<ITermVar> pair = Set2.of(leftRep, rightRep);
        final boolean unequal;
        if(!visited.containsKey(pair)) {
            stack.add(pair);
            visited.put(pair, null);
            final ITerm leftTerm = terms().get(leftRep);
            final ITerm rightTerm = terms().get(rightRep);
            unequal =
                    (leftTerm != null && rightTerm != null) ? unequalTerms(leftTerm, rightTerm, stack, visited) : false;
            visited.put(pair, unequal);
            stack.remove(pair);
        } else if(stack.contains(pair)) {
            unequal = false;
        } else {
            unequal = visited.get(pair);
        }
        return unequal;
    }

    private boolean unequals(final Iterable<ITerm> lefts, final Iterable<ITerm> rights, Set<Set2<ITermVar>> stack,
            final java.util.Map<Set2<ITermVar>, Boolean> visited) {
        Iterator<ITerm> itLeft = lefts.iterator();
        Iterator<ITerm> itRight = rights.iterator();
        while(itLeft.hasNext()) {
            if(!itRight.hasNext()) {
                return true;
            }
            if(unequalTerms(itLeft.next(), itRight.next(), stack, visited)) {
                return true;
            }
        }
        if(itRight.hasNext()) {
            return true;
        }
        return false;
    }

    ///////////////////////////////////////////
    // isCyclic(ITerm)
    ///////////////////////////////////////////

    @Override public boolean isCyclic(final ITerm term) {
        return isCyclic(term.getVars().elementSet(), Sets.newHashSet(), Maps.newHashMap());
    }

    protected boolean isCyclic(final Set<ITermVar> vars) {
        return isCyclic(vars, Sets.newHashSet(), Maps.newHashMap());
    }

    private boolean isCyclic(final Set<ITermVar> vars, final Set<ITermVar> stack,
            final java.util.Map<ITermVar, Boolean> visited) {
        return vars.stream().anyMatch(var -> isCyclic(var, stack, visited));
    }

    private boolean isCyclic(final ITermVar var, final Set<ITermVar> stack,
            final java.util.Map<ITermVar, Boolean> visited) {
        final boolean cyclic;
        final ITermVar rep = findRep(var);
        if(!visited.containsKey(rep)) {
            stack.add(rep);
            visited.put(rep, null);
            final ITerm term = terms().get(rep);
            cyclic = term != null ? isCyclic(term.getVars().elementSet(), stack, visited) : false;
            visited.put(rep, cyclic);
            stack.remove(rep);
        } else if(stack.contains(rep)) {
            cyclic = true;
        } else {
            cyclic = visited.get(rep);
        }
        return cyclic;
    }

    ///////////////////////////////////////////
    // isGround(ITerm)
    ///////////////////////////////////////////

    @Override public boolean isGround(final ITerm term) {
        return isGround(term.getVars().elementSet(), Sets.newHashSet(), Maps.newHashMap());
    }

    private boolean isGround(final Set<ITermVar> vars, final Set<ITermVar> stack,
            final java.util.Map<ITermVar, Boolean> visited) {
        return vars.stream().allMatch(var -> isGround(var, stack, visited));
    }

    private boolean isGround(final ITermVar var, final Set<ITermVar> stack,
            final java.util.Map<ITermVar, Boolean> visited) {
        final boolean ground;
        final ITermVar rep = findRep(var);
        if(!visited.containsKey(rep)) {
            stack.add(rep);
            visited.put(rep, null);
            final ITerm term = terms().get(rep);
            ground = term != null ? isGround(term.getVars().elementSet(), stack, visited) : false;
            visited.put(rep, ground);
            stack.remove(rep);
        } else if(stack.contains(rep)) {
            ground = true;
        } else {
            ground = visited.get(rep);
        }
        return ground;
    }

    ///////////////////////////////////////////
    // getVars(ITerm)
    ///////////////////////////////////////////

    @Override public Set<ITermVar> getVars(final ITerm term) {
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
            final ITerm term = terms().get(rep);
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

    ///////////////////////////////////////////
    // size(ITerm)
    ///////////////////////////////////////////

    @Override public TermSize size(final ITerm term) {
        return size(term, Sets.newHashSet(), Maps.newHashMap());
    }

    private TermSize size(final ITerm term, final Set<ITermVar> stack,
            final java.util.Map<ITermVar, TermSize> visited) {
        return term.match(Terms.cases(
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

    private TermSize size(IListTerm list, final Set<ITermVar> stack, final java.util.Map<ITermVar, TermSize> visited) {
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
            stack.add(rep);
            visited.put(rep, null);
            final ITerm term = terms().get(rep);
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

    ///////////////////////////////////////////
    // toString(ITerm)
    ///////////////////////////////////////////

    @Override public String toString(final ITerm term) {
        return toString(term, Maps.newHashMap(), Maps.newHashMap());
    }

    private String toString(final ITerm term, final java.util.Map<ITermVar, String> stack,
            final java.util.Map<ITermVar, String> visited) {
        return term.match(Terms.cases(
        // @formatter:off
            appl -> appl.getOp() + "(" + toStrings(appl.getArgs(), stack, visited) + ")",
            list -> toString(list, stack, visited),
            string -> string.toString(),
            integer -> integer.toString(),
            blob -> blob.toString(),
            var -> toString(var, stack, visited)
            // @formatter:on
        ));
    }

    private String toString(IListTerm list, final java.util.Map<ITermVar, String> stack,
            final java.util.Map<ITermVar, String> visited) {
        final StringBuilder sb = new StringBuilder();
        final AtomicBoolean tail = new AtomicBoolean();
        sb.append("[");
        while(list != null) {
            list = list.match(ListTerms.cases(
            // @formatter:off
                cons -> {
                    if(tail.getAndSet(true)) {
                        sb.append(",");
                    }
                    sb.append(toString(cons.getHead(), stack, visited));
                    return cons.getTail();
                },
                nil -> {
                    return null;
                },
                var -> {
                    sb.append("|");
                    sb.append(toString(var, stack, visited));
                    return null;
                }
                // @formatter:on
            ));
        }
        sb.append("]");
        return sb.toString();
    }

    private String toString(final ITermVar var, final java.util.Map<ITermVar, String> stack,
            final java.util.Map<ITermVar, String> visited) {
        final ITermVar rep = findRep(var);
        final String toString;
        if(!visited.containsKey(rep)) {
            stack.put(rep, null);
            visited.put(rep, null);
            final ITerm term = terms().get(rep);
            if(term != null) {
                final String termString = toString(term, stack, visited);
                toString = (stack.get(rep) != null ? "μ" + stack.get(rep) + "." : "") + termString;
            } else {
                toString = rep.toString();
            }
            visited.put(rep, toString);
            stack.remove(rep);
            return toString;
        } else if(stack.containsKey(rep)) {
            final String muVar;
            if(stack.get(rep) == null) {
                muVar = "X" + stack.values().stream().filter(v -> v != null).count();
                stack.put(rep, muVar);
            } else {
                muVar = stack.get(rep);
            }
            toString = muVar;
        } else {
            toString = visited.get(rep);
        }
        return toString;
    }

    private String toStrings(final Iterable<ITerm> terms, final java.util.Map<ITermVar, String> stack,
            final java.util.Map<ITermVar, String> visited) {
        final StringBuilder sb = new StringBuilder();
        final AtomicBoolean tail = new AtomicBoolean();
        for(ITerm term : terms) {
            if(tail.getAndSet(true)) {
                sb.append(",");
            }
            sb.append(toString(term, stack, visited));
        }
        return sb.toString();
    }

    ///////////////////////////////////////////
    // class Immutable
    ///////////////////////////////////////////

    public static class Immutable extends PersistentUnifier implements IUnifier.Immutable, Serializable {

        private static final long serialVersionUID = 42L;

        private final boolean finite;

        private final Ref<Map.Immutable<ITermVar, ITermVar>> reps;
        private final Map.Immutable<ITermVar, Integer> ranks;
        private final Map.Immutable<ITermVar, ITerm> terms;

        Immutable(final boolean finite, final Map.Immutable<ITermVar, ITermVar> reps,
                final Map.Immutable<ITermVar, Integer> ranks, final Map.Immutable<ITermVar, ITerm> terms) {
            this.finite = finite;
            this.reps = new Ref<>(reps);
            this.ranks = ranks;
            this.terms = terms;
        }

        @Override public boolean isFinite() {
            return finite;
        }

        @Override protected Map<ITermVar, ITermVar> reps() {
            return reps.get();
        }

        @Override protected Map<ITermVar, ITerm> terms() {
            return terms;
        }

        @Override public IUnifier.Immutable.Result<IUnifier.Immutable> unify(ITerm left, ITerm right)
                throws UnificationException {
            final IUnifier.Transient unifier = melt();
            final IUnifier.Immutable diff = unifier.unify(left, right);
            return new PersistentUnifier.Result<>(diff, unifier.freeze());
        }

        @Override public IUnifier.Immutable.Result<IUnifier.Immutable> unify(IUnifier other)
                throws UnificationException {
            final IUnifier.Transient unifier = melt();
            final IUnifier.Immutable diff = unifier.unify(other);
            return new PersistentUnifier.Result<>(diff, unifier.freeze());
        }

        @Override public ITermVar findRep(ITermVar var) {
            final Map.Transient<ITermVar, ITermVar> reps = this.reps.get().asTransient();
            final ITermVar rep = findRep(var, reps);
            this.reps.set(reps.freeze());
            return rep;
        }

        @Override public IUnifier.Immutable.Result<ISubstitution.Immutable> retain(ITermVar var) {
            final IUnifier.Transient unifier = melt();
            ISubstitution.Immutable result = unifier.retain(var);
            return new PersistentUnifier.Result<>(result, unifier.freeze());
        }

        @Override public IUnifier.Immutable.Result<ISubstitution.Immutable> retainAll(Iterable<ITermVar> vars) {
            final IUnifier.Transient unifier = melt();
            ISubstitution.Immutable result = unifier.retainAll(vars);
            return new PersistentUnifier.Result<>(result, unifier.freeze());
        }

        @Override public IUnifier.Immutable.Result<ISubstitution.Immutable> remove(ITermVar var) {
            final IUnifier.Transient unifier = melt();
            ISubstitution.Immutable result = unifier.remove(var);
            return new PersistentUnifier.Result<>(result, unifier.freeze());
        }

        @Override public IUnifier.Immutable.Result<ISubstitution.Immutable> removeAll(Iterable<ITermVar> vars) {
            final IUnifier.Transient unifier = melt();
            ISubstitution.Immutable result = unifier.removeAll(vars);
            return new PersistentUnifier.Result<>(result, unifier.freeze());
        }

        @Override public IUnifier.Transient melt() {
            return new PersistentUnifier.Transient(finite, reps.get().asTransient(), ranks.asTransient(),
                    terms.asTransient());
        }

        public static IUnifier.Immutable of() {
            return of(true);
        }

        public static IUnifier.Immutable of(boolean finite) {
            return new PersistentUnifier.Immutable(finite, Map.Immutable.of(), Map.Immutable.of(), Map.Immutable.of());
        }

    }

    ///////////////////////////////////////////
    // class Transient
    ///////////////////////////////////////////

    public static class Transient extends PersistentUnifier implements IUnifier.Transient, Serializable {

        private static final long serialVersionUID = 42L;

        private final boolean finite;

        private final Map.Transient<ITermVar, ITermVar> reps;
        private final Map.Transient<ITermVar, Integer> ranks;
        private final Map.Transient<ITermVar, ITerm> terms;

        Transient(final boolean finite, final Map.Transient<ITermVar, ITermVar> reps,
                final Map.Transient<ITermVar, Integer> ranks, final Map.Transient<ITermVar, ITerm> terms) {
            this.finite = finite;
            this.reps = reps;
            this.ranks = ranks;
            this.terms = terms;
        }

        @Override public boolean isFinite() {
            return finite;
        }

        @Override protected Map<ITermVar, ITermVar> reps() {
            return reps;
        }

        @Override protected Map<ITermVar, ITerm> terms() {
            return terms;
        }

        @Override public ITermVar findRep(ITermVar var) {
            return findRep(var, reps);
        }

        @Override public PersistentUnifier.Immutable freeze() {
            return new PersistentUnifier.Immutable(finite, reps.freeze(), ranks.freeze(), terms.freeze());
        }

        public static IUnifier.Transient of() {
            return of(true);
        }

        public static IUnifier.Transient of(boolean finite) {
            return new PersistentUnifier.Transient(finite, Map.Transient.of(), Map.Transient.of(), Map.Transient.of());
        }

        ///////////////////////////////////////////
        // unify(ITerm, ITerm)
        ///////////////////////////////////////////

        @Override public IUnifier.Immutable unify(final ITerm left, final ITerm right) throws UnificationException {
            final Set<ITermVar> result = Sets.newHashSet();
            final Deque<Tuple2<ITerm, ITerm>> worklist = Lists.newLinkedList();
            worklist.push(ImmutableTuple2.of(left, right));
            while(!worklist.isEmpty()) {
                final Tuple2<ITerm, ITerm> work = worklist.pop();
                if(!unifyTerms(work._1(), work._2(), worklist, result)) {
                    throw new UnificationException(left, right);
                }
            }
            if(isFinite() && isCyclic(result)) {
                throw new UnificationException(left, right);
            }
            return diffUnifier(result);
        }

        @Override public IUnifier.Immutable unify(IUnifier other) throws UnificationException {
            final Set<ITermVar> result = Sets.newHashSet();
            final Deque<Tuple2<ITerm, ITerm>> worklist = Lists.newLinkedList();
            for(ITermVar var : other.varSet()) {
                final ITermVar rep = other.findRep(var);
                if(!var.equals(rep)) {
                    worklist.push(ImmutableTuple2.of(var, rep));
                } else {
                    final ITerm term = other.findTerm(var);
                    if(!var.equals(term)) {
                        worklist.push(ImmutableTuple2.of(var, term));
                    }
                }
            }
            while(!worklist.isEmpty()) {
                final Tuple2<ITerm, ITerm> work = worklist.pop();
                if(!unifyTerms(work._1(), work._2(), worklist, result)) {
                    throw new UnificationException(work._1(), work._2());
                }
            }
            if(isFinite() && isCyclic(result)) {
                throw new UnificationException(B.newTuple(), B.newTuple()); // FIXME
            }
            return diffUnifier(result);
        }

        private boolean unifyTerms(final ITerm left, final ITerm right, final Deque<Tuple2<ITerm, ITerm>> worklist,
                Set<ITermVar> result) {
            // @formatter:off
            return left.match(Terms.cases(
                applLeft -> right.match(Terms.<Boolean>cases()
                    .appl(applRight -> applLeft.getOp().equals(applRight.getOp()) &&
                                        applLeft.getArity() == applRight.getArity() &&
                                        unifys(applLeft.getArgs(), applRight.getArgs(), worklist))
                    .var(varRight -> unifyTerms(varRight, applLeft, worklist, result))
                    .otherwise(t -> false)
                ),
                listLeft -> right.match(Terms.<Boolean>cases()
                    .list(listRight -> unifyLists(listLeft, listRight, worklist, result))
                    .var(varRight -> unifyTerms(varRight, listLeft, worklist, result))
                    .otherwise(t -> false)
                ),
                stringLeft -> right.match(Terms.<Boolean>cases()
                    .string(stringRight -> stringLeft.getValue().equals(stringRight.getValue()))
                    .var(varRight -> unifyTerms(varRight, stringLeft, worklist, result))
                    .otherwise(t -> false)
                ),
                integerLeft -> right.match(Terms.<Boolean>cases()
                    .integer(integerRight -> integerLeft.getValue() == integerRight.getValue())
                    .var(varRight -> unifyTerms(varRight, integerLeft, worklist, result))
                    .otherwise(t -> false)
                ),
                blobLeft -> right.match(Terms.<Boolean>cases()
                    .blob(blobRight -> blobLeft.getValue().equals(blobRight.getValue()))
                    .var(varRight -> unifyTerms(varRight, blobLeft, worklist, result))
                    .otherwise(t -> false)
                ),
                varLeft -> right.match(Terms.<Boolean>cases()
                    // match var before term, or term will always match
                    .var(varRight -> unifyVars(varLeft, varRight, worklist, result))
                    .otherwise(termRight -> unifyVarTerm(varLeft, termRight, worklist, result))
                )
            ));
            // @formatter:on
        }

        private boolean unifyLists(final IListTerm left, final IListTerm right,
                final Deque<Tuple2<ITerm, ITerm>> worklist, Set<ITermVar> result) {
            return left.match(ListTerms.cases(
            // @formatter:off
                consLeft -> right.match(ListTerms.<Boolean>cases()
                    .cons(consRight -> {
                        worklist.push(ImmutableTuple2.of(consLeft.getHead(), consRight.getHead()));
                        worklist.push(ImmutableTuple2.of(consLeft.getTail(), consRight.getTail()));
                        return true;
                    })
                    .var(varRight -> unifyLists(varRight, consLeft, worklist, result))
                    .otherwise(l -> false)
                ),
                nilLeft -> right.match(ListTerms.<Boolean>cases()
                    .nil(nilRight -> true)
                    .var(varRight -> unifyVarTerm(varRight, nilLeft, worklist, result))
                    .otherwise(l -> false)
                ),
                varLeft -> right.match(ListTerms.<Boolean>cases()
                    .var(varRight -> unifyVars(varLeft, varRight, worklist, result))
                    .otherwise(termRight -> unifyVarTerm(varLeft, termRight, worklist, result))
                )
                // @formatter:on
            ));
        }

        private boolean unifyVarTerm(final ITermVar var, final ITerm term, final Deque<Tuple2<ITerm, ITerm>> worklist,
                Set<ITermVar> result) {
            final ITermVar rep = findRep(var);
            if(terms.containsKey(rep)) {
                worklist.push(ImmutableTuple2.of(terms.get(rep), term));
            } else {
                terms.__put(rep, term);
                result.add(rep);
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
            ranks.__put(with, leftRank + rightRank);
            reps.__put(var, with);
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
        // retain(ITermVar)
        ///////////////////////////////////////////

        public ISubstitution.Immutable retain(ITermVar var) {
            return retainAll(Collections.singleton(var));
        }

        @Override public ISubstitution.Immutable retainAll(Iterable<ITermVar> vars) {
            return removeAll(Sets.difference(ImmutableSet.copyOf(varSet()), ImmutableSet.copyOf(vars)));
        }

        ///////////////////////////////////////////
        // remove(ITermVar)
        ///////////////////////////////////////////

        @Override public ISubstitution.Immutable removeAll(Iterable<ITermVar> vars) {
            final ISubstitution.Transient subst = PersistentSubstitution.Transient.of();
            for(ITermVar var : vars) {
                subst.compose(remove(var));
            }
            return subst.freeze();
        }

        @Override public ISubstitution.Immutable remove(ITermVar var) {
            final ISubstitution.Immutable subst;
            if(reps.containsKey(var)) { // var |-> rep
                final ITermVar rep = reps.__remove(var);
                CapsuleUtil.replace(reps, (v, r) -> r.equals(var) ? rep : r);
                subst = PersistentSubstitution.Immutable.of(var, rep);
            } else {
                final Optional<ITermVar> maybeNewRep =
                        reps.entrySet().stream().filter(e -> e.getValue().equals(var)).map(e -> e.getKey()).findAny();
                if(maybeNewRep.isPresent()) { // newRep |-> var
                    final ITermVar newRep = maybeNewRep.get();
                    reps.__remove(newRep);
                    CapsuleUtil.replace(reps, (v, r) -> r.equals(var) ? newRep : r);
                    if(terms.containsKey(var)) { // var -> term
                        final ITerm term = terms.__remove(var);
                        terms.__put(newRep, term);
                    }
                    subst = PersistentSubstitution.Immutable.of(var, newRep);
                } else {
                    if(terms.containsKey(var)) { // var -> term
                        final ITerm term = terms.__remove(var);
                        subst = PersistentSubstitution.Immutable.of(var, term);
                    } else { // var free -- cannot eliminate
                        subst = PersistentSubstitution.Immutable.of();
                    }
                }
            }
            CapsuleUtil.replace(terms, (v, t) -> subst.apply(t));
            return subst;
        }

        ///////////////////////////////////////////
        // diffUnifier(Set<ITermVar>)
        ///////////////////////////////////////////

        private IUnifier.Immutable diffUnifier(Set<ITermVar> vars) {
            final Map.Transient<ITermVar, ITermVar> diffReps = Map.Transient.of();
            final Map.Transient<ITermVar, ITerm> diffTerms = Map.Transient.of();
            for(ITermVar var : vars) {
                if(reps.containsKey(var)) {
                    diffReps.__put(var, reps.get(var));
                } else if(terms.containsKey(var)) {
                    diffTerms.__put(var, terms.get(var));
                }
            }
            return new PersistentUnifier.Immutable(finite, diffReps.freeze(), Map.Immutable.of(), diffTerms.freeze());
        }

    }

    ///////////////////////////////////////////
    // class Result
    ///////////////////////////////////////////

    private static class Result<T> implements IUnifier.Immutable.Result<T> {

        private final T result;
        private final IUnifier.Immutable unifier;

        private Result(T result, IUnifier.Immutable unifier) {
            this.result = result;
            this.unifier = unifier;
        }

        @Override public T result() {
            return result;
        }

        public IUnifier.Immutable unifier() {
            return unifier;
        }

    }

}
