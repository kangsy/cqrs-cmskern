(ns cmskern.ui.binary
  (:import org.bson.types.ObjectId)
  (:require
   [taoensso.timbre :as log]
   [image-resizer.core :as resizer]
   [image-resizer.format]
   [monger.conversion :as mconv]

   [cmskern.db :as db]
   ))

(defn scale-crop 
  [imgs [width w-attr height h-attr]]
  (log/debug "scale-crop " width w-attr height h-attr)
  (when (and imgs width height)
    (let [img-type (if (= "image/png" (.getContentType imgs)) "png" "jpg")]
      (let [istream (.getInputStream imgs)]
        (try 
          (let [image (javax.imageio.ImageIO/read istream)
                ratio (/ (.getWidth image) (.getHeight image))
                target-ratio (/ width height)]
                                        ;    (debug "ratio "  ratio " target-ratio " target-ratio)
            (image-resizer.format/as-stream
             (if (or (= w-attr :fix) (not= h-attr :crop))
               (resizer/crop 
                (cond
                  (and (= w-attr :fix) (not= h-attr :crop))
                  (resizer/resize-to-width image width)
                  (and (= h-attr :fix) (not= w-attr :crop))
                  (resizer/resize-to-height image height)
                  :else (if (> target-ratio ratio)
                          (resizer/resize-to-width image width)
                          (resizer/resize-to-height image height)
                          )
                  )
                width height)
               (cond
                 (and (= w-attr :fix) (not= h-attr :crop))
                 (resizer/resize-to-width image width)
                 (and (= h-attr :fix) (not= w-attr :crop))
                 (resizer/resize-to-height image height)
                 :else (if (> target-ratio ratio)
                         (resizer/resize-to-width image width)
                         (resizer/resize-to-height image height)
                         )
                 )
               ) img-type
             ))
          (finally (.close istream)))))))

(defn get-binary
  ""
  [dbid id format]
  (let [oid (ObjectId. id) ;(mconv/to-object-id id)
        dbfile (db/gridfs-get dbid {"_id" oid})]
    (log/debug ::get-binary dbid id oid format dbfile)
    (if (= :orig format)
      [(:contentType dbfile) (.getInputStream dbfile)]
      [(:contentType dbfile) (scale-crop dbfile format)]
      )))
