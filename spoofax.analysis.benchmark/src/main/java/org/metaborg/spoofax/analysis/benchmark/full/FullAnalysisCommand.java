package org.metaborg.spoofax.analysis.benchmark.full;

import java.util.concurrent.Callable;

import org.openjdk.jmh.runner.RunnerException;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "full", description = "Benchmark full analysis")
public class FullAnalysisCommand implements Callable<Integer> {

    @Option(names = { "-l", "--lang" }, required = true) private String langName;

    @Override public Integer call() throws RunnerException {
        FullAnalysisBenchmark.run(langName);
        return 0;
    }

}