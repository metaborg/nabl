package mb.scopegraph.resolution;

import java.util.List;

public interface RExp<L> {

    <R> R match(Cases<L, R> cases);

    <R, E extends Throwable> R matchOrThrow(CheckedCases<L, R, E> cases) throws E;

    interface Cases<L, R> {

        R caseResolve();

        R caseSubEnv(L label, String stateRef);

        R caseMerge(List<RVar> envs);

        R caseShadow(RVar left, RVar right);

        R caseCExp(RVar env, RExp<L> exp);

        default R match(RExp<L> exp) {
            return exp.match(this);
        }

    }

    interface CheckedCases<L, R, E extends Throwable> {

        R caseResolve() throws E;

        R caseSubEnv(L label, String stateRef) throws E;

        R caseMerge(List<RVar> envs) throws E;

        R caseShadow(RVar left, RVar right) throws E;

        R caseCExp(RVar env, RExp<L> exp) throws E;

        default R match(RExp<L> exp) throws E {
            return exp.matchOrThrow(this);
        }

    }

}
