(ns ethlance.ui.event.utils)


(defn create-assoc-handler
  "Creates an Event handler that associates a value stored in the
  re-frame db `state-key` map, with the given `key`.

  # Notes

  - `transform-fn` is an optional transform performed on the first
  argument of the handled dispatched event.

  - It is assumed that the first argument after the event name is the
  new associated value

  # Examples

  We want to store the `:min-num-feedbacks` within the `:page.jobs` map

  (require '[re-frame.core :as re])
  (re/reg-event-fx :page.jobs/set-min-num-feedbacks
                   (create-assoc-handler :page.jobs :min-num-feedbacks))

  If the value being passed to the event handler is a string, transform it.

  (re/reg-event-fx :page.jobs/set-min-num-feedbacks
                   (create-assoc-handler :page.jobs :min-num-feedbacks js/parseInt))

  The Event handler manipulates the db state {:page.jobs {:min-num-feedbacks (transform-fn <value>)}}}

  "
  [state-key key & [transform-fn]]
  (fn [{:keys [db]} [_ & args]]
    (assert (= (count args) 1) (str "Event Dispatch includes more than one argument (" (count args) ")"))
    (let [arg (first args)
          value (if transform-fn (transform-fn arg) arg)]
      {:db (assoc-in db [state-key key] value)})))
