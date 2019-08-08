# Constraint-based Permission Analysis

Issues:
- Maybe we can generate these constraints from the regular NaBL2 rules as symbolic
  constraints. That way, they may contain variables, occurrences, and everything.
  During the custom phase, we collect all of them and solve. We can add permissions
  to the type, or to a separate property on declarations or lambda's.
  We need a technical trick to get parameter numbers right, perhaps.
- Lambda parameters to queries should not have requirements on any parameters.

## Generated constraints

For predicates, requirements are all requirements not covered by local provisions,
and permissions are all permissions provided by all rules of that predicate.

    p(..., x, ...) :- _     p.i requires (diff R(x) P(x))
                            p.i provides P(x)
                            
Local variables' requirements should all be covered by permissions. 
                            
    p(...) :- {.. x ..} _   assertEmpty (diff R(x) P(x))

Query parameters should never require anything.

    p                       forall i. assertEmpty R(p.i)
    { p :- _ }              forall x in p. assertEmpty R(x)

A predicate use simply propagates requirements and provisions to the local
variable.

    p(..., x, ...)          x requires R(p.i)
                            x provides P(p.i)

The constraint `new` provides permission.

    new x                   x provides *

Edge and relation assertions require permission.

    x -l-> _                x requires {l}
    x -l-[] _               x requires {l}

## Computing permissions

A fixed point computation using the following formulae:

    R(v) = union { R | v requires R }
    P(v) = conj  { P | v provides P }

The `diff` function is defined as:

    diff R * = {}
    diff R 0 = R

Initial values ensure that requirements are a least fixed point, and provisions
a greatest fixed point.

    initial R(v) = {}    // least fixed point
            P(v) = *     // greatest fixed point

## Errors

Check all `assertEmpty` constraints.

## Sugar

Inline new:

    P(new) = *

Functional constraint outputs:

    f(...) = x :- _                 f.n provides P(x)
                                    assertEmpty R(f.n)

    P(f(...)) = P(f.n)
    R(f(...)) = R(f.n)

Mappings:

    ps maps p(..., *, ...)          ps.i requires R(p.i)
                                    ps.i provides P(p.i)

    fs maps f(..., *, ...) = _      fs.i requires R(f.i)
                                    fs.i provides P(f.i)

    fs maps f(...) = *              fs.n provides P(f.n)
