(ns ethlance.ui.event.templates
  (:require district.parsers))

(defn create-set-feedback-min-rating
  "Event FX Handler. Set the current feedback min rating.

   # Notes

   - If the min rating is higher than the max rating, the max rating will also be adjusted appropriately."
  [state-key]
  (fn [{:keys [db]} [_ new-min-rating]]
    (let [new-min-rating (district.parsers/parse-int new-min-rating)
          current-max-rating (-> db (get-in [state-key :feedback-max-rating]) district.parsers/parse-int)
          new-max-rating (max new-min-rating current-max-rating)]
      {:db (-> db
               (assoc-in [state-key :feedback-min-rating] new-min-rating)
               (assoc-in [state-key :feedback-max-rating] new-max-rating))})))


(defn create-set-feedback-max-rating
  "Creates an Event FX Handler. Sets the current feedback max rating with respect to the given `state-key`.

   # Notes

   - If the max rating is lower than the min rating, the min rating will also be adjusted appropriately."
  [state-key]
  (fn [{:keys [db]} [_ new-max-rating]]
    (let [new-max-rating (district.parsers/parse-int new-max-rating)
          current-min-rating (-> db (get-in [state-key :feedback-min-rating]) district.parsers/parse-int)
          min-rating (min new-max-rating current-min-rating)]
      {:db (-> db
               (assoc-in [state-key :feedback-max-rating] new-max-rating)
               (assoc-in [state-key :feedback-min-rating] min-rating))})))


(defn create-set-min-hourly-rate
  "Creates an Event FX Handler. Set the current mininum hourly rate with respect to the given `state-key`.

   # Notes

   - If the min hourly rate is higher than the max hourly rate, the max hourly rate will also be adjusted appropriately."
  [state-key]
  (fn [{:keys [db]} [_ new-min-hourly-rate]]
    (let [new-min-hourly-rate (district.parsers/parse-float new-min-hourly-rate)
          current-max-hourly-rate (-> db (get-in [state-key :max-hourly-rate]) district.parsers/parse-float)
          new-max-hourly-rate (when current-max-hourly-rate (max new-min-hourly-rate current-max-hourly-rate))]
      {:db (-> db
               (assoc-in [state-key :min-hourly-rate] new-min-hourly-rate)
               (assoc-in [state-key :max-hourly-rate] new-max-hourly-rate))})))


(defn create-set-max-hourly-rate
  "Creates an Event FX Handler. Set the current maximum hourly rate with respect to the given `state-key`.

   # Notes

   - If the max hourly rate is lower than the min hourly rate, the min hourly rate will also be adjusted appropriately."
  [state-key]
  (fn [{:keys [db]} [_ new-max-hourly-rate]]
    (let [new-max-hourly-rate (district.parsers/parse-float new-max-hourly-rate)
          current-min-hourly-rate (-> db (get-in [state-key :min-hourly-rate]) district.parsers/parse-float)
          new-min-hourly-rate (when current-min-hourly-rate (min new-max-hourly-rate current-min-hourly-rate))]
      {:db (-> db
               (assoc-in [state-key :min-hourly-rate] new-min-hourly-rate)
               (assoc-in [state-key :max-hourly-rate] new-max-hourly-rate))})))
