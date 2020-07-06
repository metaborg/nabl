package mb.statix.concurrent.p_raffrayi.impl;

import org.spoofax.terms.util.NotImplementedException;

public interface IWaitFor {

    void fail();

    public static IWaitFor of(String name, Object... args) {
        throw new NotImplementedException();
    }

}