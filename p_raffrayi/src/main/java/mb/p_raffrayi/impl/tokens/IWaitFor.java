package mb.p_raffrayi.impl.tokens;

import org.metaborg.util.functions.Action1;

import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.impl.IUnit;

public interface IWaitFor<S, L, D> {

    IActorRef<? extends IUnit<S, L, D, ?>> origin();

    void visit(Cases<S, L, D> cases);

    interface Cases<S, L, D> {

        void on(InitScope<S, L, D> initScope);

        void on(CloseScope<S, L, D> closeScope);

        void on(CloseLabel<S, L, D> closeLabel);

        void on(Query<S, L, D> query);

        void on(PQuery<S, L, D> pQuery);

        void on(Confirm<S, L, D> confirm);

        void on(Datum<S, L, D> datum);

        void on(Match<S, L, D> match);

        void on(TypeCheckerResult<S, L, D> result);

        void on(TypeCheckerState<S, L, D> typeCheckerState);

        void on(DifferResult<S, L, D> differResult);

        void on(Activate<S, L, D> activate);

        void on(UnitAdd<S, L, D> unitAdd);

    }

    static <S, L, D> Cases<S, L, D> cases(Action1<InitScope<S, L, D>> onInitScope,
            Action1<CloseScope<S, L, D>> onCloseScope, Action1<CloseLabel<S, L, D>> onCloseLabel,
            Action1<Query<S, L, D>> onQuery, Action1<PQuery<S, L, D>> onPQuery, Action1<Confirm<S, L, D>> onConfirm,
            Action1<Datum<S, L, D>> onDatum, Action1<Match<S, L, D>> onMatch,
            Action1<TypeCheckerResult<S, L, D>> onResult, Action1<TypeCheckerState<S, L, D>> onTypeCheckerState,
            Action1<DifferResult<S, L, D>> onDifferResult, Action1<Activate<S, L, D>> onActivate,
            Action1<UnitAdd<S, L, D>> onUnitAdd) {
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

            @Override public void on(PQuery<S, L, D> pQuery) {
                onPQuery.apply(pQuery);
            }

            @Override public void on(Confirm<S, L, D> confirm) {
                onConfirm.apply(confirm);
            }

            @Override public void on(Datum<S, L, D> datum) {
                onDatum.apply(datum);
            }

            @Override public void on(Match<S, L, D> match) {
                onMatch.apply(match);
            }

            @Override public void on(TypeCheckerResult<S, L, D> result) {
                onResult.apply(result);
            }

            @Override public void on(TypeCheckerState<S, L, D> typeCheckerState) {
                onTypeCheckerState.apply(typeCheckerState);
            }

            @Override public void on(DifferResult<S, L, D> differResult) {
                onDifferResult.apply(differResult);
            }

            @Override public void on(Activate<S, L, D> activate) {
                onActivate.apply(activate);
            }

            @Override public void on(UnitAdd<S, L, D> unitAdd) {
                onUnitAdd.apply(unitAdd);
            }
        };
    }

}