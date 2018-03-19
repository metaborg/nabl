package mb.nabl2.constraints.ast;

import static mb.nabl2.terms.build.TermBuild.B;

import mb.nabl2.terms.ITerm;

public final class AstProperties {

    public static final ITerm TYPE_KEY = B.newAppl("Type");
    public static final ITerm PARAMS_KEY = B.newAppl("Params");

    public static final ITerm key(String name) {
        return B.newAppl("Property", B.newString(name));
    }

}