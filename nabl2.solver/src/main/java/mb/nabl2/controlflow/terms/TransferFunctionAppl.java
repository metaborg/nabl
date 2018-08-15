package mb.nabl2.controlflow.terms;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;

@Immutable
public abstract class TransferFunctionAppl {
    @Parameter public abstract String moduleName();
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

    public Object[] args() {
        return otherArgs().toArray();
    }

    @Override
    public String toString() {
        return "(" + moduleName() + ", " + offset() + ", " + otherArgs().toString() + ")";
    }

    public static IMatcher<TransferFunctionAppl> match() {
        return M.appl3("", M.stringValue(), M.integerValue(), M.listElems(), (applTerm, strTerm, intTerm, argsTerm) -> {
            return ImmutableTransferFunctionAppl.of(strTerm, intTerm, argsTerm);
        });
    }
}
