package mb.nabl2.solver;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Map;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.functions.PartialFunction1;

import mb.nabl2.relations.terms.FunctionTerms;
import mb.nabl2.relations.terms.RelationTerms;
import mb.nabl2.relations.variants.VariantRelationDescription;
import mb.nabl2.scopegraph.terms.ResolutionParameters;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ASolverConfig {

    @Value.Parameter public abstract ResolutionParameters getResolutionParams();

    @Value.Parameter public abstract Map<String, VariantRelationDescription<ITerm>> getRelations();

    @Value.Parameter public abstract Map<String, PartialFunction1<ITerm, ITerm>> getFunctions();

    public static IMatcher<SolverConfig> matcher() {
        return M.tuple3(ResolutionParameters.matcher(), RelationTerms.relations(), FunctionTerms.functions(),
                (t, resolutionParams, relations, functions) -> {
                    return SolverConfig.of(resolutionParams, relations, functions);
                });
    }

}