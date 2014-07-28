# NaBL Core

## Binding and Bound Instances

The essence of name binding is establishing relations between a *binding instance* that *defines* a name and a *bound instance* that *uses* that name.

In NaBL Core, binding and bound instances are specified in rules. These rules consists of a name (`RuleID`), a pattern (`Pattern`), and either a `defines` or a `refers to` clause, which specify the namespace of a name (`Namespace`), the actual name (a variable from the pattern, `Variable`), and the scope, where this name is visible (`Scope`).

    <RuleID>: 
      <Pattern> 
        defines unique <Namespace> <Variable> 
        in <Scope>
    
    <RuleID>: 
      <Pattern> 
        defines non-unique <Namespace> <Variable> 
        in <Scope>

While `defines` clauses are unconditional, `refers` clauses specify additional conditions in a `where` clause (`Formula`). 

    <RuleID>: 
      <Pattern>
        refers to <Namespace> <Variable> 
        in <Scope>
        where <Formula>

## Scopes

Scopes determine the visibility of names. In NaBL, scopes can be nested and can be distinguished by namespace. At the root node, the global scope is the scope for all namespaces. Furthermore, each node determines the scope at its leftmost child and at its right sibling. By default, the scopes at the leftmost child and the right sibling are the same as at the node. NaBL Core allows to specify alternative scoping rules:

    <RuleID>: 
      <Pattern>
        <Scope> scopes <Namespace> at child nodes 

    <RuleID>: 
      <Pattern>
        <Scope> scopes <Namespace> at subsequent nodes 

All `defines`, `refers to`, and `scope` clauses refer to existing or new scopes. Existing scopes are:

* the global scope 

        global scope

* scope for a namespace at the current node, matched by the pattern

        current <Namespace> scope

* scope for a namespace at a subnode, bound to a variable in the pattern

        <Namespace> scope at <Variable>

* the enclosing scope of another scope

        enclosing <Namespace> of <Scope>

There are three ways to specify new scopes. First, every binding instance defines a new named scope inside the scope it is visible. These scopes are referred to by namespace and name of the binding instance.

    <Namespace> <Variable>

Second, new anonymous scopes can be defined inside an existing scope.
 
    new scope in <Scope>

Finally, new anonymous scopes can be specified as a requirement for subnodes.

    <RuleID>:
      <Pattern> 
        requires <Namespace> scope 
        at <Variable>

## Properties

In NaBL Core and TS, properties associate information with tree nodes and names. Properties associated tree nodes are specified in TS. Properties associated with names are specified in an NaBL  Core rule with a `defines … has` clause.

   <RuleID>: 
      <Pattern> 
        defines <Variable> has <Property> <Term>
        where <Formula>

NaBL Core and TS provide a single built-in property `type`.

## Formulas

Formulas can either specify derived information needed in `refers to` and `has` clauses or restrict these clauses to apply only if a formula holds. Formulas compose atomic formulas by applying boolean operators `not`, `and`, `or`. These atomic formulas are:

* boolean literals

        true
        false

* equality check

        <Variable> == <Term>

* relational check (relations are provided by TS)

        <Term> <Relation> <Term>

* pattern matching

        <Variable> => <Pattern>

* binding a variable to the binding instance of a bound instance

        definition of <Variable> => <Variable>

* binding a variable to a scope

        <Scope> => <Variable>

* binding a variable to a property of another variable (bound to a binding instance or a term)

        <Variable> has <Property> <Variable>

## Disambiguation

A name might be bound to different binding instances by different name binding rules with overlapping patterns. NaBL Core allows to disambiguate such ambiguous binding rules by specifying precedence of rules:

    <RuleID> > <RuleID> 

Disambiguation based on (partial) orders …

    <RuleID>:
      <Pattern>
        disambiguates <Variable>
        with <Formula>
        by <Selector> <Variable> wrt <Relation>
        where <Formula>
 
Selectors …

    minimum 
    maximum
    supremum
    infimium
    
## Aliases & Wildcard Imports

Aliases are pairs of binding and bound instances. The bound instance refers to the original name while the binding instance introduces a new name. The new name inherits properties and relations of the original name. In NaBL Core, binding and bound instances need to be specified in separate rules, while the alias is specified in a binding rule with an `imports as` clause.

   <RuleID>:
      <Pattern>
        imports <Variable> as <Variable>
        where <Formula>

Wildcard imports make binding instances from an original scope visible in another scope. 

   <RuleID>:
      <Pattern>
        imports <Namespace> 
        from <Scope>
        into <Scope>
        with <Formula>
        where <Formula>

