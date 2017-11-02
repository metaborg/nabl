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

To be able to build this project, following these steps:

1. Clone this repository
1. `cd nabl/`
1. `mvn install` This should all projects in the correct dependency order
1. Open the projects in Spoofax
