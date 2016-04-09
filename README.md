# Iron-y

A project for building robust, scalable FE.

## Intro

Irony is an exploration of Elm-like component models for scalable 
front-ends. The Elm Architecture is a mechanism for highly simple, 
composable user interfaces atop virtual dom technology.

### Interface state, application state

Front-end applications above a certain size begin to form a distinction 
between "interface state" and "application state". As an example, there
is a difference in behavior and affordance between state describing, 
e.g., whether a drop-down box is opened or not and the name of 
application user with ID #2.

To dig into this difference a little further, we can compare and 
contrast the styles of state:

- **Interface State**
  - Lifecycle tied directly to visible UI
  - Synchronous
  - Denormalized, *or* normalized with respect only to UI structure
    - E.g., if you have three counters, each counter's state is likely 
      to be independent
- **Application State**
  - Persistent between all kinds of UI evolution
  - Asynchronous, likely to be updated out of synch with render cycles
  - Normalized, *or* expected to be the same across UI structure
    - E.g., if your app contemplates the "status of build #5" then that
      state should be identical no matter the location in the UI through
      which you are observing it
      
### Composability of interface state

We're well familiar with the notion of composing sub-interface (virtual)
dom notes together as a tree so as to form a complete interface. One 
nice affordance of interface state is that it can be composed along the
exact same lines.

If a Counter component has DOM nodes that look like

    <div class="counter">
      <button ...> + </button>
      <span class="counter-display"> 10 </span>
      <button ...> - </button>
    </div>

then a set of counters might look like

    <div class="counter-set">
        <div class="counter">
          <button ...> + </button>
          <span class="counter-display"> 23 </span>
          <button ...> - </button>
        </div>
        <div class="counter">
          <button ...> + </button>
          <span class="counter-display"> 19 </span>
          <button ...> - </button>
        </div>
    </div>

Likewise, if the state of the individual counter looks like

    {:count 10}
    
then the state of the set of counters might look like

    [{:count 23} {:count 19}]

By having interface state compose identically as interfaces themselves 
do we reduce the overhead of knowing the structure of the interface tree 
*and* the structure of the interface state tree. This also increases the 
ease of composition since the two kinds of tree plug into one another in
the exact same way.

### Elm architecture

Fundamentally, the Elm architecture is a system for composing interface 
state and interface form in the same way at the same time. We bundle the 
interface form (via a "view" function) and the interface state (via a 
"update" function giving semantics to interface state transitions) into
a record, a "component" which gets composed from there.

### Reacting to the user

While state and interface forms can paint the initial interface and 
update it as the state changes, we still have to understand how the 
interface itself accepts changes and interactions from the user.

The Elm architecture analogue here is the notion of components having 
access to a dispatcher ("address" in Elm terms, one place where Irony 
has a terminological departure) which they can use to submit user 
actions for (synchronous) interpretation.

Actions arise from the leaves of our component tree and are funnelled 
upward to cause a transition in the master, top-level state. This 
can feel like a lot of wiring, but it's easy to do due to the 
compositional nature of Elm components. Whenever defining a component 
which makes use of sub-components you must pass an appropriate dispatch 
function to the sub-components which interprets their submitted actions 
in terms of the actions appropriate to the super-component's state.

This ensures that the semantics of every action can be defined 
completely locally even though they end up being interpreted globally! 
What you purchase with explicit wiring is the best of both worlds of 
local state and global explicitness. Undo/redo functionality is trivial 
atop the Elm architecture, for instance.

#### Action example

Understanding actions can be tough in the abstract, but they're really 
quite simple. Consider again the counter and set of counters example 
from above.

For an individual counter, the actions we anticipate arise from the two
buttons and could be represented as a choice of keywords `:inc` and 
`:dec`. We're provided a `dispatch` function during render so we use it
to provide an `:onClick` implementation.

    (d/div {}
      (d/button
        {:onClick (fn [_] (dispatch :dec))}
        "-")
      (d/div
        {:style count-style}
        (str model))
      (d/button
        {:onClick (fn [_] (dispatch :inc))}
        "+"))
        
Giving local interpretation to these actions is simple to do when we 
define a component-local update function

    (defn updater [action model]
      (case action
        :inc (update model :count inc)
        :dec (update model :counte dec)))
        
When we're working with sets of counters, we need to handle the actions
which can arise from the subcomponent Counters and also craft an update 
function which "does the right thing" updating our subcomponent state.

For the first task, we just transform the dispatch function passed on 
to the subcomponents

    (Counter inner-state #(dispatch [ix %]))

where `inner-state` is one piece of the counter set state corresponding 
to the sub-counter at index `ix`. To be clear, counter set actions take 
the form `[index inner-action]` where `index` tells us which subcounter 
we're seeing an action from an `inner-action` is the corresponding 
counter action.

The counter set update function is responsible for doing routing to make 
sure that inner counters get the right actions passed back down to them.

    (defn updater [action model]
      (match action
        [target inner-action]
        (update
          model target
          (elm/updatef Counter inner-action))))
          
Here `elm/updatef` is a helper function which gets the state transition 
function for a component given the action it should be responding to. 
It's natural to build a master transition from the sub-transitions in 
this form.

#### Automating composition

Many times when you compose Elm components you're interested in having 
each subcomponent manage its state more-or-less independently of all of 
its peer components. For this common case and when the set of 
sub-components can be known statically the `elm/parallel` combinator 
lets you quickly compose non-interacting components without any wiring
boilerplate

For instance, if we just need two independent counters we might write

    (def CounterPair
      (elm/parallel 
        {:counter-one Counter
         :counter-two Counter}))
         
and it'll work more or less as intended. Options can be passed to 
customize both the rendering of the two subcomponents and to augment and
customize the action routing.

### Handling application state

Elm components manage interface state well, but will cause suffering if 
they are used to handle application state as well. Instead, application
state should be managed by mechanisms better suited to evolving, 
normalized data. 

A good option available today in ClojureScript is the Datascript 
client-side database library which offers a full Datomic interface 
in-browser. Updates to this database can trigger re-renders on a smaller 
number of "impure" components within the tree.

**Note:** Irony is still exploring how to achieve this part well.

### Side-effects and asynchronous effects

Tbd.

## License

Copyright © 2016 Joseph Abrahamson
