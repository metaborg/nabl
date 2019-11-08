package mb.nabl2.terms.unification;

public class Unifiers {

    public static class Immutable {

        public static IUnifier.Immutable of() {
            return PersistentUnifier.Immutable.of();
//            return IncrementalVarSetPersistentUnifier.Immutable.of();
        }

        public static IUnifier.Immutable of(boolean finite) {
            return PersistentUnifier.Immutable.of(finite);
//            return IncrementalVarSetPersistentUnifier.Immutable.of(finite);
        }

    }

}