(ns newseasons.models.shows
  (:use newseasons.settings)
  (:use newseasons.models.keys)
  (:use [aleph.redis :only (redis-client)]))


(def r (redis-client {:host "localhost" :password redis-pass}))

; "Schema" --------------------------------------------------------------------
;
; Shows are stored as hashes.
;
; shows:<iTunes artist ID> = {
;     id: show id
;     title: show tile
;     latest: description of the latest season
;     release-date: release date of the latest season
;     image: url to show's image
;     url: url to view the show on iTunes
; }
;
; The shows we need to check are stored in a set:
;
; shows:to-check = #{<iTunes artist ID>, ...}
;
; All current version IDs for shows are stored as a hash:
;
; shows:versions = {
;     <iTunes artist ID>: <iTunes collection release date>,
;     ...
; }

; Code ------------------------------------------------------------------------

(defn show-get [id]
  (let [show (apply hash-map @(r [:hgetall (key-show id)]))]
    (when (not (empty? show))
      {:id (show "id")
       :title (show "title")
       :image (show "image")
       :latest (show "latest")
       :release-date (show "release-date")
       :url (show "url")})))

(defn show-set-id! [id new-id]
  @(r [:hset (key-show id) "id" new-id]))

(defn show-set-title! [id new-title]
  @(r [:hset (key-show id) "title" new-title]))

(defn show-set-latest! [id new-latest]
  @(r [:hset (key-show id) "latest" new-latest]))

(defn show-set-release-date! [id new-release-date]
  @(r [:hset (key-show id) "release-date" new-release-date]))

(defn show-set-image! [id new-image]
  @(r [:hset (key-show id) "image" new-image]))

(defn show-set-url! [id new-url]
  @(r [:hset (key-show id) "url" new-url]))


(defn show-get-version [id]
  @(r [:hget "shows:versions" id]))

(defn show-set-version-maybe! [id release-date]
  @(r [:hsetnx "shows:versions" id release-date]))

(defn show-set-version! [id release-date]
  @(r [:hset "shows:versions" id release-date]))


(defn show-get-watchers [show-id]
  @(r [:smembers (key-show-watchers show-id)]))

(defn show-add-to-check! [id]
  @(r [:sadd "shows:to-check" id]))

(defn shows-get-to-check []
  @(r [:smembers "shows:to-check"]))


(defn store-raw-show [show]
  (let [id (show "artistId")]
    (show-set-id! id id)
    (show-set-title! id (show "artistName"))
    (show-set-latest! id (show "collectionName"))
    (show-set-release-date! id (show "releaseDate"))
    (show-set-image! id (show "artworkUrl100"))
    (show-set-url! id (show "artistViewUrl"))))

(defn store-raw-shows [seasons]
  (dorun (map store-raw-show seasons)))
