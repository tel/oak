(ns irony.bootstrap
  (:require
    [quiescent.factory :as f :include-macros true]
    [cljsjs.react-bootstrap]))

(declare
  Accordion Affix AffixMixin Alert BootstrapMixin Badge Button ButtonGroup ButtonToolbar
  Carousel CarouselItem Col CollapsableMixin DropdownButton DropdownMenu
  DropdownStateMixin FadeMixin Glyphicon Grid Input Interpolate Jumbotron Label
  ListGroup ListGroupItem MenuItemModal Nav Navbar NavItem ModalTrigger OverlayTrigger
  OverlayMixin PageHeader Panel PanelGroup PageItem Pager Popover ProgressBar Row
  SplitButton SubNav TabbedArea Table TabPane Tooltip Well)

(f/def-factories js/ReactBootstrap
  Accordion Affix AffixMixin Alert BootstrapMixin Badge Button ButtonGroup ButtonToolbar
  Carousel CarouselItem Col CollapsableMixin DropdownButton DropdownMenu
  DropdownStateMixin FadeMixin Glyphicon Grid Input Interpolate Jumbotron Label
  ListGroup ListGroupItem MenuItemModal Nav Navbar NavItem ModalTrigger OverlayTrigger
  OverlayMixin PageHeader Panel PanelGroup PageItem Pager Popover ProgressBar Row
  SplitButton SubNav TabbedArea Table TabPane Tooltip Well)

