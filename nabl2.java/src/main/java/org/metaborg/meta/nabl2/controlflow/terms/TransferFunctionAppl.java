package org.metaborg.meta.nabl2.controlflow.terms;

import static org.metaborg.meta.nabl2.terms.matching.TermMatch.M;

import java.util.List;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.matching.TermMatch.IMatcher;

@Immutable
public abstract class TransferFunctionAppl {
    @Parameter public abstract int offset();
    @Parameter protected abstract List<ITerm> otherArgs();

    public ITerm[] args(ITerm firstArg) {
        List<ITerm> otherArgs = otherArgs();
        ITerm[] args = new ITerm[otherArgs.size()+1];
        args[0] = firstArg;
        int i = 1;
        for(ITerm arg : otherArgs) {
            args[i] = arg;
            i++;
        }
        return args;
    }

    @Override
    public String toString() {
        return "(" + offset() + ", " + otherArgs().toString() + ")";
    }

    public static IMatcher<TransferFunctionAppl> match() {
        return M.appl2("", M.integer(), M.listElems(), (applTerm, intTerm, argsTerm) -> {
            return ImmutableTransferFunctionAppl.of(intTerm.getValue(), argsTerm);
        });
    }
}
