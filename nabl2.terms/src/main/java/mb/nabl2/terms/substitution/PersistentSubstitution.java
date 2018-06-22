package mb.nabl2.terms.substitution;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.io.Serializable;
import java.util.stream.Collectors;

import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import io.usethesource.capsule.Map;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;

public abstract class PersistentSubstitution implements ISubstitution {

    protected abstract Map<ITermVar, ITerm> subst();

    @Override public boolean isEmpty() {
        return subst().isEmpty();
    }

    @Override public boolean contains(ITermVar var) {
        return subst().containsKey(var);
    }

    @Override public ITerm apply(ITerm term) {
        // @formatter:off
        return term.match(Terms.cases(
            appl -> B.newAppl(appl.getOp(), appl.getArgs().stream().map(this::apply).collect(Collectors.toList()), appl.getAttachments()),
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

    @Override public boolean isRenaming() {
        final Multiset<ITermVar> vars = HashMultiset.create();
        if(!subst().values().stream().allMatch(t -> M.var(v -> {
            vars.add(v);
            return true;
        }).match(t).orElse(false))) {
            return false;
        }
        if(!vars.entrySet().stream().allMatch(e -> e.getCount() <= 1)) {
            return false;
        }
        if(!subst().keySet().equals(vars.elementSet())) {
            return false;
        }
        return true;
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

        public ISubstitution.Transient melt() {
            return new PersistentSubstitution.Transient(subst.asTransient());
        }

        public static ISubstitution.Immutable of() {
            return new PersistentSubstitution.Immutable(Map.Immutable.of());
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

        public ISubstitution.Immutable freeze() {
            return new PersistentSubstitution.Immutable(subst.freeze());
        }

        public static ISubstitution.Transient of() {
            return new PersistentSubstitution.Transient(Map.Transient.of());
        }

    }

}