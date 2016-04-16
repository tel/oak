(ns oak.oracle
  "An Oracle is a system for determining, perhaps only eventually, answers to
  queries. For instance, a database is naturally a (synchronous) Oracle. So
  is a REST API, though this one is asynchronous.

  Oracles differ in the kinds of queries they respond to and the nature of
  their responses. They are the same in that they manage state in a way
  that's compatible with the explicit nature of Oak.

  In particular, an Oracle operates in stages. During the 'respond' stage,
  the Oracle answers queries to the best of its ability atop a fixed 'state'
  value. After the 'respond' stage the Oracle gets a chance to have a 'research'
  stage updating the 'state' value in knowledge of all of the queries it
  received during the 'respond' stage.

  Notably, an Oracle must usually respond even before doing any research such
  that asynchronous Oracles will probably return empty responses at first.
  Importantly, the 'respond' stage must be completely pure---no side effects
  allowed! All of the side effects occur during the 'research' phase offering a
  mechanism for asynchronous data loading.")


