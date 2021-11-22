(ns plug-pagination.core
  (:require [re-frame.core :as rf]))

;|-------------------------------------------------
;| DEFINITIONS

(def STORE-KEY ::config)                                    ;; Where in re-frame's app-db to store data

(def ^:private defaults
  {:allow-jump?            true
   :allow-set-per-page?    true
   :allowed-items-per-page [10 20 50 100 200]})


;|-------------------------------------------------
;| CALCULATIONS

(defn calc-pagination [content {:keys [current-page items-per-page] :as pagination-data
                                :or   {current-page 1}}]
  {:pre [(sequential? content) (map? pagination-data)]}
  (let [total-items  (count content)
        max-pages    (.ceil js/Math (/ total-items items-per-page))
        total-items  (count content)
        current-page (or (min current-page max-pages) 1)
        drop-count   (or (* items-per-page (dec current-page)) 0)
        jump-length  (.floor js/Math (/ max-pages 4))
        new-data     {:total-items       total-items
                      :page-count        max-pages
                      :current-page      current-page
                      :jump-back-page    (max 1 (- current-page jump-length))
                      :jump-forward-page (min max-pages (+ current-page jump-length))
                      :prev-page         (max 1 (dec current-page))
                      :last-page         max-pages
                      :next-page         (min max-pages (inc current-page))
                      :first-page        1
                      :at-first?         (= current-page 1)
                      :at-last?          (= current-page max-pages)
                      :content           (or
                                           (some->> content
                                                    (drop drop-count)
                                                    (take (js/parseInt items-per-page)))
                                           [])}]
    (merge
      pagination-data
      new-data)))


(defn calc-pagination-for-re-frame-subscription
  "Helper for making subscription to paginated data nice and simple.

  Intended use:
  (rf/reg-sub
  :my/paginated-data
  :<-[:your/raw-data]
  :<-[::pag/config :some-id]
  calc-pagination-for-re-frame-subscription)

  Assumptions in the above example:
   - ns has (:require [plug-pagination.core :as pag])
   - ':some-id' registered with ::pag/register-config
   - ':some-id' unique to this utilization of plug-pagination"
  [[entries pagination-settings]]
  (calc-pagination entries pagination-settings))


;|-------------------------------------------------
;| HELPERS

(defn- assemble-initial-config
  "Assemble the config based on defaults and what the user provided to ensure we get something valid"
  [defaults user-config]
  (let [{:keys [items-per-page allowed-items-per-page] :as cfg} (merge defaults user-config)]
    (cond-> cfg
            (not items-per-page) (assoc :items-per-page (first allowed-items-per-page)))))


;|-------------------------------------------------
;| EVENTS - FOR CONFIG

(rf/reg-event-db
  ::register-config
  [rf/trim-v]
  (fn [db [{:keys [id] :as config}]]
    (assert (:id config) (str "config to re-frame event " ::register-config " must include :id key"))
    (->> config
         (assemble-initial-config defaults)
         (update-in db [STORE-KEY id] merge))))             ;; Update + merge to treat a potential non-sync init a bit nicer, only overwriting what we need.


(rf/reg-sub
  ::config
  (fn [db [_ id]]
    (get-in db [STORE-KEY id] {:id id})))


;|-------------------------------------------------
;| EVENTS - INTERNAL

(rf/reg-event-db
  ::set-page
  [rf/trim-v]
  (fn [db [id page]]
    (assoc-in db [STORE-KEY id :current-page] page)))


(rf/reg-event-db
  ::set-item-per-page
  [rf/trim-v]
  (fn [db [id page]]
    (assoc-in db [STORE-KEY id :items-per-page] page)))


;|-------------------------------------------------
;| UI

(defn- icon [name & {:keys [tooltip]}]
  [:span.icon>i.material-icons {:title tooltip}
   name])


(defn pagination-controls
  "Presents content in a pagination wrapper, where 'content' is a collection of hiccup forms.

  Options:
  :id                       -- Identifier needed to keep data for different pagination applications apart.
  :allowed-items-per-page   -- coll of pos-ints being the alternative item counts per page.
  :items-per-page           -- Default number of entries per page. Must be part of :allowed-items-per-page.
  :allow-jump?              -- Show '<<' '>>' controls to go more than one page forwards/backwards
  :allow-set-per-page?      -- Show UI control for choosing number of entries per page?
  "
  [{:keys [id
           allowed-items-per-page
           allow-jump?
           allow-set-per-page?
           items-per-page
           page-count
           total-items
           first-page
           jump-back-page
           prev-page
           current-page
           next-page
           jump-forward-page
           last-page
           at-first?
           at-last?]
    :as   opts}]
  {:pre [(map? opts)]}
  [:div
   [:div.level {:style {:cursor "pointer"}}
    [:div.level-left
     [:div.level-item
      {:on-click #(rf/dispatch [::set-page id first-page]) :class (when at-first? "disabled")}
      [icon "first_page" :tooltip "Show first page"]]
     (when allow-jump?
       [:div.level-item
        {:on-click #(rf/dispatch [::set-page id jump-back-page]) :class (when at-first? "disabled")}
        [icon "keyboard_double_arrow_left" :tooltip "Jump backwards"]])
     [:div.level-item
      {:on-click #(rf/dispatch [::set-page id prev-page]) :class (when at-first? "disabled")}
      [icon "navigate_before" :tooltip "Previous page"]]
     [:div.level-item
      {:class "disabled"}
      [:span (str current-page "/" page-count)]]
     [:div.level-item
      {:on-click #(rf/dispatch [::set-page id next-page]) :class (when at-last? "disabled")}
      [icon "navigate_next" :tooltip "Next page"]]
     (when allow-jump?
       [:div.level-item
        {:on-click #(rf/dispatch [::set-page id jump-forward-page]) :class (when at-last? "disabled")}
        [icon "keyboard_double_arrow_right" :tooltip "Jump forwards"]])
     [:div.level-item
      {:on-click #(rf/dispatch [::set-page id last-page]) :class (when at-last? "disabled")}
      [icon "last_page" :tooltip "Show last page"]]]

    (when allow-set-per-page?
      [:div.level-right
       [:div.level-item
        [:span total-items " entries "]
        [:div.select
         [:select {:style     {:width "10rem"}
                   :value     (or items-per-page "")
                   :on-change #(rf/dispatch [::set-item-per-page id (-> % .-target .-value)])}
          (for [number-of-items allowed-items-per-page]
            ^{:key number-of-items}
            [:option {:value number-of-items} (str number-of-items " / page")])]]]])]])