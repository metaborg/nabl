package mb.nabl2.terms.substitution;

import static mb.nabl2.terms.build.TermBuild.B;

import java.io.Serializable;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.ImmutableList;

import io.usethesource.capsule.Map;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import mb.nabl2.util.CapsuleUtil;
import mb.nabl2.util.collections.MultiSet;

public abstract class PersistentSubstitution implements ISubstitution {

    protected abstract Map<ITermVar, ITerm> subst();

    protected abstract MultiSet<ITermVar> range();

    @Override public boolean isEmpty() {
        return subst().isEmpty();
    }

    @Override public boolean contains(ITermVar var) {
        return subst().containsKey(var);
    }

    @Override public Set<ITermVar> domainSet() {
        return subst().keySet();
    }

    @Override public Set<ITermVar> rangeSet() {
        return range().elementSet();
    }

    @Override public Set<Entry<ITermVar, ITerm>> entrySet() {
        return subst().entrySet();
    }

    @Override public ITerm apply(ITerm term) {
        if(term.isGround()) {
            return term;
        }
        // @formatter:off
        return term.match(Terms.cases(
            appl -> {
                final ImmutableList<ITerm> newArgs;
                if((newArgs = Terms.applyLazy(appl.getArgs(), this::apply)) == null) {
                    return appl;
                }
                return B.newAppl(appl.getOp(), newArgs, appl.getAttachments());
            },
            list -> apply(list),
            string -> string,
            integer -> integer,
            blob -> blob,
            var -> apply(var)
        ));
        // @formatter:on
    }

    private IListTerm apply(IListTerm list) {
        if(list.isGround()) {
            return list;
        }
        // @formatter:off
        return list.<IListTerm>match(ListTerms.cases(
            cons -> {
                final ITerm newHead = apply(cons.getHead());
                final IListTerm newTail = apply(cons.getTail());
                if(newHead == cons.getHead() && newTail == cons.getTail()) {
                    return cons;
                }
                return B.newCons(newHead, newTail, cons.getAttachments());
            },
            nil -> nil,
            var -> (IListTerm) apply(var)
        ));
        // @formatter:on
    }

    private ITerm apply(ITermVar var) {
        return subst().getOrDefault(var, var);
    }

    public static class Immutable extends PersistentSubstitution implements ISubstitution.Immutable, Serializable {

        private static final long serialVersionUID = 1L;

        private Map.Immutable<ITermVar, ITerm> subst;
        private MultiSet.Immutable<ITermVar> range;

        public Immutable(Map.Immutable<ITermVar, ITerm> subst, MultiSet.Immutable<ITermVar> range) {
            this.subst = subst;
            this.range = range;
        }

        @Override protected Map<ITermVar, ITerm> subst() {
            return subst;
        }

        @Override protected MultiSet<ITermVar> range() {
            return range;
        }

        @Override public ISubstitution.Immutable put(ITermVar var, ITerm term) {
            final ISubstitution.Transient result = melt();
            result.put(var, term);
            return result.freeze();
        }

        @Override public ISubstitution.Immutable remove(ITermVar var) {
            final ISubstitution.Transient result = melt();
            result.remove(var);
            return result.freeze();
        }

        @Override public ISubstitution.Immutable removeAll(Iterable<ITermVar> vars) {
            final ISubstitution.Transient result = melt();
            result.removeAll(vars);
            return result.freeze();
        }

        @Override public ISubstitution.Immutable retainAll(Iterable<ITermVar> vars) {
            final ISubstitution.Transient result = melt();
            result.retainAll(vars);
            return result.freeze();
        }

        @Override public ISubstitution.Immutable compose(ISubstitution.Immutable other) {
            final ISubstitution.Transient result = melt();
            result.compose(other);
            return result.freeze();
        }

        @Override public ISubstitution.Immutable compose(ITermVar var, ITerm term) {
            final ISubstitution.Transient result = melt();
            result.compose(var, term);
            return result.freeze();
        }

        @Override public ISubstitution.Transient melt() {
            return new PersistentSubstitution.Transient(subst.asTransient(), range.melt());
        }

        public static ISubstitution.Immutable of() {
            return new PersistentSubstitution.Immutable(Map.Immutable.of(), MultiSet.Immutable.of());
        }

        public static ISubstitution.Immutable of(ITermVar var, ITerm term) {
            return new PersistentSubstitution.Immutable(Map.Immutable.of(var, term),
                    MultiSet.Immutable.of(term.getVars()));
        }

        public static ISubstitution.Immutable of(java.util.Map<ITermVar, ? extends ITerm> substEntries) {
            final Map.Transient<ITermVar, ITerm> subst = Map.Transient.of();
            final MultiSet.Transient<ITermVar> range = MultiSet.Transient.of();
            substEntries.forEach((v, t) -> {
                subst.__put(v, t);
                range.addAll(t.getVars());
            });
            return new PersistentSubstitution.Immutable(subst.freeze(), range.freeze());
        }

    }

    public static class Transient extends PersistentSubstitution implements ISubstitution.Transient {

        private Map.Transient<ITermVar, ITerm> subst;
        private MultiSet.Transient<ITermVar> range;

        public Transient(Map.Transient<ITermVar, ITerm> subst, MultiSet.Transient<ITermVar> range) {
            this.subst = subst;
            this.range = range;
        }

        @Override protected Map<ITermVar, ITerm> subst() {
            return subst;
        }

        @Override protected MultiSet<ITermVar> range() {
            return range;
        }

        @Override public void put(ITermVar var, ITerm term) {
            subst.__put(var, term);
            range.addAll(term.getVars());
        }

        @Override public void remove(ITermVar var) {
            ITerm t = subst.__remove(var);
            if(t != null) {
                range.removeAll(t.getVars());
            }
        }

        @Override public void removeAll(Iterable<ITermVar> vars) {
            for(ITermVar v : vars) {
                remove(v);
            }
        }

        @Override public void retainAll(Iterable<ITermVar> vars) {
            io.usethesource.capsule.Set.Immutable<ITermVar> varSet = CapsuleUtil.toSet(vars);
            for(ITermVar v : subst.keySet()) {
                if(!varSet.contains(v)) {
                    remove(v);
                }
            }
        }

        @Override public void compose(ISubstitution.Immutable other) {
            subst.forEach((v, t) -> {
                range.removeAll(t.getVars());
                t = other.apply(t);
                subst.__put(v, t);
                range.addAll(t.getVars());
            });
            for(Entry<ITermVar, ITerm> e : other.removeAll(subst.keySet()).entrySet()) {
                final ITerm t = e.getValue();
                subst.__put(e.getKey(), t);
                range.addAll(t.getVars());
            }
        }

        @Override public void compose(ITermVar var, ITerm term) {
            compose(PersistentSubstitution.Immutable.of(var, term));
        }

        @Override public ISubstitution.Immutable freeze() {
            return new PersistentSubstitution.Immutable(subst.freeze(), range.freeze());
        }

        public static ISubstitution.Transient of() {
            return new PersistentSubstitution.Transient(Map.Transient.of(), MultiSet.Transient.of());
        }

    }

    @Override public String toString() {
        return subst().toString();
    }

}