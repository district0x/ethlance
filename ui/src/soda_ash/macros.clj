(ns soda-ash.macros)


(def semantic-ui-react-tags
  ; List from https://github.com/Semantic-Org/Semantic-UI-React/blob/master/src/index.js
  '[
    Confirm
    Pagination
    PaginationItem
    Portal
    PortalInner
    Radio
    Select
    TextArea
    TransitionablePortal

    ; Collections
    Breadcrumb
    BreadcrumbDivider
    BreadcrumbSection

    Form
    FormButton
    FormCheckbox
    FormDropdown
    FormField
    FormGroup
    FormInput
    FormRadio
    FormSelect
    FormTextArea

    Grid
    GridColumn
    GridRow

    Menu
    MenuHeader
    MenuItem
    MenuMenu

    Message
    MessageContent
    MessageHeader
    MessageItem
    MessageList

    Table
    TableBody
    TableCell
    TableFooter
    TableHeader
    TableHeaderCell
    TableRow

    ; Elements
    Button
    ButtonContent
    ButtonGroup
    ButtonOr

    Container

    Divider

    Flag

    Header
    HeaderContent
    HeaderSubheader

    Icon
    IconGroup

    Image
    ImageGroup

    Input

    Label
    LabelDetail
    LabelGroup

    List
    ListContent
    ListDescription
    ListHeader
    ListIcon
    ListItem
    ListList

    Loader

    Placeholder
    PlaceholderHeader
    PlaceholderImage
    PlaceholderLine
    PlaceholderParagraph

    Rail

    Reveal
    RevealContent

    Segment
    SegmentGroup
    SegmentInline

    Step
    StepContent
    StepDescription
    StepGroup
    StepTitle

    ; Modules
    Accordion
    AccordionAccordion
    AccordionContent
    AccordionPanel
    AccordionTitle

    Checkbox

    Dimmer
    DimmerDimmable
    DimmerInner

    Dropdown
    DropdownDivider
    DropdownHeader
    DropdownItem
    DropdownMenu
    DropdownSearchInput
    DropdownText

    Embed

    Modal
    ModalActions
    ModalContent
    ModalDescription
    ModalDimmer
    ModalHeader

    Popup
    PopupContent
    PopupHeader

    Progress

    Rating
    RatingIcon

    Search
    SearchCategory
    SearchCategoryLayout
    SearchResult
    SearchResults

    Sidebar
    SidebarPushable
    SidebarPusher

    Sticky

    Tab
    TabPane

    Transition
    TransitionGroup

    ; Views
    Advertisement

    Card
    CardContent
    CardDescription
    CardGroup
    CardHeader
    CardMeta

    Comment
    CommentAction
    CommentActions
    CommentAuthor
    CommentAvatar
    CommentContent
    CommentGroup
    CommentMetadata
    CommentText

    Feed
    FeedContent
    FeedDate
    FeedEvent
    FeedExtra
    FeedLabel
    FeedLike
    FeedMeta
    FeedSummary
    FeedUser

    Item
    ItemContent
    ItemDescription
    ItemExtra
    ItemGroup
    ItemHeader
    ItemImage
    ItemMeta

    Statistic
    StatisticGroup
    StatisticLabel
    StatisticValue

    ])


(def reserved-tags #{"Comment"
                     "List"})


(println ">>> ETHLANCE semantic-ash evaluated")
(defn create-semantic-ui-react-component [tag]
  (let [tag-name (if (reserved-tags (name tag))
                   (-> tag name (str "SA") symbol)
                   tag)]
    `(def ~tag-name (reagent.core/adapt-react-class
                     (aget js/semanticUIReact ~(name tag))))))


(defmacro export-semantic-ui-react-components []
  `(do ~@(map create-semantic-ui-react-component
              semantic-ui-react-tags)))
