package mb.p_raffrayi.nameresolution;

import org.metaborg.util.future.IFuture;
import org.metaborg.util.task.ICancel;

import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

public interface IQuery<S, L, D> {

    IFuture<Env<S, L, D>> resolve(IResolutionContext<S, L, D> context, ScopePath<S, L> path, ICancel cancel);

    LabelWf<L> labelWf(); // FIXME: for confirmation purposes.

}
