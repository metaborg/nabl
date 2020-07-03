package mb.statix.p_raffrayi.impl;

import mb.statix.actors.IActor;
import mb.statix.actors.IActorRef;

public class ProjectActor<S, L, D> extends AbstractClient<S, L, D> implements IProject<S, L, D> {

    public ProjectActor(IActor<? extends IProject<S, L, D>> self, IUnitContext<S, L, D> context, S root,
            Iterable<L> edgeLabels) {
        super(self, null, context, root, edgeLabels);
    }

    @Override public void _start(Iterable<? extends IActorRef<? extends IUnit<S, L, D>>> units) {
        for(IActorRef<? extends IUnit<S, L, D>> unit : units) {
            openScope(root(), unit);
        }
    }

}