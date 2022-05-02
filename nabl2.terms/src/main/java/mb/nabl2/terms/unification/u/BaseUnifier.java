package mb.nabl2.terms.unification.u;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.metaborg.util.Ref;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.PartialFunction1;
import org.metaborg.util.functions.Predicate1;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.IBlobTerm;
import mb.nabl2.terms.IConsTerm;
import mb.nabl2.terms.IIntTerm;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.INilTerm;
import mb.nabl2.terms.IStringTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.RigidException;
import mb.nabl2.terms.unification.SpecializedTermFormatter;
import mb.nabl2.terms.unification.TermSize;

import static mb.nabl2.terms.build.TermBuild.B;

public abstract class BaseUnifier implements IUnifier, Serializable {

    private static final long serialVersionUID = 42L;

    protected abstract Map.Immutable<ITermVar, ITermVar> reps();

    protected abstract Map.Immutable<ITermVar, ITerm> terms();

    ///////////////////////////////////////////
    // unifier functions
    ///////////////////////////////////////////

    @Override public boolean isEmpty() {
        return reps().isEmpty() && terms().isEmpty();
    }

    @Override public boolean contains(ITermVar var) {
        return reps().containsKey(var) || terms().containsKey(var);
    }

    @Override public boolean isCyclic() {
        return isCyclic(domainSet());
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
            sb.append(" == ");
            sb.append(terms().get(var));
        }
        for(ITermVar var : reps().keySet()) {
            sb.append(first ? " " : ", ");
            first = false;
            sb.append(var);
            sb.append(" == ");
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
        ITerm.Tag tag = term.termTag();
        if(tag == ITerm.Tag.ITermVar) {
            ITermVar var = (ITermVar) term;
            final ITermVar rep = findRep(var);
            return terms().getOrDefault(rep, rep);
        } else {
            return term;
        }
    }

    ///////////////////////////////////////////
    // findRecursive(ITerm)
    ///////////////////////////////////////////

    @Override public ITerm findRecursive(final ITerm term) {
        return findTermRecursive(term, Sets.newHashSet(), Maps.newHashMap());
    }

    private ITerm findTermRecursive(final ITerm term, final java.util.Set<ITermVar> stack,
            final java.util.Map<ITermVar, ITerm> visited) {
        switch(term.termTag()) {
            case IApplTerm: { IApplTerm appl = (IApplTerm) term;
                return B.newAppl(appl.getOp(), findRecursiveTerms(appl.getArgs(), stack, visited),
                    appl.getAttachments());
            }

            case IConsTerm:
            case INilTerm: { IListTerm list = (IListTerm) term;
                return findListTermRecursive(list, stack, visited);
            }

            case ITermVar: { ITermVar var = (ITermVar) term;
                return findVarRecursive(var, stack, visited);
            }

            case IStringTerm:
            case IIntTerm:
            case IBlobTerm: {
                return term;
            }
        }
        // N.B. don't use this in default case branch, instead use IDE to catch non-exhaustive switch statements
        throw new RuntimeException("Missing case for ITerm subclass/tag");
    }

    private IListTerm findListTermRecursive(IListTerm list, final java.util.Set<ITermVar> stack,
            final java.util.Map<ITermVar, ITerm> visited) {
        Deque<IListTerm> elements = new ArrayDeque<>();
        while(list != null) {
            switch(list.listTermTag()) {
                case IConsTerm: { IConsTerm cons = (IConsTerm) list;
                    elements.push(cons);
                    list =  cons.getTail();
                    continue;
                }

                case INilTerm:
                case ITermVar: {
                    elements.push(list);
                    list =  null;
                    continue;
                }
            }
            // N.B. don't use this in default case branch, instead use IDE to catch non-exhaustive switch statements
            throw new RuntimeException("Missing case for IListTerm subclass/tag");
        }
        Ref<IListTerm> instance = new Ref<>();
        while(!elements.isEmpty()) {
            IListTerm element = elements.pop();
            switch(element.listTermTag()) {
                case IConsTerm: { IConsTerm cons = (IConsTerm) element;
                    instance.set(B.newCons(findTermRecursive(cons.getHead(), stack, visited),
                        instance.get(), cons.getAttachments()));
                    continue;
                }

                case INilTerm: { INilTerm nil = (INilTerm) element;
                    instance.set(nil);
                    continue;
                }

                case ITermVar: { ITermVar var = (ITermVar) element;
                    instance.set((IListTerm) findVarRecursive(var, stack, visited));
                    continue;
                }
            }
            // N.B. don't use this in default case branch, instead use IDE to catch non-exhaustive switch statements
            throw new RuntimeException("Missing case for IListTerm subclass/tag");
        }
        return instance.get();
    }

