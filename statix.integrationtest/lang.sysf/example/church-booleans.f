type B = A => A -> A -> A in
let t = Fun(A){ fun(x: A){ fun(y: A){ x } } } : B in
let f = Fun(A){ fun(x: A){ fun(y: A){ y } } } : B in
type IF = A => B -> A -> A -> A in
let if = Fun(A){ fun(x: B){ fun(y: A){ fun(z: A){ x[A] y z } } } } : IF in
  if[B] t f t