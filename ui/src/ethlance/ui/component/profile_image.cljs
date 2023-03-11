(ns ethlance.ui.component.profile-image
  (:require
    [ethlance.ui.util.urls :as util.urls]))

(def placeholder-image-url "/images/avatar-placeholder.png")

(defn c-profile-image
  "Profile Image component for displaying a given user's profile image.

  # Keyword Arguments

  opts - Optional Arguments

  # Optional Arguments (opts)

  :src - The src image url of the profile image to display. [default: placeholder image].

  :size - The size of the image to display. `:small`, `:normal`,
  `:large`, [default: `:normal`].
  "
  [{:keys [src size]}]
  (let [size-class (case size
                    :small " small "
                    :normal ""
                    :large " large "
                    "")
        src-url (util.urls/ipfs-hash->gateway-url src)]
    [:div.ethlance-profile-image
     {:class size-class}
     [:img {:src (or src-url placeholder-image-url)}]]))
