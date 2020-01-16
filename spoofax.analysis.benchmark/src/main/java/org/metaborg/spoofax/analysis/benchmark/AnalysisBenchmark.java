package org.metaborg.spoofax.analysis.benchmark;

import java.util.concurrent.Callable;

import org.metaborg.spoofax.analysis.benchmark.full.FullAnalysisCommand;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "java -jar analysis-benchmark.jar", description = "Benchmark analysis performance.",
        subcommands = { FullAnalysisCommand.class })
public class AnalysisBenchmark implements Callable<Integer> {

    @Option(names = { "-h", "--help" }, description = "show usage help", usageHelp = true) private boolean usageHelp;

    @Override public Integer call() throws Exception {
        CommandLine.usage(this, System.err);
        return 1;
    }

    public static void main(String[] args) {
        final int exitCode = new CommandLine(new AnalysisBenchmark()).execute(args);
        System.exit(exitCode);
    }

}