package mb.statix.concurrent.p_raffrayi.impl.tokens;

import org.metaborg.util.functions.Action1;

public interface IWaitFor<S, L, D> {

    void visit(Cases<S, L, D> cases);

    interface Cases<S, L, D> {

        void on(InitScope<S, L, D> initScope);

        void on(CloseScope<S, L, D> closeScope);

        void on(CloseLabel<S, L, D> closeLabel);

        void on(Query<S, L, D> query);

        void on(TypeCheckerResult<S, L, D> result);

        void on(TypeCheckerState<S, L, D> typeCheckerState);

    }

    static <S, L, D> Cases<S, L, D> cases(Action1<InitScope<S, L, D>> onInitScope,
            Action1<CloseScope<S, L, D>> onCloseScope, Action1<CloseLabel<S, L, D>> onCloseLabel,
            Action1<Query<S, L, D>> onQuery, Action1<TypeCheckerResult<S, L, D>> onResult,
            Action1<TypeCheckerState<S, L, D>> onTypeCheckerState) {
        return new Cases<S, L, D>() {

            @Override public void on(InitScope<S, L, D> initScope) {
                onInitScope.apply(initScope);
            }

            @Override public void on(CloseScope<S, L, D> closeScope) {
                onCloseScope.apply(closeScope);
            }

            @Override public void on(CloseLabel<S, L, D> closeLabel) {
                onCloseLabel.apply(closeLabel);
            }

            @Override public void on(Query<S, L, D> query) {
                onQuery.apply(query);
            }

            @Override public void on(TypeCheckerResult<S, L, D> result) {
                onResult.apply(result);
            }

            @Override public void on(TypeCheckerState<S, L, D> typeCheckerState) {
                onTypeCheckerState.apply(typeCheckerState);
            }
        };
    }

}