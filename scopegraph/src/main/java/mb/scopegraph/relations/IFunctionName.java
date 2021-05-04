package mb.scopegraph.relations;

import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.Function1;

public interface IFunctionName {

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

                @Override public T caseNamed(String name) {
                    return onNamed.apply(name);
                }

                @Override public T caseExt(String name) {
                    return onExt.apply(name);
                }

            };
        }

    }

    <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E;

    interface CheckedCases<T, E extends Throwable> {

        T caseNamed(String name) throws E;

        T caseExt(String name) throws E;

        static <T, E extends Throwable> CheckedCases<T, E> of(
        // @formatter:off
            CheckedFunction1<String, T, E> onNamed,
            CheckedFunction1<String, T, E> onExt
            // @formatter:on
        ) {
            return new CheckedCases<T, E>() {

                @Override public T caseNamed(String name) throws E {
                    return onNamed.apply(name);
                }

                @Override public T caseExt(String name) throws E {
                    return onExt.apply(name);
                }

            };
        }

    }

}