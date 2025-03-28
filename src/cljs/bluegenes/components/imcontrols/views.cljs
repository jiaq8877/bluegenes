(ns bluegenes.components.imcontrols.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [oops.core :refer [oget]]))

(defn organism-dropdown
  "Creates a dropdown of known organisms. The supplied :on-change function will
  receive all attributes of the organism selected.
  Options {}:
    :selected-value (optional) Supply this if you want to change the dropdown's selected-value
    :on-change Function to call when changed
  Example usage:
    [im-controls/organism-dropdown
     {:selected-value     (if-let [sn (get-in @app-db [:my-tool :selected-organism :shortName])]
                   sn \"All Organisms\")
      :on-change (fn [organism]
                   (dispatch [:mytool/set-selected-organism organism]))}]"
  []
  (let [organisms (subscribe [:cache/organisms])]
    (fn [{:keys [selected-value on-change organisms-pred]}]
      [:div.btn-group.organism-dropdown
       [:button.btn.dropdown-toggle
        {:data-toggle "dropdown"}
        [:span (if selected-value (str selected-value " ") "All Organisms ") [:span.caret]]]
       (-> [:ul.dropdown-menu]
           (into [[:li [:a.clear {:on-click (partial on-change nil)}
                        [:svg.icon.icon-close [:use {:xlinkHref "#icon-close"}]] " Clear"]]
                  [:li.divider]
                  (when (empty? @organisms)
                    [:li [:a {:disabled true}
                          "Failed to query organisms from mine"]])])
           (into (map (fn [organism]
                        [:li [:a
                              {:on-click (partial on-change organism)}
                              (:shortName organism)]])
                      (sort-by :shortName
                               (cond->> @organisms
                                 organisms-pred (filter organisms-pred))))))])))

(defn select-organism []
  (let [organisms        (subscribe [:cache/organisms])
        default-organism (subscribe [:mine-default-organism])]
    (fn [{:keys [value on-change disabled class]}]
      [:div.form-group.organism-selector
       (into [:select.form-control
              {:value (if disabled "" (or value @default-organism ""))
               :disabled disabled
               :class class
               :on-change (fn [e]
                            (on-change (oget e :target :value)))}]
             (concat
              [[:option {:value ""} "Any"]
               [:option {:value "_" :disabled true} "---"]
               (when (empty? @organisms)
                 [:option {:value "_" :disabled true} "Failed to query organisms from mine"])]
              (map (fn [{short-name :shortName}]
                     [:option {:value short-name} short-name]) @organisms)))])))

(defn sort-classes [classes]
  (sort-by (comp :displayName second) < classes))

(defn select-type []
  (let [model        (subscribe [:current-model])
        current-mine (subscribe [:current-mine])]
    (fn [{:keys [value on-change qualified?]}]
      ; when qualified? is true, only show intermine object types
      ; that have class keys
      (let [{default-types :default-object-types
             class-keys :class-keys} @current-mine]
        [:div.form-group
         [:select.form-control
          {:value (or value (-> default-types first name))
           :on-change (fn [e] (on-change (oget e :target :value)))}
          (into
           [:optgroup {:label "Popular"}]
           (map
            (fn [[class-kw {:keys [name displayName]}]]
              [:option {:value name} displayName])
            (sort-classes (select-keys (:classes @model) default-types))))
          (into
           [:optgroup {:label "All classes"}]
           (map
            (fn [[class-kw {:keys [name displayName]}]]
              [:option {:value name} displayName])
            (sort-classes
             (apply dissoc
                    (cond-> (:classes @model)
                      qualified? (select-keys (keys class-keys)))
                    default-types))))]]))))

(defn object-type-dropdown []
  (let [display-names @(subscribe [:model])]
    (fn [{:keys [values selected-value on-change]}]
      [:div.btn-group.object-type-dropdown
       [:button.btn.btn-primary.dropdown-toggle
        {:data-toggle "dropdown"}
        [:span (if selected-value
                 (str (get-in display-names [selected-value :displayName])
                      " ") "Select a type")
         [:span.caret]]]
       (-> [:ul.dropdown-menu]
           (into (map (fn [value]
                        [:li [:a
                              {:on-click (partial on-change value)}
                              (get-in display-names [value :displayName])]])
                      values)))])))
