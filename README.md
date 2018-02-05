[![Build status](http://buildfarm.metaborg.org/job/metaborg/job/nabl/job/master/badge/icon)](http://buildfarm.metaborg.org/job/metaborg/job/nabl/job/master/)
<!-- note that the branchname is not automatically updated in this badge -->


# NaBL: A Meta-Language for Declarative Name Binding and Scope Rules

In Spoofax, name bindings are specified in NaBL.
NaBL stands for *Name Binding Language* and the acronym is pronounced 'enable'.
Name binding is specified in terms of
  namespaces,
  binding instances (name declarations),
  bound instances (name references),
  scopes and
  imports.

## Documentation

We maintain the NaBL documentation in [our documentation repository](https://github.com/metaborg/doc/tree/master/meta-languages/nabl).

## Contributing

### Project structure

The project consists of several modules.

```
org.metaborg.meta.nabl2.shared
              |
org.metaborg.meta.nabl2.lang
              |
org.metaborg.meta.nabl2.runtime
              |
org.metaborg.meta.nabl2.java
```

* `shared`:

  All constructors and rules that exist in shared are used both during compilation with `lang` as well as during running using `runtime`.

* `lang`:

  The `lang` module is a codegenerator that, based on the constraint definitions by the user language, generates stratego rules that exist in `runtime`.

* `runtime`:

  The `runtime` module defines all stratego implementation to run the actual constraint generation based on the constraints that were defined in the user language.

* `java`:

  The constraints generated on runtime by `runtime` are solved using the `java` implementation.

### Extending nabl2

If you want to extend the Nabl2 language, you have several options.

1. If you need to extend the syntax of Nabl2 with for example sugar, you also need to implement the required transformations in `org.metaborg.meta.nabl2.lang/trans/nabl2/lang`.
These transformations define a mapping from Nabl2 constructs to stratego calls.
1. If the new syntax also requires new functionality, you will need to implement the functionality in `java` and create corresponding stratego definitions in `runtime`.
Do not directly call `java`-like functions inside generated stratego code from an user language nabl2 definition.
