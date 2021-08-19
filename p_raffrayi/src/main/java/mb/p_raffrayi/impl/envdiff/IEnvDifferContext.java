package mb.p_raffrayi.impl.envdiff;

import org.metaborg.util.future.IFuture;

import mb.p_raffrayi.impl.diff.IScopeDiff;

public interface IEnvDifferContext<S, L, D> {

    IFuture<IScopeDiff<S, L, D>> scopeDiff(S previousScope);

}
