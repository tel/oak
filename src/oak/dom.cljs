(ns oak.dom
  (:refer-clojure :exclude [time map meta])
  (:require
    [quiescent.factory :as factory]
    [quiescent.dom :as dm :include-macros true]
    [quiescent.dom.uncontrolled :as dm-u]))

(declare
  a abbr address area article aside audio b base bdi bdo big blockquote body br
  button canvas caption cite code col colgroup data datalist dd del details dfn
  div dl dt em embed fieldset figcaption figure footer form h1 h2 h3 h4 h5 h6
  head header hr html i iframe img input ins kbd keygen label legend li link main
  map mark menu menuitem meta meter nav noscript object ol optgroup option output
  p param pre progress q rp rt ruby s samp script section select small source
  span strong style sub summary sup table tbody td textarea tfoot th thead time
  title tr track u ul var video wbr circle g line path polygon polyline rect svg
  text)

(dm/define-tags
  a abbr address area article aside audio b base bdi bdo big blockquote body br
  button canvas caption cite code col colgroup data datalist dd del details dfn
  div dl dt em embed fieldset figcaption figure footer form h1 h2 h3 h4 h5 h6
  head header hr html i iframe img input ins kbd keygen label legend li link main
  map mark menu menuitem meta meter nav noscript object ol optgroup option output
  p param pre progress q rp rt ruby s samp script section select small source
  span strong style sub summary sup table tbody td textarea tfoot th thead time
  title tr track u ul var video wbr circle g line path polygon polyline rect svg
  text)

(def uinput (factory/factory (dm-u/uncontrolled-component "input" "input")))
(def utextarea (factory/factory (dm-u/uncontrolled-component "textarea" "textarea")))
(def uoption (factory/factory (dm-u/uncontrolled-component "option" "option")))
