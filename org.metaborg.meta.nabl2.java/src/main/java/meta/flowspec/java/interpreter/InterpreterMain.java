package meta.flowspec.java.interpreter;

import java.io.File;
import java.io.IOException;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.action.EndNamedGoal;
import org.metaborg.core.build.BuildInput;
import org.metaborg.core.build.BuildInputBuilder;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.ISimpleProjectService;
import org.metaborg.core.project.SimpleProjectService;
import org.metaborg.core.transform.TransformException;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.build.ISpoofaxBuildOutput;
import org.metaborg.spoofax.core.resource.SpoofaxIgnoresSelector;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;

import meta.flowspec.java.interpreter.expressions.ExpressionNode;

public class InterpreterMain {
    public static void main(String[] args) {
        File input = new File(args[0]);
        PolyglotEngine engine = PolyglotEngine.newBuilder().build();

        try {
            Source src = Source.newBuilder(input).build();
            engine.eval(src);
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
        }

        engine.dispose();
        
        try(Spoofax spoofax = new Spoofax()) {
            FileObject location = spoofax.resourceService.resolve(args[1]);
            ILanguageImpl implementation = spoofax.languageDiscoveryService.languageFromArchive(location);
            // TODO: adapt to reading CFG and creating {@link TransferFunction}s for each node
//            Expression expr = fileToExpression(spoofax, implementation, args[0]);
        } catch (MetaborgException e) {
            e.printStackTrace();
        }
    }

//    public static Expression fileToExpression(Spoofax spoofax, ILanguageImpl implementation, String path)
//        throws MetaborgException, IOException, InterruptedException
//    {
//
//        FileObject file = spoofax.resourceService.resolve(path);
//        IProject project = getProject(spoofax, file.getParent());
//
//        BuildInput input = new BuildInputBuilder(project)
//                .withSelector(new SpoofaxIgnoresSelector())
//                .addLanguage(implementation)
//                .addSource(file)
//                .addTransformGoal(new EndNamedGoal("Preprocess"))
//                .build(spoofax.dependencyService, spoofax.languagePathService);
//
//        ISpoofaxBuildOutput output = spoofax.builder.build(input);
//
//        if (!output.success())
//            throw new TransformException("Could not preprocess");
//
//        return Expression.fromIStrategoTerm(output.transformResults().iterator().next().ast());
//    }
//
//    public static IProject getProject(Spoofax spoofax, FileObject file) throws MetaborgException {
//        IProject project = spoofax.projectService.get(file);
//        if (project == null) {
//            ISimpleProjectService projectService = spoofax.injector.getInstance(SimpleProjectService.class);
//            project = projectService.create(file);
//        }
//        return project;
//    }
}
