package org.metaborg.meta.nabl2.controlflow.terms;

import static org.metaborg.meta.nabl2.terms.matching.TermMatch.M;

import java.util.Arrays;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.build.ListTermIterator;
import org.metaborg.meta.nabl2.terms.matching.TermMatch.IMatcher;

import com.google.common.collect.Iterators;


public class TransferFunctionAppl {
    public final int tfOffset;
    public final Object[] args;

    public TransferFunctionAppl(int tf, Object[] args) {
        this.tfOffset = tf;
        this.args = Arrays.copyOf(args, args.length+1);
        for(int i = args.length; i > 0; i--) {
            this.args[i] = this.args[i-1];
        }
    }
    
    @Override
    public String toString() {
        return "(" + tfOffset + ", " + args.toString() + ")";
    }
    
    public static IMatcher<TransferFunctionAppl> match() {
        return M.appl2("", M.integer(), M.list(), (applTerm, intTerm, argsTerm) -> {
            return new TransferFunctionAppl(intTerm.getValue(), Iterators.toArray(new ListTermIterator(argsTerm), ITerm.class));
        });
    }
}
