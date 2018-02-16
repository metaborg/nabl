package org.metaborg.meta.nabl2.constraints.namebinding;

import static org.metaborg.meta.nabl2.terms.build.TermBuild.B;

import org.metaborg.meta.nabl2.terms.ITerm;

public final class DeclProperties {

    public static final ITerm TYPE_KEY = B.newAppl("Type");

    public static final ITerm key(String name) {
        return B.newAppl("Property", B.newString(name));
    }

}