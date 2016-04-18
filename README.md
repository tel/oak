
# Oak

*A library for compositional web applications.*

The fundamental challenges of web application design are state and time 
management. Oak tackles these challenges head-on be composition of 
standard (Moore-style) state machines. Oak is built atop React and uses
a design very similar to the Elm architecture which influenced 
Javascript's `redux` library---so if you know those technologies, this 
will look pretty familiar.

## Try it out!

This repository is both the library and a Devcards environment of 
examples. Download the repo and run `lein figwheel` to access the 
examples.

## Concepts

*Components are rendered as pure functions of some local parameters*

At the core of any component is a pure function from an immutable value 
to a ReactElement. Oak doesn't really care how you construct this 
function and is reasonably naive to choice of React wrappers. What's 
important is that to the greatest degree possible the entire state of 
your component is reflected in this immutable parameter.

*Applications are state machines, reductions of state over an event stream*

Ultimately, an application's ("local") function is just a big reduction
step. An "event" is generated from the UI and this triggers a transition
of the current state to the next one.

*Local state is different from "Application State"*

Local state and "application state" behave very differently. To draw the 
line clearly, local state answers questions like "is my dropdown 
expanded?" while application state answers questions like "what is 
User #10's first name?". Local state is denormalized, changes 
synchronously, and is shaped almost the same as your UI ReactElement 
tree. Application state is normalized, updates asynchronously, and is 
shaped more like a SQL database.

*State, events, reducers, and application state queries are all fractal*

Oak encourages you to build up to the full complexity of your interface
step-by-step by composing smaller fully functional "applications" 
one-by-one. An Oak "component" is pretty much exactly that—a small, 
fully functional application, and it expects that any other component 
which embeds it supports that view.

As it turns out, it's easy to compose the upward and downward 
information flows of nested applications with simple, pure functions. 
The wiring burden is highly distributed and each component sees all of 
the state of the world it needs.

## What is a component?

A component is a set of 3 functions and 2 schemata. It is a spec for a
fully functional web interface all by itself.

```clojure
:state ; a schema describing the local state
:event ; a schema describing the events this component emits

:query ; a function constructing the application state (different from 
       ; :state) queries this component demands 
       ; (this is described in more detail later)
       
:step  ; a (pure) function (event, state) -> state describing how
       ; :events this component emits update the local :state
       
:view  ; a (pure) function (state, submit-fn) -> ReactElement which
       ; interprets the state as a UI view. Here, submit-fn is a 
       ; callback the UI view uses to submit :events to the :step 
       ; function updating the :state.
```

When a component is run it's expected that it will be provided with an 
initial `:state` to generate the `:view`. It's expected that the 
`:query` is satisfied by a third-party "oracle" and used to construct 
the `:state`. It is expected that any `:event` submitted in the `:view`
is used by the `:step` function to update the `:state` and `:query` then,
likely, triggering a re-render if the change is substantial.

From the outside, *all* of the `:state`, `:event`, `:step`, `:query`, 
and `:view` ought to be considered private and abstract. This key to 
making composition scale.

When working with components it's a good idea to provide a function to
construct the initial state of your component. Users of your component 
are well-recommended to use it, too, in order to maintain the 
abstractness of the component `:state`.

### A simple example

An example component using only local state is a counter with increment 
and decrement buttons. We use the `oak/make` function to build it. By 
default this uses Quiescent to wrap up the `:view` function into a React 
component factory letting React lifecycle hooks become available, but you
can choose whatever ReactElement constructor you like (see the 
`:build-factory` key)

```clojure
(require '[oak.core :as oak])
(require '[oak.dom :as d])
(require '[schema.core :as s])

(def counter
  (oak/make
    :name "Counter"
    :state s/Int
    :event (s/enum :inc :dec)
    :step (fn [event state]
             (case event
               :inc (inc state)
               :dec (dec state)))
    :view (fn [state submit]
             (d/div {}
                (d/button {:onClick (fn [_] (submit :dec))} "-")
                (d/span {} (str state))
                (d/button {:onClick (fn [_] (submit :inc))} "+")))))
```

## Application state and Oracles

Application state is often a complex case. Oak essentially avoids 
committing to anything about application state, but instead offers 
another state-machine-like interface for handling application state 
called the "Oracle".

Essentially, an oracle is responsible for maintaining some kind of data 
cache (anything from a map to a DataScript database), using it to 
substantiate component `:query` requests during render steps, and then, 
asynchronously, refreshing it given knowledge of all the queries which 
were requested.

The structure of the queries, the nature of the responses, the design of
the cache, and the mechanism of refreshing it are all up to the design 
of the Oracle.

Some example Oracles might be

- A simple map where the queries are the keys and refreshing entails 
  nothing more than updating the cache value from a global atom which is
  updated periodically though some other mechanism.
- HTTP requests to a backend API which are cached with a TTL. Queries are
  initially substantiated with a placeholder and then refresh steps 
  replace that placeholder in the cache with an actual successful fetch
  or the appropriate error.
- A DataScript database where queries are actual datalog queries
- A combination of all of the above acting in parallel!

## System actions

Missing from the architecture so far is the ability to trigger 
system-level state transitions. In other words, if `:query` behaves a 
bit like an HTTP `GET` request, what are `POST` and `PUT`?

Oak has no built-in answer for this! Generally, one might think of 
either (a) having an impure top-level `:step` function which interprets 
local `:event`s as having global consequences, or (b) passing down a 
CSP channel application-event bus as part of the local state which can 
be used by some components to submit application-level events.

# License

Copyright (c) 2016, Joseph Abrahamson
All rights reserved.

Redistribution and use in source and binary forms, with or without 
modification, are permitted provided that the following conditions are 
met:

1. Redistributions of source code must retain the above copyright 
   notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright 
   notice, this list of conditions and the following disclaimer in the 
   documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its 
   contributors may be used to endorse or promote products derived from 
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS 
IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED 
TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
