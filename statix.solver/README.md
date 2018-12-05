Statix -- Java Components
=========================

The project contains the Java implementation of the Statix solver.

Semantics of delays
-------------------

Constraints can be delayed on variables or critical edges. When
constraints are delayed on multiple variables, it is important to
reactivate the constraint when _any_ of the variables is instantiated. To
see this, we consider an example:

    resolve {i j}
      eq(i, j), i == j
    rules
      eq(t, t).

If the unification of `i` and `j` didn't happen yet, the `eq` constraint
gets stuck on both `i` and `j`. It would be wrong to index the constraint on
either one of them, since the unification could result in `i` being
substituted for `j`, or the other way around. Therefore, if a constraint
is stuck on multiple variables, we treat it as stuck on any of them,
but not all of them.