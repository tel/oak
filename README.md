
# Oak

*Purely functional React components with managed local state and single-atom 
 state management. Each component is totally independent and can be composed.*
 
Oak *components* are an implementation of the 
[Elm Architecture](http://guide.elm-lang.org/architecture/index.html)
in ClojureScript. Oak *oracles* are a composable, query-based solution to 
asynchronous global state.

## Status

*Beta*. The Oak API is still under active investigation and is subject 
to change. There is a public release on the horizon.

## Try it out!

This repository is both the library and a Devcards environment of 
examples. Download the repo and run `lein figwheel` to access the 
examples.

## 5-Minute Tutorial

### Basics

At the core, a basic Oak component is a *pure* function of its "model" value. 
The simplest Oak component is a constant component which expresses nothing 
more than a view function (very similar to 
[Quiescent](https://github.com/levand/quiescent))

```
(require '[oak.component :as oak])
(require '[oak.dom :as d])

(oak/make
  :view (fn [model _] 
          (d/div {} 
            (str (:count model)))))
```

Use the `oak.dom` namespace to construct basic DOM fragments or any other 
React library. Your component's view function merely needs to return a 
`ReactElement` somehow or another.

### Defining dynamics

Oak components are simple state machines. The model represents the current 
state and *actions* represent state transitions. To give dynamics to a 
component describe how actions change the state with a `:step` function. 
We'll, for instance, improve the previous example to respond to the `:inc` 
action by incrementing the model's `:count` and the `:dec` action by 
decrementing it.

```
(oak/make
  :step (fn [action model] 
          (case action
             :inc (update model :count inc)
             :dec (update model :count dec)))
  :view ...)
```

The view function needn't change. It tells us how to show any state.

### Implementing dynamics

A component's model is updated by actions which are submitted from its view. 
To provide for this we use the `submit` function given to our `view` function

```
(oak/make
  :step ... 
  :view (fn [model submit]
          (d/div {} 
            (d/button {:onClick (fn [_] (submit :dec))} "-")
            (d/span (str (:count model)))
            (d/button {:onClick (fn [_] (submit :inc))} "+"))))
```

Calling the passed submit function causes the component's state to update 
synchronously.

*And that's pretty much all you need to know to get started!*

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
