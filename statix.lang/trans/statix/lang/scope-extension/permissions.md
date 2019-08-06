# Constraint-based Permission Analysis

Issues:
- Parameters should locally behave like they have provide permission.
- What identifies the parameters of a lambda?
- Maybe a nicer approach is to generate these constraints from the regular NaBL2
  rules as symbolic constraints. That way, they may contain variables and everything.
  During the custom phase, we collect all of them and solve. We can add permissions
  to the type, or to a separate property on declarations or lambda's.
  We need a technical trick to get parameter numbers right, perhaps.
- Lambda parameters to queries should not have requirements on any parameters.

## Generated constraints

For predicates, requirements are all requirements not covered by local provisions,
and permissions are all permissions provided by all rules of that predicate.

    p(x) :- _               p.1 requires (diff R(x) P(x))
                            p.1 provides P(x)

A predicate use simply propagates requirements and provisions to the local
variable.

    p(x)                    x requires R(p.1)
                            x provides P(p.1)
                            assertEmpty (diff R(p.1) P(x))

The constraint `new` provides permission.

    new x                   x provides T

Edge and relation assertions require permission.

    x -l-> _                x requires {l}
    x -l-[] _               x requires {l}
                            assertEmpty diff {l} P(x)

## Computing permissions

A fixed point computation using the following formulae:

    R(v) = union { R | v requires R }
    P(v) = conj  { P | v provides P }

The `diff` function is defined as:

    diff R T = {}
    diff R F = R

Initial values ensure that requirements are a least fixed point, and provisions
a greatest fixed point.

    initial R(v) = {}    // least fixed point
            P(v) = T     // greatest fixed point

## Errors

Check all `assertEmpty` constraints.

## Sugar

Inline new:

    P(new) = T

Functional constraints:

    f(... i-1 ...) = x :- _         f.i provides P(x)
                                    check R(f.i) <? {}

Use of functional constraints:

    P(f(...)) = P(f.i)
    R(f(...)) = R(f.i)

Mappings:    

    ps maps p(..., *, ...)          ps.i requires R(p.i)
                                    ps.i provides P(p.i)
