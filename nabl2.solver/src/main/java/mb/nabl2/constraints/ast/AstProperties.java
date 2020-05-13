package mb.nabl2.constraints.ast;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.stream.Collectors;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.util.collections.IProperties;

public final class AstProperties {

    public static final ITerm TYPE_KEY = B.newAppl("Type");
    public static final ITerm PARAMS_KEY = B.newAppl("Params");

    public static final ITerm key(String name) {
        return B.newAppl("Property", B.newString(name));
    }

    public static ITerm build(IProperties<TermIndex, ITerm, ITerm> termProperties, IUnifier unifier) {
        final List<ITerm> props = termProperties.stream()
                .map(entry -> B.newTuple(entry._1(), entry._2(), unifier.findRecursive(entry._3())))
                .collect(Collectors.toList());
        return B.newAppl("AstProperties", B.newList(props));
    }

}