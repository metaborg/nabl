package org.metaborg.meta.nabl2.controlflow.terms;

public class IdentityTFAppl<S extends ICFGNode> extends TransferFunctionAppl {
    public final String prop;
    public final IControlFlowGraph<S> cfg;

    public IdentityTFAppl(IControlFlowGraph<S> cfg, String prop) {
        super(0, new Object[] {});
        this.prop = prop;
        this.cfg = cfg;
    }
}