(ns reagent-keybindings.keyboard
  (:require [reagent.core :as r]
            [goog.events :as events]
            [clojure.string :as s])
  (:import [goog.events EventType]))


(def modifiers
  (reduce (fn [m [k v]]
            (assoc m k v))
          { "shift" 16
            "alt" 18
            "option" 18
            "ctrl" 17
            "control" 17
            "cmd" 91
            "command" 91}
          (for [k (range 1 20)] [(str "f" k) (+ 111 k)])))


;; We include the mouse buttons, but will most likely never be used.
;; (Contrary to a keybinding, we often click directly on the item on
;; which we want to apply an action.)
(def mouse-buttons
  {"mouse0" :m0
   "mouseleft" :m0
   "mouse2" :m2
   "mouseright" :m2
   "mouse1" :m1
   "mousemiddle" :m1})


(def special-ks
  { "backspace" 8
    "tab" 9
    "clear" 12
    "enter" 13
    "return" 13
    "esc" 27
    "escape" 27
    "space" 32
    "left" 37
    "up" 38
    "right" 39
    "down" 40
    "del" 46
    "delete" 46
    "home" 36
    "end" 35
    "pageup" 33
    "pagedown" 34
    "," 188
    "." 190
    "/" 191
    "`" 192
    "-" 189
    "=" 187,
    ";" 186
    "'" 222
    "[" 219
    "]" 221
    "\\" 220})


(defn- get-keycode
  "Return the keycode (number) of the key given as a string."
  [key]
  (or (get special-ks key)
      (get modifiers key)
      (get mouse-buttons key)
      (.charCodeAt (.toUpperCase key) 0)))
;; keyCode will give us the code the for the uppercase letter


