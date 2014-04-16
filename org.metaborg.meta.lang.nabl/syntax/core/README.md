# NaBL Core

## Binding and Bound Instances

The essence of name binding is establishing relations between a *binding instance* that *defines* a name and a *bound instance* that *uses* that name.

In NaBL, binding and bound instances are specified in rules. These rules consists of a name (`RuleID`), a pattern (`Pattern`), and either a `defines` or a `refers to` clause, which specify the namespace of a name (`Namespace`), the actual name (a variable from the pattern, `Variable`), and the scope, where this name is visible (`Scope`).

    <RuleID>: 
      <Pattern> 
        defines <Namespace> <Variable> 
        in <Scope>

While `defines` clauses are unconditional, `refers` clauses specify additional conditions in a `where` clause (`Formula`). 

    <RuleID>: 
      <Pattern>
        refers to <Namespace> <Variable> 
        in <Scope>
        where <Formula>

## Scopes

Scopes determine the visibility of names. In NaBL, scopes can be nested and can be distinguished by namespace. At the root node, the global scope is the scope for all namespaces. Furthermore, each node determines the scope at its leftmost child and at its right sibling. By default, the scopes at the leftmost child and the right sibling are the same as at the node. NaBL allows to specify alternative scoping rules:

    <RuleID>: 
      <Pattern>
        <Scope> scopes <Namespace> at child nodes 

    <RuleID>: 
      <Pattern>
        <Scope> scopes <Namespace> at subsequent nodes 

Such as `defines` and `refers` clauses, `scope` clauses refer to existing or new scopes. Existing scopes are:

* the global scope 

        global scope

* scope for a namespace at the current node, matched by the pattern

        current <Namespace> scope

* scope for a namespace at a submode, bound to a variable in the pattern

        <Namespace> scope at <Variable>

* the enclosing scope of another scope

        enclosing <Namespace> of <Scope>

There are two kinds of defining new scopes. First, every binding instance defines a new named scope inside the scope it is visible. These scopes are referred to by namespace and name of the binding instance.

    <Namespace> <Variable>

Second, new anonymous scopes can be defined inside an existing scope:
 
    new scope in <Scope>

## Disambiguation

A name might be bound to different binding instances by competing name binding rules with overlapping patterns. NaBL allows to disambiguate such ambiguous binding rules by precedence:

    <RuleID> > <RuleID> 
 
## Formulas


