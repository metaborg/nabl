package statix.integration.test;

import org.metaborg.core.MetaborgException;
import org.metaborg.spoofax.core.Spoofax;

public abstract class AnalysisTest {

    protected static Spoofax S;

    protected static void setUp() throws MetaborgException {
        S = new Spoofax();
    }

    protected static void tearDown() {
        S.close();
    }

}