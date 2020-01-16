/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.metaborg.spoofax.analysis.benchmark;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.MetaborgRuntimeException;
import org.metaborg.core.analysis.AnalysisException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.IdentifiedResource;
import org.metaborg.core.project.IProject;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.analysis.ISpoofaxAnalyzeResults;
import org.metaborg.spoofax.core.shell.CLIUtils;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.util.concurrent.IClosableLock;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

import com.google.common.collect.Lists;

public class AnalysisBenchmark {

    private static final ILogger log = LoggerUtils.logger(AnalysisBenchmark.class);

    @State(Scope.Thread)
    public static class BenchmarkState {

        @Param({}) private String lang;

        Spoofax S;
        CLIUtils cli;
        ILanguageImpl langImpl;
        IProject project;
        IContext context;
        Collection<ISpoofaxParseUnit> parseUnits;

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

    @Benchmark @BenchmarkMode(Mode.SampleTime) @OutputTimeUnit(TimeUnit.SECONDS) public void
            doAnalysis(BenchmarkState state, Blackhole blackhole) {
        ISpoofaxAnalyzeResults results;
        try(IClosableLock lock = state.context.write()) {
            results = state.S.analysisService.analyzeAll(state.parseUnits, state.context);
        } catch(AnalysisException e) {
            throw new MetaborgRuntimeException(e);
        }
        blackhole.consume(results);
    }

}