
# Oak Tutorial

*A library for compositional web applications.*

The fundamental challenges of web application design are state and time 
management. Oak tackles these challenges head-on be composition of 
standard (Moore-style) state machines. Oak is built atop React and uses
a design very similar to the Elm architecture which influenced 
Javascript's `redux` library---so if you know those technologies, this 
will look pretty familiar.

## Constant components

Oak decomposes applications into components. The simplest components are
*constant* components which are nothing more than a view function.

```clojure
(require '[oak.core :as oak])
(require '[oak.dom :as d])

(def my-component
  (oak/make
    {:name "My-Component"
     :view 
     (fn [_ _]
       (d/h1 {:class "my-component"} "Hello world"))}))
```

As you can see, you use the `oak/make` function to create components and
then define a `:view` function of two (currently ignored) arguments. 
This view function should be pure and returns a virtual dom tree 
constructed from functions from `oak.dom` or from the view functions of
other Oak components as we will see now.

```clojure
(def my-layout
  (oak/make
    :name "My-Layout"
    :view
    (fn [_ context]
      (d/section {:class "layout"}
        (my-component nil context)))))
```

Here, another constant component is created demonstrating two new ideas.
First, we see that Oak components implement `IFn` and can be called as
regular functions of two arguments. Second, we see that the second 
argument is called the "context" and must be passed to the inner 
component.

> Why don't we use dynamic binding to do context passing? This would 
> let our components take only one argument, after all! Oak avoids this
> for two reasons, though. First, explicit passing is simpler and makes
> testing easier. Second, we'll see later that it's not at all uncommon
> to *modify* the context before you pass it on to child components--- 
> explicit passing makes this more obvious.

## Parameterizing components

Constant components are pretty boring. If `view` must be pure, then we 
need to pass in interesting arguments for it to behave nicely! For this
we introduce the *model*.

```clojure
(require '[schema.core :as s])

(def number-display
  (oak/make
    {:name "Number-Display"
     :model {:name s/Str :value s/Int}
     :view 
     (fn [model context]
       (d/div {:class "number-display"} 
         (d/strong (:name model))
         (str (:value model))))}))
```

Here, we finally see what the first argument to our view function is: 
the "model" for this component. "Model" (and "view") here steals from 
MVC terminology so the right way to think is that the model describes 
the "raw" data substantiating our view. We give our model a schema for 
documentation and dev-time checking purposes.

> Models aren't necessarily business objects. In Oak we take note that 
> state within your UI is often relatively denormalized (repeated) and 
> may not be easily kept in synch with the domain objects your code is
> dealing in. Think of the view as a very thin layer over the model and
> we'll see how to handle larger scale "domain state" later.

Finally, we should show how to provide an instance of the model to your
components---we just pass it in next to the context, of course!

```clojure
(def scoreboard
  (oak/make
    {:name "Scoreboard"
     :model {:runs   (oak/model number-display)
             :hits   (oak/model number-display)
             :errors (oak/model number-display)
     :view
     (fn [model context]
       (d/ul {:class "scoreboard"}
         (d/li {} (number-display {:name "Runs" :value (:runs model)}))
         (d/li {} (number-display {:name "Hits" :value (:hits model)}))
         (d/li {} (number-display {:name "Errors" :value (:errors model)}))))}))
```

As a side note, take notice that we can use `oak/model` to get the model 
schema of a component. This is highly recommended when composing 
components! It'll protect your schema definition from changes to your 
subcomponent models.

## States in motion, actions and steps

So far, we've created a pretty belabored templating system, but UIs need
to support interaction! For this, we introduce the notion of the state 
machine.

First, an example:

```clojure
(def counter
  (oak/make
    {:name "Counter"
     :model s/Int
     :action (s/enum :inc :dec)
     :step
     (fn [action model]
       (case action
         :inc (inc model)
         :dec (dec model)))
     :view
     (fn [model context]
       (d/div {:class "counter"}
         (d/button {:onClick (fn [_] (oak/act context :inc))} "+")
         (d/span {:class "number"} (str model))
         (d/button {:onClick (fn [_] (oak/act context :dec))} "-")))}))
```

What we can see here is that the view consists of a numeric display with 
two buttons. On clicking the button we `oak/act` upon the `context` with
a keyword. There's a new definition, the `:step` function which takes in
the action and the model and defines how this action "acts" upon the 
model. Finally, we note that the `:action` key defines a schema 
describing all possible actions.

So, the obvious thing to think is that our component is able to send 
messages to itself (called "actions") and the `:step` function controls
how these messages cause updates. This is, in fact, exactly the case, 
but let's belabor this a bit further.

### State machines

A state machine is a way of describing how some piece of state evolves 
over discrete time steps by way of "events". Given a state, `s` and such
an event, `e`, we also need a function `(next e s)` which produces the 
next state.

