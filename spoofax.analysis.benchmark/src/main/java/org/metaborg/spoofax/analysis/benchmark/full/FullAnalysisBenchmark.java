package org.metaborg.spoofax.analysis.benchmark.full;

import java.util.concurrent.TimeUnit;

import org.metaborg.core.MetaborgRuntimeException;
import org.metaborg.core.analysis.AnalysisException;
import org.metaborg.spoofax.analysis.benchmark.BenchmarkState;
import org.metaborg.spoofax.core.analysis.ISpoofaxAnalyzeResults;
import org.metaborg.util.concurrent.IClosableLock;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class FullAnalysisBenchmark {

    public static void run(String langName) throws RunnerException {
        // @formatter:off
        final Options opt = new OptionsBuilder()
            .include(FullAnalysisBenchmark.class.getName() + ".run")
            .param(BenchmarkState.LANG_PARAM, langName)
            .shouldFailOnError(true)
            .build();
        // @formatter:on
        new Runner(opt).runSingle();
    }

    @Benchmark @BenchmarkMode(Mode.SampleTime) @OutputTimeUnit(TimeUnit.SECONDS) public void run(BenchmarkState state,
            Blackhole blackhole) {
        ISpoofaxAnalyzeResults results;
        try(IClosableLock lock = state.context.write()) {
            results = state.S.analysisService.analyzeAll(state.parseUnits, state.context);
        } catch(AnalysisException e) {
            throw new MetaborgRuntimeException(e);
        }
        blackhole.consume(results);
    }

}