package mb.nabl2.terms.substitution;

import static mb.nabl2.terms.build.TermBuild.B;

import java.io.Serializable;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import io.usethesource.capsule.Map;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import mb.nabl2.util.CapsuleUtil;

public abstract class PersistentRenaming implements IRenaming {

    protected abstract Map<ITermVar, ITermVar> subst();

    @Override public boolean isEmpty() {
        return subst().isEmpty();
    }

    @Override public boolean contains(ITermVar var) {
        return subst().containsKey(var);
    }

    @Override public Set<ITermVar> varSet() {
        return subst().keySet();
    }

    @Override public Set<ITermVar> freeVarSet() {
        return ImmutableSet.copyOf(subst().values());
    }

    @Override public Set<Entry<ITermVar, ITermVar>> entrySet() {
        return subst().entrySet();
    }

    @Override public ITerm apply(ITerm term) {
        // @formatter:off
        return term.match(Terms.cases(
            appl -> {
                final List<ITerm> args = appl.getArgs().stream().map(this::apply).collect(ImmutableList.toImmutableList());
                return B.newAppl(appl.getOp(), args, appl.getAttachments());
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
        // @formatter:off
        return list.<IListTerm>match(ListTerms.cases(
            cons -> B.newCons(apply(cons.getHead()), apply(cons.getTail()), cons.getAttachments()),
            nil -> nil,
            var -> (IListTerm) apply(var)
        ));
        // @formatter:on
    }

    @Override public ITermVar apply(ITermVar var) {
        return subst().getOrDefault(var, var);
    }

    public static class Immutable extends PersistentRenaming implements IRenaming.Immutable, Serializable {

        private static final long serialVersionUID = 1L;

        private Map.Immutable<ITermVar, ITermVar> subst;

        public Immutable(Map.Immutable<ITermVar, ITermVar> sub) {
            this.subst = sub;
        }

        @Override protected Map<ITermVar, ITermVar> subst() {
            return subst;
        }

        @Override public IRenaming.Immutable put(ITermVar var, ITermVar term) {
            return new PersistentRenaming.Immutable(subst.__put(var, term));
        }

        @Override public ISubstitution.Immutable put(ITermVar var, ITerm term) {
            return PersistentSubstitution.Immutable.of(subst).put(var, term);
        }

        @Override public IRenaming.Immutable remove(ITermVar var) {
            return new PersistentRenaming.Immutable(subst.__remove(var));
        }

        @Override public IRenaming.Immutable removeAll(Iterable<ITermVar> vars) {
            final Map.Transient<ITermVar, ITermVar> subst = this.subst.asTransient();
            Iterables2.stream(vars).forEach(subst::__remove);
            return new PersistentRenaming.Immutable(subst.freeze());
        }

        @Override public IRenaming.Immutable compose(IRenaming.Immutable other) {
            final Map.Transient<ITermVar, ITermVar> subst = this.subst.asTransient();
            CapsuleUtil.updateValues(subst, (v, t) -> other.apply(t));
            other.removeAll(subst.keySet()).entrySet().forEach(e -> subst.__put(e.getKey(), e.getValue()));
            return new PersistentRenaming.Immutable(subst.freeze());
        }

        @Override public ISubstitution.Immutable compose(ISubstitution.Immutable other) {
            final Map.Transient<ITermVar, ITerm> subst = Map.Transient.of();
            subst.__putAll(this.subst);
            CapsuleUtil.updateValues(subst, (v, t) -> other.apply(t));
            other.removeAll(subst.keySet()).entrySet().forEach(e -> subst.__put(e.getKey(), e.getValue()));
            return new PersistentSubstitution.Immutable(subst.freeze());
        }

        @Override public IRenaming.Transient melt() {
            return new PersistentRenaming.Transient(subst.asTransient());
        }

        public static IRenaming.Immutable of() {
            return new PersistentRenaming.Immutable(Map.Immutable.of());
        }

        public static IRenaming.Immutable of(ITermVar var, ITermVar term) {
            return new PersistentRenaming.Immutable(Map.Immutable.of(var, term));
        }

        public static IRenaming.Immutable of(java.util.Map<ITermVar, ? extends ITermVar> subst) {
            return new PersistentRenaming.Immutable(Map.Immutable.<ITermVar, ITermVar>of().__putAll(subst));
        }

    }

    public static class Transient extends PersistentRenaming implements IRenaming.Transient {

        private Map.Transient<ITermVar, ITermVar> subst;

        public Transient(Map.Transient<ITermVar, ITermVar> subst) {
            this.subst = subst;
        }

        @Override protected Map<ITermVar, ITermVar> subst() {
            return subst;
        }

        @Override public void put(ITermVar var, ITermVar term) {
            subst.__put(var, term);
        }

        @Override public void remove(ITermVar var) {
            subst.__remove(var);
        }

        @Override public void removeAll(Iterable<ITermVar> vars) {
            Iterables2.stream(vars).forEach(subst::remove);
        }

        @Override public void compose(IRenaming.Immutable other) {
            CapsuleUtil.updateValues(subst, (v, t) -> other.apply(t));
            other.removeAll(subst.keySet()).entrySet().forEach(e -> subst.__put(e.getKey(), e.getValue()));
        }

        @Override public IRenaming.Immutable freeze() {
            return new PersistentRenaming.Immutable(subst.freeze());
        }

        public static IRenaming.Transient of() {
            return new PersistentRenaming.Transient(Map.Transient.of());
        }

    }

    @Override public String toString() {
        return subst().toString();
    }

}