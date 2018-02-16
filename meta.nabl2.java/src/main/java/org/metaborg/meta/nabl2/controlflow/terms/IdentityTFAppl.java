package org.metaborg.meta.nabl2.controlflow.terms;

import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;

import io.usethesource.capsule.Map;

public class IdentityTFAppl<S extends ICFGNode> extends TransferFunctionAppl {
    public final String prop;
    public final Map<Tuple2<TermIndex, String>, ITerm> properties;

    public IdentityTFAppl(Map<Tuple2<TermIndex, String>, ITerm> properties, String prop) {
        super(0, new Object[] {});
        this.prop = prop;
        this.properties = properties;
    }
}