(defn- string-to-keys*
  "Convert string representation of shortcuts to their map equilavent.

  Modifiers are separated from the main key by a space or a dash.
  For example : 
  \"ctrl-a\" or \"ctrl a\""
  [kb-string]
  (let [keys (-> (s/lower-case kb-string)
                 (s/replace #"--| -" "-dash")
                 (s/split #" |-")
                 ((fn [string]
                    (map #(s/replace % "dash" "-") string))))]
    (->> (for [k (map get-keycode keys)]
           (cond
             (= 16 k) [:shift true]
             (= 17 k) [:ctrl true]
             (= 18 k) [:alt true]
             :else [:keycode k]))
         (into {}))))

(def string-to-keys (memoize string-to-keys*))

;;;;



(def preventing-default-keys
  "Prevent the default action for these keys."
  (atom [{:keycode 82 :ctrl true} ;; ctrl r ---> we never want our user to
         ;; reload by accident
         {:keycode 83 :ctrl true} ;; ctrl s ---> don't save the HTML page
         ]))


(defonce keyboard-state
  (r/atom {:keycode nil
           :shift nil
           :ctrl nil
           :alt nil}))


(defonce deactivate-shortcuts-comps (atom []))


(defn shortcuts-active?
  "Return true if keyboard and mouse shortcuts are active."
  []
  (not (seq @deactivate-shortcuts-comps)))



(defonce registered-keys (atom {}))


(defn register-keys!
  "Register a shortcut. If multiple shortcuts have the same keys,
  only the most recently added will be active. Re-registering the same
  keys and ID combination will update the action function without
  changing the order."
  [shortcut-string id action-fn]
  (let [keys-map (string-to-keys shortcut-string)]
    (swap! registered-keys update-in [keys-map]
           (fn [action-coll]
             (if (some #(= (:id %) id) action-coll)
               ;; ID already registered
               (vec (for [entry action-coll]
                      (if (= id (:id entry))
                        (assoc entry :action-fn action-fn)
                        entry)))
               ;; New ID
               (conj (or action-coll [])
                     {:id id
                      :action-fn action-fn}))))))


(defn deregister-keys! [shortcut-string id]
  (let [keys-map (string-to-keys shortcut-string)]
    (swap! registered-keys update-in [keys-map]
           (fn [action-coll]
             (vec (remove #(= (:id %) id) action-coll))))))


(defn get-keys-action
  "Return the keys action, if any."
  [keys-map]
  (when (shortcuts-active?)
    (-> (get-in @registered-keys [keys-map])
        (peek)
        :action-fn)))


(defn evt-modifiers
  "Return the keyboard modifiers associated with this event."
  [evt]
  {:shift (.-shiftKey evt)
   :ctrl (.-ctrlKey evt)
   :alt (.-altKey evt)})

(defn key-up! [evt]
  (let [keycode (.-keyCode evt)
        mods (evt-modifiers evt)]
    (swap! keyboard-state merge mods
           (when (= keycode (:keycode @keyboard-state))
             {:keycode nil}))))


(defn key-down! [evt]
  (let [keycode (.-keyCode evt)
        mods (evt-modifiers evt)
        new-state (assoc mods :keycode keycode)
        pressed-keys (into {} (filter second new-state))]
    (reset! keyboard-state new-state)
    (when-let [action (get-keys-action pressed-keys)]
      (action evt)
      (.preventDefault evt))
    ;; maybe prevent default action
    (when (some #{pressed-keys} @preventing-default-keys)
      (.preventDefault evt))))


(defn mouse-up! [evt]
  (let [button (.-button evt)
        keycode (condp = button
                  0 :m0
                  1 :m1
                  2 :m2
                  nil)
        mods (evt-modifiers evt)]
    (swap! keyboard-state merge mods
           (when (= keycode (:keycode @keyboard-state))
             {:keycode nil}))))


(defn mouse-down! [evt]
  (let [button (.-button evt)
        keycode (keyword (str "m" button))
        mods (evt-modifiers evt)
        new-state (assoc mods :keycode keycode)
        pressed-keys (into {} (filter second new-state))]
    (reset! keyboard-state new-state)
    (when-let [action (get-keys-action pressed-keys)]
      (action evt)
      (.preventDefault evt)
      (.stopPropagation evt))))






;;; API

(defn keyboard-listener
  "Component that will add the necessary events listeners to the
  window."
  []
  (r/create-class
   {:component-did-mount (fn [_]
                           (.addEventListener js/window EventType.KEYUP key-up!)
                           (.addEventListener js/window EventType.KEYDOWN key-down!)
                           (.addEventListener js/window EventType.MOUSEUP mouse-up!)
                           (.addEventListener js/window EventType.MOUSEDOWN mouse-down!))
    :component-will-unmount (fn [_]
                              (.removeEventListener js/window EventType.KEYUP key-up!)
                              (.removeEventListener js/window EventType.KEYDOWN key-down!)
                              (.removeEventListener js/window EventType.MOUSEUP mouse-up!)
                              (.removeEventListener js/window EventType.MOUSEDOWN mouse-down!))
    :reagent-render (fn [] [:span])}))


(defn kb-action
  "Component to register a shortcut. If multiple shortcuts have the same keys,
  only the most recently added will be active. Re-registering the same
  keys and ID combination will update the action function without
  changing the order.

  Modifiers in `shortcut-string` are separated from the main key by a space or a dash.
  For example : 
  \"ctrl-a\" or \"ctrl a\"

  The `keyboard-listener` component must be mounted somewhere in order
  for the shortcuts to be activated."
  ([shortcut-string kb-fn]
   (let [id (gensym "kb-")]
     (r/create-class
      {:component-did-mount (fn [_]
                              (register-keys! shortcut-string id kb-fn))
       :component-did-update (fn [_ [_ _ new-kb-fn]]
                               (when-not (= kb-fn new-kb-fn)
                                 (register-keys! shortcut-string id kb-fn)))
       :component-will-unmount (fn [_]
                                 (deregister-keys! shortcut-string id))
       :reagent-render (fn [_] [:span])}))))

(defn deactivate-kb-shortcuts
  "While mounted, kb shortcuts are completely deactivated.
  Useful when showing a form in a modal. (We wouldn't want the user to
  activate shortcuts while typing in some text field.)"
  ([]
   (let [id (gensym "deactivate-shortcuts-")]
     (r/create-class
      {:component-did-mount (fn [_]
                              (swap! deactivate-shortcuts-comps conj id))
       :component-will-unmount (fn [_]
                                 (swap! deactivate-shortcuts-comps #(remove #{id} %)))
       :reagent-render (fn [_] [:span])}))))
