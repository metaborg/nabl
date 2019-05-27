package statix.integration.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.build.BuildInputBuilder;
import org.metaborg.core.language.ILanguageComponent;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spoofax.core.build.ISpoofaxBuildOutput;

import com.google.common.collect.Iterables;

public class SingleFileTest extends AnalysisTest {

    private static ILanguageImpl langImpl;

    @BeforeClass public static void setUp() throws MetaborgException {
        AnalysisTest.setUp();
        final ILanguageComponent comp =
                S.languageDiscoveryService.componentFromArchive(S.resolve("res:singlefile.spoofax-language"));
        langImpl = Iterables.getOnlyElement(comp.contributesTo());
    }

    @Test public void test1() throws InterruptedException, MetaborgException {
        final IProject project = S.projectService.get(S.resolve("res:singlefile.test1/"));
        final BuildInputBuilder buildInput = new BuildInputBuilder(project);
        final ISpoofaxBuildOutput buildOutput =
                S.builder.build(buildInput.build(S.dependencyService, S.languagePathService));
    }

    @AfterClass public static void tearDown() {
        AnalysisTest.tearDown();
    }

}