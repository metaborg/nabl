package org.metaborg.spoofax.analysis.benchmark;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.MetaborgRuntimeException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.IdentifiedResource;
import org.metaborg.core.project.IProject;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.shell.CLIUtils;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import com.google.common.collect.Lists;

@State(Scope.Thread)
public class BenchmarkState {

    public static final String LANG_PARAM = "lang";

    @Param({}) public String lang;

    public Spoofax S;
    public CLIUtils cli;
    public ILanguageImpl langImpl;
    public IProject project;
    public IContext context;
    public Collection<ISpoofaxParseUnit> parseUnits;

    @Setup(Level.Trial) public void setUpTrial() {
        try {
            S = new Spoofax(new SpoofaxCLIModule());
            cli = new CLIUtils(S);
            cli.loadLanguagesFromPath();
            langImpl = cli.getLanguage(lang);
            project = cli.getOrCreateCWDProject();
            context = S.contextService.getTemporary(project.location(), project, langImpl);

            parseUnits = Lists.newArrayList();
            for(IdentifiedResource source : S.languagePathService.sourceFiles(project, langImpl)) {
                FileObject resource = source.resource;
                String text = S.sourceTextService.text(resource);
                ISpoofaxInputUnit inputUnit = S.unitService.inputUnit(resource, text, langImpl, null);
                ISpoofaxParseUnit parseUnit = S.syntaxService.parse(inputUnit);
                parseUnits.add(parseUnit);
            }
        } catch(MetaborgException | IOException e) {
            throw new MetaborgRuntimeException(e);
        }
    }

    @TearDown(Level.Trial) public void tearDownTrial() {
        if(S != null) {
            S.close();
        }
        S = null;
        cli = null;
        langImpl = null;
        project = null;
        context = null;
        parseUnits = null;
    }

}