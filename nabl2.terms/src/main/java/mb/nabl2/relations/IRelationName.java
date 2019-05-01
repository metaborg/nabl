package mb.nabl2.relations;

import org.metaborg.util.functions.Function1;

public interface IRelationName {

    <T> T match(Cases<T> cases);

    interface Cases<T> {

        T caseNamed(String name);

        T caseExt(String name);

        static <T> Cases<T> of(
            // @formatter:off
            Function1<String,T> onNamed,
            Function1<String,T> onExt
            // @formatter:on
        ) {
            return new Cases<T>() {

                @Override
                public T caseNamed(String name) {
                    return onNamed.apply(name);
                }

                @Override
                public T caseExt(String name) {
                    return onExt.apply(name);
                }

            };
        }

    }

}