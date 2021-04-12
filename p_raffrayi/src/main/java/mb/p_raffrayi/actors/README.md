# Actor Library

This package implements a small actor library.







## Overview

Actors are defined by the interface they implement. Users and actors communicate
with other actors by invoking methods of the actors interface.

IActorSystem:
  Represents the system of actors and provides methods to the user to add actors,
  as well as start and stop the system.

IActor:
  On construction, the actor implementation gets and IActor reference to the under-
  lying actor. This reference should *not* be shared and is only for use within the
  actor implementation. This design was chosen so that the implementation does not
  need to inherit from a specific class.

IActorRef:
  Reference to an actor in the system, which can be used to get a callable interface
  to the actor. These reference are safe for comparison, and may be shared.

## Message Delivery

The library guarantees message delivery, or it will raise a failure on the sending
actor.

Two kinds of messages are supported:
1. Messages without a return type, implemented as `void` methods.
2. Messages with a return value, implemented as methods returning an `IFuture<T>`.
   The library ensures that the completion of the returned future is correctly
   scheduled on the original calling actor. 

## Handling Failure

Actors fail because the implementation or the underlying actor threw an exception,
or because a message could not be delivered. In the first case, the problem is
raised on actor's parent actor. In the second case, the problem is raised on the
sending actor. By default, both failures cascade, resulting in the failure of the
parent or sender, respectively.

## Monitoring & Control

The library has support for implementing control algorithms on top of the primary
computation, such as updating logical clocks, or initiating deadlock detection.
Actors can implement the IActorMonitor interface to monitor events of the under-
lying actor, such as suspend/resume and message sent/receive. Control messages
are indicated by marking their methods with the @ControlMessage annotation. These
messages do not trigger trigger the event in IActorMonitor, only primary messages
do so.