From here there are lots of things to examine. We can a state machine as
turning a sequence of events into a `trajectory` on the state of spaces

```clojure
(defn trajectory [initial-state event-sequence]
  (reductions next initial-state event-sequence)) 
```

We could also see `next` as giving us a set of "transition functions" on
the state space, one for each action:

```clojure
(defn transition-fn [event]
  (fn [state] (next event state)))
```

But the real thing to take home is how a state machine forms a 
self-contained world atop just a single pure function. Many quite 
sophisticated things can be modeled as a state machine and thus we take
them as the basis for Oak.

Except, we rename `next` to `step`, `event` to `action`, and `state` to 
`model` just to have our own nomenclature.

## Composing state machines

Up until this point composing components has been trivial. This is 
because (a) we pretty much only had to pass our models "down" the tree
and (b) because we didn't have to worry about changes.

Once we add in the state machine technology we'll need to account for it 
as well when composing components. To do this, let's build a list of 
counters: the most sophisticated example so far!

> It's worth noting that Oak *could* choose do a lot of the following 
> composition automatically for you. Why do we leave the boilerplate to 
> you? Well, first, combinators exist to automate this for you later, 
> and second, these are matters of design and it's better leave them 
> explicitly up to you, the user. Oak is almost always going to avoid 
> magic as much as possible.

```clojure
(def counter-set
  (oak/make
    {:name "Counter-Set"
     :model [(oak/model counter)]
     :action (s/cond-pre 
               :new 
               (s/pair s/Int 
                       :index
                       (s/cond-pre
                         (s/eq :remove)
                         (oak/action counter)
                       :inner-action))
     :step ...
     :view ...}))
```

Let's stop here and examine what's going on. First, we note that our 
model is defined as a vector of `counter` models. We'll represent our 
counter set as an ordered set where we can index each counter by the 
position of its "sub"-model in this vector.

We also see that the `:action` schema has gotten a little complex. 
Essentially, actions are now either `:new`, presumably generating a new
counter for our vector, or a pair (a two element vector) of an index and 
an "inner action". Inner actions are either `:remove` or an action from
a `counter`.

So it might be clear where we're going with this now, but for the 
avoidance of all doubt: the set of actions of a component are (often) 
the actions of all of its subcomponents adjoined to its own actions.

Let's see how this plays out in the `:step` function

```clojure
  ...
  :step
  (fn [action model]
    (match action
      :new (conj model 0))
      [index :remove] (vector-remove-at model index)
      [index inner-action] (update model index (oak/stepf counter inner-action))))
  ...
```

We've brought in Clojure's pattern matching macro for a little help 
describing how to handle these actions, but there's not a whole lot 
going on here. If the action is `:new` we conj on a new counter model. 
If it's a pair we'll be acting on the sub-model at the cooresponding 
index. If the action is `:remove` then we'll throw that sub-model away 
(definition of `vector-remove-at` elided). Otherwise, we pass the 
interpretation of the action down to the `step` function of `counter` 
using a helper function `stepf` (which generates transition functions 
from a component).

Notably, again, this has the feeling of boilerplate (and indeed can and 
is automated in common cases) but it's not too burdensome so long as we 
take it little piece by little piece like so. The core concept of Oak is
compositionality and so a lot of the emphasis ends up being on 
composition.

So, finally, let's take a look at what our view looks like.

```clojure
  ...
  :view
  (fn [model context]
    (let [children (map-indexed
                      (fn [ix submodel]
                        (d/div {}
                          (d/button {:onClick (fn [_] (oak/act context [ix :remove]))} "Remove")
                          (counter 
                            submodel 
                            (oak/route context (fn [a] [ix a])))))
                      model)]
      (apply d/div {:class "counter-set"} 
             (d/button {:onClick (fn [_] (oak/act context :new))} "New!")
             children)))
  ...
```

There are a few interesting things going on here. First, note that we
build each child dynamically from our model which ends up being vector 
of counter models. Second, note that we construct our `:remove` action
by building a pair of the current index and the `:remove` keyword. 
Third, and most important note how we use `oak/route` to *modify the 
context* so that it prepends the index to *all* child actions.

To be clear, `oak/route` lets us modify a context by providing a 
"pre-processor" function which modifies actions in flight before passing
them on. This is worth analyzing carefully.

In particular, we note that as we move "down" the tree we see 
composition arise in the *model* as us splitting it into little pieces,
extracting submodels and passing them down. It's an act of 
decomposition. On the other hand, at the same time we talk about how to
build actions "up", combining them into super actions.

If you look at it just right, you see that the actions and the models 
compose in "opposite directions". This is exactly the nature of 
combining state machines since actions are "inputs" and the models are 
"outputs".

## Running Oak UIs

tk.

## Interface State versus Application State

