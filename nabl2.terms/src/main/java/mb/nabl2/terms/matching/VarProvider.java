package mb.nabl2.terms.matching;

import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function1;

import mb.nabl2.terms.ITermVar;

public interface VarProvider {

    ITermVar freshVar(ITermVar var);

    ITermVar freshWld();

    static VarProvider of(Function1<ITermVar, ITermVar> freshVar, Function0<ITermVar> freshWld) {
        return new VarProvider() {

            @Override public ITermVar freshVar(ITermVar var) {
                return freshVar.apply(var);
            }

            @Override public ITermVar freshWld() {
                return freshWld.apply();
            }

        };
    }

}