    private ITerm findVarRecursive(final ITermVar var, final java.util.Set<ITermVar> stack,
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

    private Iterable<ITerm> findRecursiveTerms(final Iterable<ITerm> terms, final java.util.Set<ITermVar> stack,
            final java.util.Map<ITermVar, ITerm> visited) {
        List<ITerm> instances = new ArrayList<>();
        for(ITerm term : terms) {
            instances.add(findTermRecursive(term, stack, visited));
        }
        return instances;
    }

    ///////////////////////////////////////////
    // isCyclic(ITerm)
    ///////////////////////////////////////////

    @Override public boolean isCyclic(final ITerm term) {
        return isCyclic(term.getVars(), Sets.newHashSet(), Maps.newHashMap());
    }

    protected boolean isCyclic(final java.util.Set<ITermVar> vars) {
        return isCyclic(vars, Sets.newHashSet(), Maps.newHashMap());
    }

    private boolean isCyclic(final java.util.Set<ITermVar> vars, final java.util.Set<ITermVar> stack,
            final java.util.Map<ITermVar, Boolean> visited) {
        for(ITermVar var : vars) {
            if(isCyclic(var, stack, visited)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCyclic(final ITermVar var, final java.util.Set<ITermVar> stack,
            final java.util.Map<ITermVar, Boolean> visited) {
        final boolean cyclic;
        final ITermVar rep = findRep(var);
        if(!visited.containsKey(rep)) {
            stack.add(rep);
            visited.put(rep, null);
            final ITerm term = terms().get(rep);
            cyclic = term != null ? isCyclic(term.getVars(), stack, visited) : false;
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
        return isGround(term.getVars(), Sets.newHashSet(), Maps.newHashMap());
    }

    private boolean isGround(final java.util.Set<ITermVar> vars, final java.util.Set<ITermVar> stack,
            final java.util.Map<ITermVar, Boolean> visited) {
        return vars.stream().allMatch(var -> isGround(var, stack, visited));
    }

    private boolean isGround(final ITermVar var, final java.util.Set<ITermVar> stack,
            final java.util.Map<ITermVar, Boolean> visited) {
        final boolean ground;
        final ITermVar rep = findRep(var);
        if(!visited.containsKey(rep)) {
            stack.add(rep);
            visited.put(rep, null);
            final ITerm term = terms().get(rep);
            ground = term != null ? isGround(term.getVars(), stack, visited) : false;
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

    @Override public Set.Immutable<ITermVar> getVars(final ITerm term) {
        final Set.Transient<ITermVar> vars = CapsuleUtil.transientSet();
        getVars(term.getVars(), Lists.newLinkedList(), Sets.newHashSet(), vars);
        return vars.freeze();
    }

    private void getVars(final java.util.Set<ITermVar> tryVars, final LinkedList<ITermVar> stack,
            final java.util.Set<ITermVar> visited, Set.Transient<ITermVar> vars) {
        for(ITermVar var : tryVars) {
            getVars(var, stack, visited, vars);
        }
    }

    private void getVars(final ITermVar var, final LinkedList<ITermVar> stack, final java.util.Set<ITermVar> visited,
            Set.Transient<ITermVar> vars) {
        final ITermVar rep = findRep(var);
        if(!visited.contains(rep)) {
            visited.add(rep);
            stack.push(rep);
            final ITerm term = terms().get(rep);
            if(term != null) {
                getVars(term.getVars(), stack, visited, vars);
            } else {
                vars.__insert(rep);
            }
            stack.pop();
        } else {
            final int index = stack.indexOf(rep); // linear
            if(index >= 0) {
                for(ITermVar v : stack.subList(0, index + 1)) {
                    vars.__insert(v);
                }
            }
        }
    }

    ///////////////////////////////////////////
    // size(ITerm)
    ///////////////////////////////////////////

    @Override public TermSize size(final ITerm term) {
        return size(term, Sets.newHashSet(), Maps.newHashMap());
    }

    private TermSize size(final ITerm term, final java.util.Set<ITermVar> stack,
            final java.util.Map<ITermVar, TermSize> visited) {
        switch(term.termTag()) {
            case IApplTerm: { IApplTerm appl = (IApplTerm) term;
                return TermSize.ONE.add(sizes(appl.getArgs(), stack, visited));
            }

            case IConsTerm:
            case INilTerm:
            case ITermVar: {
                return size((IListTerm) term, stack, visited);
            }

            case IStringTerm:
            case IIntTerm:
            case IBlobTerm: {
                return TermSize.ONE;
            }
        }
        // N.B. don't use this in default case branch, instead use IDE to catch non-exhaustive switch statements
        throw new RuntimeException("Missing case for ITerm subclass/tag");
    }

    private TermSize size(IListTerm list, final java.util.Set<ITermVar> stack,
            final java.util.Map<ITermVar, TermSize> visited) {
        final Ref<TermSize> size = new Ref<>(TermSize.ZERO);
        while(list != null) {
            switch(list.listTermTag()) {
                case IConsTerm: { IConsTerm cons = (IConsTerm) list;
                    size.set(size.get().add(TermSize.ONE)
                        .add(size(cons.getHead(), stack, visited)));
                    list = cons.getTail();
                    continue;
                }

                case INilTerm: {
                    size.set(size.get().add(TermSize.ONE));
                    list = null;
                    continue;
                }

                case ITermVar: { ITermVar var = (ITermVar) list;
                    size.set(size.get().add(size(var, stack, visited)));
                    list = null;
                    continue;
                }
            }
            // N.B. don't use this in default case branch, instead use IDE to catch non-exhaustive switch statements
            throw new RuntimeException("Missing case for IListTerm subclass/tag");
        }
        return size.get();
    }

    private TermSize size(final ITermVar var, final java.util.Set<ITermVar> stack,
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

    private TermSize sizes(final Iterable<ITerm> terms, final java.util.Set<ITermVar> stack,
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

    @Override public String toString(final ITerm term, SpecializedTermFormatter specializedTermFormatter) {
        return toString(term, Maps.newHashMap(), Maps.newHashMap(), -1, specializedTermFormatter);
    }

    @Override public String toString(final ITerm term, int depth, SpecializedTermFormatter specializedTermFormatter) {
        return toString(term, Maps.newHashMap(), Maps.newHashMap(), depth, specializedTermFormatter);
    }

    private String toString(final ITerm term, final java.util.Map<ITermVar, String> stack,
        final java.util.Map<ITermVar, String> visited, final int maxDepth,
        final SpecializedTermFormatter specializedTermFormatter) {
        if(maxDepth == 0) {
            return "…";
        }
        if(term.termTag() == ITerm.Tag.ITermVar) {
            ITermVar var = (ITermVar) term;
            return toString(var, stack, visited, maxDepth,
                specializedTermFormatter);
        }
        final Optional<String> formatted =
            specializedTermFormatter.formatSpecialized(term, this,
                st -> toString(st, stack, visited, maxDepth - 1, specializedTermFormatter));
        switch(term.termTag()) {
            case IApplTerm: {
                IApplTerm appl = (IApplTerm) term;
                return formatted.orElseGet(
                    () -> appl.getOp() + "(" + toStrings(appl.getArgs(), stack, visited,
                        maxDepth - 1, specializedTermFormatter) + ")");
            }

            case IConsTerm:
            case INilTerm: {
                IListTerm list = (IListTerm) term;
                return formatted.orElseGet(
                    () -> toString(list, stack, visited, maxDepth,
                        specializedTermFormatter));
            }

            case IStringTerm:
            case IIntTerm:
            case IBlobTerm: {
                return formatted.orElseGet(term::toString);
            }

            case ITermVar: {
                // impossible branch due to earlier if + return
                break;
            }
        }
        // N.B. don't use this in default case branch, instead use IDE to catch non-exhaustive switch statements
        throw new RuntimeException("Missing case for ITerm subclass/tag");
    }

    private String toString(IListTerm list, final java.util.Map<ITermVar, String> stack,
            final java.util.Map<ITermVar, String> visited, final int maxDepth,
            final SpecializedTermFormatter specializedTermFormatter) {
        if(maxDepth == 0) {
            return "…";
        }
        final StringBuilder sb = new StringBuilder();
        final AtomicBoolean tail = new AtomicBoolean();
        int remaining = maxDepth;
        sb.append("[");
        while(list != null) {
            if(remaining == 0) {
                if(list.listTermTag() != IListTerm.Tag.INilTerm) {
                    sb.append("|…");
                }
                break;
            }
            switch(list.listTermTag()) {
                case IConsTerm: { IConsTerm cons = (IConsTerm) list;
                    if(tail.getAndSet(true)) {
                        sb.append(",");
                    }
                    sb.append(
                        toString(cons.getHead(), stack, visited, maxDepth - 1,
                            specializedTermFormatter));
                    list =  cons.getTail();
                    break;
                }

                case INilTerm: {
                    list = null;
                    break;
                }

                case ITermVar: { ITermVar var = (ITermVar) list;
                    sb.append("|");
                    sb.append(toString(var, stack, visited, maxDepth - 1,
                        specializedTermFormatter));
                    list = null;
                    break;
                }
            }
            remaining--;
        }
        sb.append("]");
        return sb.toString();
    }

    private String toString(final ITermVar var, final java.util.Map<ITermVar, String> stack,
            final java.util.Map<ITermVar, String> visited, final int maxDepth,
            final SpecializedTermFormatter specializedTermFormatter) {
        if(maxDepth == 0) {
            return "…";
        }
        final ITermVar rep = findRep(var);
        final String toString;
        if(!visited.containsKey(rep)) {
            stack.put(rep, null);
            visited.put(rep, null);
            final ITerm term = terms().get(rep);
            if(term != null) {
                final String termString = toString(term, stack, visited, maxDepth, specializedTermFormatter);
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
            final java.util.Map<ITermVar, String> visited, final int maxDepth,
            final SpecializedTermFormatter specializedTermFormatter) {
        final StringBuilder sb = new StringBuilder();
        final AtomicBoolean tail = new AtomicBoolean();
        for(ITerm term : terms) {
            if(tail.getAndSet(true)) {
                sb.append(",");
            }
            sb.append(toString(term, stack, visited, maxDepth, specializedTermFormatter));
        }
        return sb.toString();
    }

    ///////////////////////////////////////////
    // class Result
    ///////////////////////////////////////////

    public static class ImmutableResult<T> implements Result<T> {

        private final T result;
        private final PersistentUnifier.Immutable unifier;

        public ImmutableResult(T result, PersistentUnifier.Immutable unifier) {
            this.result = result;
            this.unifier = unifier;
        }

        @Override public T result() {
            return result;
        }

        @Override public PersistentUnifier.Immutable unifier() {
            return unifier;
        }

    }

    ///////////////////////////////////////////
    // class Transient
    ///////////////////////////////////////////

    protected static class Transient implements IUnifier.Transient {

        private IUnifier.Immutable unifier;

        public Transient(IUnifier.Immutable unifier) {
            this.unifier = unifier;
        }

        @Override public boolean isFinite() {
            return unifier.isFinite();
        }

        @Override public boolean isEmpty() {
            return unifier.isEmpty();
        }

        @Override public boolean contains(ITermVar var) {
            return unifier.contains(var);
        }

        @Override public Set.Immutable<ITermVar> domainSet() {
            return unifier.domainSet();
        }

        @Override public Set.Immutable<ITermVar> rangeSet() {
            return unifier.rangeSet();
        }

        @Override public Set.Immutable<ITermVar> varSet() {
            return unifier.varSet();
        }

        @Override public boolean isCyclic() {
            return unifier.isCyclic();
        }

        @Override public ITermVar findRep(ITermVar var) {
            return unifier.findRep(var);
        }

        @Override public boolean hasTerm(ITermVar var) {
            return unifier.hasTerm(var);
        }

        @Override public ITerm findTerm(ITerm term) {
            return unifier.findTerm(term);
        }

        @Override public ITerm findRecursive(ITerm term) {
            return unifier.findRecursive(term);
        }

        @Override public boolean isGround(ITerm term) {
            return unifier.isGround(term);
        }

        @Override public boolean isCyclic(ITerm term) {
            return unifier.isCyclic(term);
        }

        @Override public Set.Immutable<ITermVar> getVars(ITerm term) {
            return unifier.getVars(term);
        }

        @Override public TermSize size(ITerm term) {
            return unifier.size(term);
        }

        @Override public String toString(ITerm term, SpecializedTermFormatter specializedTermFormatter) {
            return unifier.toString(term, specializedTermFormatter);
        }

        @Override public String toString(ITerm term, int depth, SpecializedTermFormatter specializedTermFormatter) {
            return unifier.toString(term, depth, specializedTermFormatter);
        }

        @Override public Optional<? extends IUnifier.Immutable> unify(ITerm term1, ITerm term2,
                Predicate1<ITermVar> isRigid) throws OccursException, RigidException {
            final Optional<? extends Result<? extends Immutable>> result = unifier.unify(term1, term2, isRigid);
            return result.map(r -> {
                unifier = r.unifier();
                return r.result();
            });
        }

        @Override public Optional<? extends IUnifier.Immutable> unify(IUnifier other, Predicate1<ITermVar> isRigid)
                throws OccursException, RigidException {
            final Optional<? extends Result<? extends Immutable>> result = unifier.unify(other, isRigid);
            return result.map(r -> {
                unifier = r.unifier();
                return r.result();
            });
        }

        @Override public Optional<? extends IUnifier.Immutable> unify(
                Iterable<? extends Entry<? extends ITerm, ? extends ITerm>> equalities, Predicate1<ITermVar> isRigid)
                throws OccursException, RigidException {
            final Optional<? extends Result<? extends Immutable>> result = unifier.unify(equalities, isRigid);
            return result.map(r -> {
                unifier = r.unifier();
                return r.result();
            });
        }

        @Override public Optional<? extends IUnifier.Immutable> diff(ITerm term1, ITerm term2) {
            return unifier.diff(term1, term2);
        }

        @Override public boolean equal(ITerm term1, ITerm term2) {
            return unifier.equal(term1, term2);
        }

        @Override public ISubstitution.Immutable retain(ITermVar var) {
            final Result<mb.nabl2.terms.substitution.ISubstitution.Immutable> result = unifier.retain(var);
            unifier = result.unifier();
            return result.result();
        }

        @Override public ISubstitution.Immutable retainAll(Iterable<ITermVar> vars) {
            final Result<mb.nabl2.terms.substitution.ISubstitution.Immutable> result = unifier.retainAll(vars);
            unifier = result.unifier();
            return result.result();
        }

        @Override public ISubstitution.Immutable remove(ITermVar var) {
            final Result<mb.nabl2.terms.substitution.ISubstitution.Immutable> result = unifier.remove(var);
            unifier = result.unifier();
            return result.result();
        }

        @Override public ISubstitution.Immutable removeAll(Iterable<ITermVar> vars) {
            final Result<mb.nabl2.terms.substitution.ISubstitution.Immutable> result = unifier.removeAll(vars);
            unifier = result.unifier();
            return result.result();
        }

        @Override public IUnifier.Immutable freeze() {
            return unifier;
        }

        @Override public String toString() {
            return unifier.toString();
        }

    }

}