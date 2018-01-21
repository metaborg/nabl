package org.metaborg.meta.nabl2.constraints.ast;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.TB;

public final class AstProperties {

    public static final ITerm TYPE_KEY = TB.newAppl("Type");
    public static final ITerm PARAMS_KEY = TB.newAppl("Params");

    public static final ITerm key(String name) {
        return TB.newAppl("Property", TB.newString(name));
    }

}