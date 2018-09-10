package mb.nabl2.scopegraph.path;

import org.metaborg.util.functions.Function3;
import org.metaborg.util.functions.Function4;

import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;

public interface IStep<S extends IScope, L extends ILabel, O extends IOccurrence> extends IScopePath<S, L, O> {

    L getLabel();

    <T> T match(ICases<S, L, O, T> cases);

    interface ICases<S extends IScope, L extends ILabel, O extends IOccurrence, T> {

        T caseE(S source, L label, S target);

        T caseN(S source, L label, IResolutionPath<S, L, O> importPath, S target);

        static <S extends IScope, L extends ILabel, O extends IOccurrence, T> ICases<S, L, O, T>
                of(Function3<S, L, S, T> onE, Function4<S, L, IResolutionPath<S, L, O>, S, T> onN) {
            return new ICases<S, L, O, T>() {

                @Override public T caseE(S source, L label, S target) {
                    return onE.apply(source, label, target);
                }

                @Override public T caseN(S source, L label, IResolutionPath<S, L, O> importPath, S target) {
                    return onN.apply(source, label, importPath, target);
                }

            };
        }

    }

}