package org.metaborg.meta.nabl2.controlflow.terms;

import java.util.Arrays;


public class TransferFunctionAppl {
    public final int tfOffset;
    public final Object[] args;

    public TransferFunctionAppl(int tf, Object[] args) {
        this.tfOffset = tf;
        this.args = Arrays.copyOf(args, args.length+1);
    }
    
    @Override
    public String toString() {
        return "(" + tfOffset + ", " + args.toString() + ")";
    }
}
