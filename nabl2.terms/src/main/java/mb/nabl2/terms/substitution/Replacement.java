package mb.nabl2.terms.substitution;

import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.util.functions.Function1;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.IBlobTerm;
import mb.nabl2.terms.IConsTerm;
import mb.nabl2.terms.IIntTerm;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.INilTerm;
import mb.nabl2.terms.IStringTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.Terms;

import static mb.nabl2.terms.build.TermBuild.B;

public class Replacement implements IReplacement {

    private final BiMap<ITerm, ITerm> replacement;
    private final boolean traverseSubTerms;

    private Replacement(BiMap<ITerm, ITerm> replacement, boolean traverseSubTerms) {
        this.replacement = replacement;
        this.traverseSubTerms = traverseSubTerms;
    }

    @Override public boolean isEmpty() {
        return replacement.isEmpty();
    }

    @Override public Set<ITerm> keySet() {
        return replacement.keySet();
    }

    @Override public Set<ITerm> valueSet() {
        return replacement.values();
    }

    @Override public Set<? extends Entry<ITerm, ITerm>> entrySet() {
        return replacement.entrySet();
    }

    @Override public ITerm replace(ITerm term) {
        // TODO: require term to be ground????
        // TODO: disable subterm inclusion?
        return replacement.getOrDefault(term, term);
    }

    @Override public ITerm apply(ITerm term) {
        // Cannot happen
        switch(term.termTag()) {
            case IApplTerm: {
                final IApplTerm newAppl = (IApplTerm) replace(term);
                if(!traverseSubTerms) {
                    return newAppl;
                }
                final ImmutableList<ITerm> newArgs;
                if((newArgs = Terms.applyLazy(newAppl.getArgs(), this::apply))
                    == null) {
                    return newAppl;
                }
                return B.newAppl(newAppl.getOp(), newArgs, term.getAttachments());
            }

            case IConsTerm:
            case INilTerm: {
                return apply((IListTerm) term);
            }

            case IStringTerm:
            case IIntTerm:
            case IBlobTerm: {
                return replace(term);
            }

            case ITermVar: {
                return term;
            }
        }
        // N.B. don't use this in default case branch, instead use IDE to catch non-exhaustive switch statements
        throw new RuntimeException("Missing case for ITerm subclass/tag");
    }

    private IListTerm apply(IListTerm list) {
        final IListTerm newList = (IListTerm) replace(list);
        if(!traverseSubTerms) {
            return newList;
        }
        // Cannot happen
        if(newList.listTermTag() == IListTerm.Tag.IConsTerm) {
            IConsTerm cons = (IConsTerm) newList;
            final ITerm newHead = apply(cons.getHead());
            final IListTerm newTail = apply(cons.getTail());

            if(newHead != cons.getHead() || newTail != cons.getTail()) {
                B.newCons(newHead, newTail, cons.getAttachments());
            }
            return cons;
        } else {
            return newList;
        }
    }


    @Override public String toString() {
        return replacement.entrySet().stream().map(e -> e.getKey() + " |-> " + e.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private boolean traverseSubTerms = false;
        private final BiMap<ITerm, ITerm> replacement;

        private Builder() {
            this.replacement = HashBiMap.create();
        }

        public boolean containsKey(ITermVar var) {
            return replacement.containsKey(var);
        }

        public boolean containsValue(ITermVar var) {
            return replacement.containsValue(var);
        }

        public Builder put(boolean traverseSubTerms) {
            this.traverseSubTerms = traverseSubTerms;
            return this;
        }

        public Builder put(ITerm v1, ITerm v2) {
            if(!v1.isGround() || !v2.isGround()) {
                throw new IllegalStateException("Use unification or renaming to change unification variables.");
            }
            if(!v1.equals(v2)) {
                replacement.put(v1, v2);
            }
            return this;
        }

        public Replacement build() {
            return new Replacement(replacement, traverseSubTerms);
        }

    }

}
