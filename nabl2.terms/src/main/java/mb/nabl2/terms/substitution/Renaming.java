package mb.nabl2.terms.substitution;

import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.util.collection.BiMap;
import org.metaborg.util.collection.ImList;

import io.usethesource.capsule.Map;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;

import static mb.nabl2.terms.build.TermBuild.B;

public class Renaming implements IRenaming {

    private final BiMap.Immutable<ITermVar> renaming;

    private Renaming(BiMap.Immutable<ITermVar> renaming) {
        this.renaming = renaming;
    }

    @Override public boolean isEmpty() {
        return renaming.isEmpty();
    }

    @Override public Set<ITermVar> keySet() {
        return renaming.keySet();
    }

    @Override public Set<ITermVar> valueSet() {
        return renaming.valueSet();
    }

    @Override public Set<? extends Entry<ITermVar, ITermVar>> entrySet() {
        return renaming.entrySet();
    }

    @Override public ITermVar rename(ITermVar var) {
        return renaming.getKeyOrDefault(var, var);
    }

    @Override public ITerm apply(ITerm term) {
        // @formatter:off
        return term.match(Terms.cases(
            appl -> {
                final ImList.Immutable<ITerm> newArgs;
                if((newArgs = Terms.applyLazy(appl.getArgs(), this::apply)) == null) {
                    return appl;
                }
                return B.newAppl(appl.getOp(), newArgs, appl.getAttachments());
            },
            list -> apply(list),
            string -> string,
            integer -> integer,
            blob -> blob,
            var -> rename(var)
        ));
        // @formatter:on
    }

    private IListTerm apply(IListTerm list) {
        // @formatter:off
        return list.<IListTerm>match(ListTerms.cases(
            cons -> B.newCons(apply(cons.getHead()), apply(cons.getTail()), cons.getAttachments()),
            nil -> nil,
            var -> (IListTerm) rename(var)
        ));
        // @formatter:on
    }

    @Override public Map.Immutable<ITermVar, ITermVar> asMap() {
        return renaming.asMap();
    }

    @Override public ISubstitution.Immutable asSubstitution() {
        return PersistentSubstitution.Immutable.of(renaming.asMap());
    }

    @Override public String toString() {
        return renaming.entrySet().stream().map(e -> e.getKey() + " |-> " + e.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final BiMap.Transient<ITermVar> renaming;

        private Builder() {
            this.renaming = BiMap.Transient.of();
        }

        public boolean containsKey(ITermVar var) {
            return renaming.containsKey(var);
        }

        public boolean containsValue(ITermVar var) {
            return renaming.containsValue(var);
        }

        public Builder put(ITermVar v1, ITermVar v2) {
            if(!v1.equals(v2)) {
                renaming.put(v1, v2);
            }
            return this;
        }

        public Renaming build() {
            return new Renaming(renaming.freeze());
        }

    }

}