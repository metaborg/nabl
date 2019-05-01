package mb.nabl2.terms.substitution;

import static mb.nabl2.terms.build.TermBuild.B;

import java.io.Serializable;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.ImmutableList;

import io.usethesource.capsule.Map;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import mb.nabl2.util.CapsuleUtil;

public abstract class PersistentSubstitution implements ISubstitution {

    protected abstract Map<ITermVar, ITerm> subst();

    @Override public boolean isEmpty() {
        return subst().isEmpty();
    }

    @Override public boolean contains(ITermVar var) {
        return subst().containsKey(var);
    }

    @Override public Set<ITermVar> varSet() {
        return subst().keySet();
    }

    @Override public Set<Entry<ITermVar, ITerm>> entrySet() {
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

    private ITerm apply(ITermVar var) {
        return subst().getOrDefault(var, var);
    }

    public static class Immutable extends PersistentSubstitution implements ISubstitution.Immutable, Serializable {

        private static final long serialVersionUID = 1L;

        private Map.Immutable<ITermVar, ITerm> subst;

        public Immutable(Map.Immutable<ITermVar, ITerm> sub) {
            this.subst = sub;
        }

        @Override protected Map<ITermVar, ITerm> subst() {
            return subst;
        }

        @Override public ISubstitution.Immutable put(ITermVar var, ITerm term) {
            return new PersistentSubstitution.Immutable(subst.__put(var, term));
        }

        @Override public ISubstitution.Immutable remove(ITermVar var) {
            return new PersistentSubstitution.Immutable(subst.__remove(var));
        }

        @Override public ISubstitution.Immutable removeAll(Iterable<ITermVar> vars) {
            final Map.Transient<ITermVar, ITerm> subst = this.subst.asTransient();
            Iterables2.stream(vars).forEach(subst::__remove);
            return new PersistentSubstitution.Immutable(subst.freeze());
        }

        @Override public ISubstitution.Immutable compose(ISubstitution.Immutable other) {
            final Map.Transient<ITermVar, ITerm> subst = this.subst.asTransient();
            CapsuleUtil.replace(subst, (v, t) -> other.apply(t));
            other.removeAll(subst.keySet()).entrySet().forEach(e -> subst.__put(e.getKey(), e.getValue()));
            return new PersistentSubstitution.Immutable(subst.freeze());
        }

        @Override public ISubstitution.Transient melt() {
            return new PersistentSubstitution.Transient(subst.asTransient());
        }

        public static ISubstitution.Immutable of() {
            return new PersistentSubstitution.Immutable(Map.Immutable.of());
        }

        public static ISubstitution.Immutable of(ITermVar var, ITerm term) {
            return new PersistentSubstitution.Immutable(Map.Immutable.of(var, term));
        }

    }

    public static class Transient extends PersistentSubstitution implements ISubstitution.Transient {

        private Map.Transient<ITermVar, ITerm> subst;

        public Transient(Map.Transient<ITermVar, ITerm> subst) {
            this.subst = subst;
        }

        @Override protected Map<ITermVar, ITerm> subst() {
            return subst;
        }

        @Override public void put(ITermVar var, ITerm term) {
            subst.__put(var, term);
        }

        @Override public void remove(ITermVar var) {
            subst.__remove(var);
        }

        @Override public void removeAll(Iterable<ITermVar> vars) {
            Iterables2.stream(vars).forEach(subst::remove);
        }

        @Override public void compose(ISubstitution.Immutable other) {
            CapsuleUtil.replace(subst, (v, t) -> other.apply(t));
            other.removeAll(subst.keySet()).entrySet().forEach(e -> subst.__put(e.getKey(), e.getValue()));
        }

        @Override public ISubstitution.Immutable freeze() {
            return new PersistentSubstitution.Immutable(subst.freeze());
        }

        public static ISubstitution.Transient of() {
            return new PersistentSubstitution.Transient(Map.Transient.of());
        }

    }

    @Override public String toString() {
        return subst().toString();
    }

}