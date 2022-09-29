package mb.nabl2.terms.substitution;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;

public class Renaming implements IRenaming {

    private final BiMap<ITermVar, ITermVar> renaming;

    private Renaming(BiMap<ITermVar, ITermVar> renaming) {
        this.renaming = renaming;
    }

    @Override public boolean isEmpty() {
        return renaming.isEmpty();
    }

    @Override public Set<ITermVar> keySet() {
        return renaming.keySet();
    }

    @Override public Set<ITermVar> valueSet() {
        return renaming.values();
    }

    @Override public Set<? extends Entry<ITermVar, ITermVar>> entrySet() {
        return renaming.entrySet();
    }

    @Override public ITermVar rename(ITermVar var) {
        return renaming.getOrDefault(var, var);
    }

    @Override public ITerm apply(ITerm term) {
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
            var -> rename(var),
            other -> other
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

    @Override public Map<ITermVar, ITermVar> asMap() {
        return Collections.unmodifiableMap(renaming);
    }

    @Override public ISubstitution.Immutable asSubstitution() {
        return PersistentSubstitution.Immutable.of(renaming);
    }

    @Override public String toString() {
        return renaming.entrySet().stream().map(e -> e.getKey() + " |-> " + e.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final BiMap<ITermVar, ITermVar> renaming;

        private Builder() {
            this.renaming = HashBiMap.create();
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
            return new Renaming(renaming);
        }

    }

}
