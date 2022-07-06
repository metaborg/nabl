package mb.scopegraph.resolution;

import java.util.List;

import mb.scopegraph.oopsla20.reference.ResolutionException;

public interface RExp<L> {

    <R> R match(Cases<L, R> cases);

    <R, E extends Throwable> R matchInResolution(ResolutionCases<L, R> cases)
            throws ResolutionException, InterruptedException;

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

    interface ResolutionCases<L, R> {

        R caseResolve() throws ResolutionException, InterruptedException;

        R caseSubEnv(L label, String stateRef) throws ResolutionException, InterruptedException;

        R caseMerge(List<RVar> envs);

        R caseShadow(RVar left, RVar right) throws ResolutionException, InterruptedException;

        R caseCExp(RVar env, RExp<L> exp) throws ResolutionException, InterruptedException;

        default R match(RExp<L> exp) throws ResolutionException, InterruptedException {
            return exp.matchInResolution(this);
        }

    }
}
