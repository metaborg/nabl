package mb.statix.constraints.compiled;

import java.util.List;

import mb.nabl2.terms.ITerm;

public interface RExp {

    <R> R match(Cases<R> cases);

    <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E;

    interface Cases<R> {

        R caseResolve();

        R caseSubEnv(ITerm label, String stateRef);

        R caseMerge(List<RVar> envs);

        R caseShadow(RVar left, RVar right);

        R caseCExp(RVar env, RExp exp);

        default R match(RExp exp) {
            return exp.match(this);
        }

    }

    interface CheckedCases<R, E extends Throwable> {

        R caseResolve() throws E;

        R caseSubEnv(ITerm label, String stateRef) throws E;

        R caseMerge(List<RVar> envs) throws E;

        R caseShadow(RVar left, RVar right) throws E;

        R caseCExp(RVar env, RExp exp) throws E;

        default R match(RExp exp) throws E {
            return exp.matchOrThrow(this);
        }

    }

}
