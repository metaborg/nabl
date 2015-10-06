# NaBL2

Namebinding and typechecking language based on scope graphs and
constraints. Read further for instructions on how to use it for your
own Spoofax language.

## Project Setup

NaBL2 only works with Spoofax Core projects. Instructions how to
convert existing languages to the Spoofax Core plugin can be found at
http://metaborg.org/dev/newplugin/.

To use NaBL2 we need to add two dependencies to the POM, one to
compile NaBL2 files and one for the accompanying runtime library, like
this:

    <project>
      [...]
      <dependencies>
        [...]
        <dependency>
          <groupId>org.metaborg</groupId>
          <artifactId>org.metaborg.meta.lib.analysis2</artifactId>
          <version>${metaborg-version}</version>
          <type>spoofax-language</type>
          <scope>${metaborg-meta-lang-scope}</scope>
        </dependency>
        <dependency>
          <groupId>org.metaborg</groupId>
          <artifactId>org.metaborg.meta.lang.nabl2</artifactId>
          <version>${metaborg-version}</version>
          <type>spoofax-language</type>
          <scope>${metaborg-meta-lang-scope}</scope>
        </dependency>
        [...]
      </dependencies>
      [...]
    </project>

You can remove the dependencies to `org.metaborg.meta.lang.nabl` and
`org.metaborg.meta.lang.ts`, although they won't cause conflicts if
you leave them in.

## Use NaBL2 Analysis

NaBL2 comes with its own runtime, which provides the analysis
functions you should use. To use them, we need to change the
language's main Stratego file (e.g. `trans/LANGNAME.str`). First,
remove any imports of
`runtime/{analysis,nabl,task,index,properties,types}/-`, and usages of
the old `analysis-*` strategies, because they will not work
anymore. Then, add the following imports and rules:

    module LANGNAME

    imports

      [...]

      libanalysis2/-
      libanalysis2/namebinding/-
      libanalysis2/subtyping/-
      libanalysis2/prelude/-
      libanalysis2/nabl2/-

      [...]

    rules // Analysis

      [...]

      // TODO: rename analyze(|) to the 'on save' strategy in your main ESV.
      // TODO: replace desugar-all with your desugaring strategy or 'id', if you none
      analyze = analyze(desugar-all)
 
      debug-analyze = debug-analyze(desugar-all)

    rules // Editor services

      [...]

      editor-resolve: (node, position, ast, path, project-path) -> decl
        where decl := <get-decl> node

      editor-hover: (node, position, ast, path, project-path) -> label
        where label := <get-type;pp-partial-PCF-string;xmlencode> node

      [...]

    rules // Debugging

      [...]

      debug-show-analysis: (_, _, source-ast, path, project-path) -> (filename, result)
        with filename := <guarantee-extension(|"analysis.aterm")> path
           ; result := <debug-analyze;pp-debug-analysis> (source-ast, path, project-path)

      [...]

Add an entry to `editor/LANGNAME-Menus.esv` to add a builder that lets
you inspect the analysis result:

    module LANGNAME-Menus

    menus

      menu: "Analysis"

        action: "Show analysis" = debug-show-analysis (source) (openeditor) (meta) (realtime)

## Specify Your Analysis in NaBL2

You are ready to start writing NaBL2 (`.nab2` and `.ts2`) files. Start
by looking at the example in `test/example.nab2`.
