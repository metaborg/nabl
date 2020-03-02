package mb.nabl2.terms.unification;

import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.terms.unification.ud.PersistentUniDisunifier;

public class Unifiers {

    public static class Immutable {

        public static IUniDisunifier.Immutable of() {
            return PersistentUniDisunifier.Immutable.of();
//            return IncrementalVarSetPersistentUnifier.Immutable.of();
        }

        public static IUniDisunifier.Immutable of(boolean finite) {
            return PersistentUniDisunifier.Immutable.of(finite);
//            return IncrementalVarSetPersistentUnifier.Immutable.of(finite);
        }

    }

}