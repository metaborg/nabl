package mb.statix.solver.persistent.step;

import mb.statix.solver.IConstraint;
import org.metaborg.util.functions.Function1;

public interface IStep {

    IConstraint constraint();

    StepResult result();

    <R> R match(Cases<R> cases);

    interface Cases<R> extends Function1<IStep, R> {

        R caseArith(CArithStep step);

        R caseAstId(CAstIdStep step);

        R caseAstProperty(CAstPropertyStep step);

        R caseConj(CConjStep step);

        R caseEqual(CEqualStep step);

        R caseExists(CExistsStep step);

        R caseFalse(CFalseStep step);

        R caseInequal(CInequalStep step);

        R caseNew(CNewStep step);

        R caseResolveQuery(AResolveQueryStep step);

        R caseTellEdge(CTellEdgeStep step);

        R caseTrue(CTrueStep step);

        R caseTry(CTryStep step);

        R caseUser(CUserStep step);

        @Override default R apply(IStep step) {
            return step.match(this);
        }

    }

}
