package meta.flowspec.java.interpreter;

import org.metaborg.meta.nabl2.controlflow.ICFGNode;
import org.metaborg.meta.nabl2.controlflow.impl.ControlFlowGraph;

public class IdentityTFAppl<S extends ICFGNode> extends TransferFunctionAppl {
    private final String prop;
    private final ControlFlowGraph<S> cfg;

    public IdentityTFAppl(ControlFlowGraph<S> cfg, String prop) {
        super(-1, null);
        this.prop = prop;
        this.cfg = cfg;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object call(TransferFunction[] _tfs, Object _arg) {
        return cfg.getProperty((S) _arg, prop);
    }
}