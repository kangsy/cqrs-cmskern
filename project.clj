(defproject cqrs-cmskern "0.1.0-SNAPSHOT"
  :description "General-purpose CMS."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                 ;; basics für init
                 [org.clojure/clojure "1.9.0-alpha14" :scope "provided"]
                 [org.clojure/core.async "0.2.395"]

                 [liberator "0.14.1"]
                 ;;[compojure "1.5.1"]
            
                 [danlentz/clj-uuid "0.1.6"]
                 [ring-middleware-format "0.7.0"]
                 ;;[ring/ring-core "1.5.0"]
                 [cider/cider-nrepl "0.15.0-SNAPSHOT"]
                 [ring/ring-anti-forgery "1.0.1"]
                 [http-kit "2.1.19"]
                 [org.clojure/algo.monads "0.1.6"]

                 [com.novemberain/monger "3.1.0"]
                 [clj-jwt "0.1.1"]
                 [clj-time "0.13.0"]
                 [crypto-password "0.2.0"]

                 [image-resizer "0.1.9"]

                 ;; --------------
                 [ymilky/franzy-nippy "0.0.1"]

                 [spootnik/kinsky "0.1.15"]
                 ;; --------------

                 [mount "0.1.10"]
                 [environ "1.1.0"]

                 ;; --------------
                 [garden "1.3.2"]


                 [com.kangrd/conqueress "0.1.0"]
                 [com.kangrd/garden-libs "0.1.0"]
                 ;; --------------
                 ;;profiling
                 [com.taoensso/tufte "1.1.1"]

                 ;; logging
                 [com.taoensso/timbre "4.7.4"]

                 ;; i18n
                 [com.taoensso/tempura "1.0.0"]

                 [com.taoensso/sente "1.11.0"]

                 ;; cljs -----------------------
                 [org.clojure/clojurescript "1.9.293" :scope "provided"]
                 [reagent "0.6.0"  :exclusions [cljsjs/react cljsjs/react-dom]]
                 [re-frame "0.9.1"]
                 [binaryage/devtools "0.8.3"]
                 [devcards "0.2.2"]
                 [venantius/accountant "0.1.7"]
                 [bidi "2.0.14"]
                 [cljs-ajax "0.5.8"]
                 [day8.re-frame/http-fx "0.1.3"]
                 [re-frisk "0.3.1"]
                 [com.andrewmcveigh/cljs-time "0.5.0-alpha2"]
                 ]

  ;; cljs -----------------------
  :figwheel { :css-dirs ["resources/public/css"] 
             :server-port 9017
             ;:nrepl-port 7888
             :open-file-command "emacsclient"
             ;:reload-clj-files false
             }
  :cljsbuild {
              :builds [
                       {:id "dev"
                        :source-paths ["src/cljc" "src/cljs/common" "src/cljs/dev"]
                        :figwheel {
                                        ;:devcards true
                                   :on-jsload "cmskern.core/mount-root"
                                   }
                        :compiler {
                                   :preloads [devtools.preload]
                                   :main cmskern.core
                                   :output-to "resources/public/cljs/cmskern.js"
                                   :output-dir "resources/public/cljs/out"
                                   :asset-path           "/cljs/out"
                                   :optimizations :none
                                   :externs ["node_modules/react-jsonschema-form/dist/react-jsonschema-form.js"]
                                   :source-map true}}
                       {:id "devcards"
                        :source-paths ["src/cljc" "src/cljs/common" "src/cljs/dev"]
                        :figwheel {
                                   :devcards true
                                   ;:on-jsload "cmskern.core/mount-root"
                                   }
                        :compiler {
                                   :preloads [devtools.preload]
                                   :main    cmskern-devcards.core
                                   :output-to "resources/public/cljs/cmskern_devcards.js"
                                   :externs ["node_modules/react-jsonschema-form/dist/react-jsonschema-form.js"]
                                   :output-dir "resources/public/cljs/devcards_out"
                                   :asset-path           "/cljs/devcards_out"
                                   :optimizations :none
                                   :source-map true}}
                       {:id "min"
                        :source-paths [ "src/cljc" "src/cljs/prod" "src/cljs/common"]
                        :compiler     {
                                       :main cmskern.core
                                       :output-to "resources/public/cljs/cmskern.min.js"
                                       :optimizations   :advanced
                                       :pretty-print false
                                       :pseudo-names false
                                       :closure-defines {goog.DEBUG false}
                                       }}
                       ]}
  ;; cljs end -------------------

  :min-lein-version "2.0.0"
  :plugins [
            [lein-environ "1.1.0"]]

  :jvm-opts ["-server"]
  :source-paths ["src/clj" "src/cljc"]
  :target-path "target/%s/"

  :clean-targets ^{:protect false} [:target-path [:cljsbuild :builds :app :compiler :output-dir] [:cljsbuild :builds :app :compiler :output-to]]
  :main cmskern.main
  :profiles {:uberjar {:omit-source true
                       :prep-tasks ["compile"]
                       :aot :all
                       :uberjar-name "cqrs-cmskern.jar"
                       }
             :dev           [:project/dev :profiles/dev]
             :project/dev {
                           ;; this is needed for kafka to load the serializers
                           ;; in repl this is not needed. I guess because of "alembic"
                           :aot [
                                 franzy.serialization.nippy.serializers
                                 franzy.serialization.nippy.deserializers
                                 ]
                           :dependencies [
                                          [org.clojure/test.check "0.9.0"]
                                          [javax.servlet/servlet-api "2.5"]
                                          ;; für api-test
                                          [clj-http "3.3.0"]
                                          ;; mw-reload
                                          [ring/ring-devel "1.5.0"]
                                          [stylefruits/gniazdo "1.0.0"]

                                          [devcards "0.2.2"]
                                          ]
                           :repl-options {:init-ns user}
                           :plugins      [[lein-figwheel "0.5.8"]]
                           :injections [
                                        ;(require 'pjstadig.humane-test-output)
                                        ;(pjstadig.humane-test-output/activate!)
                                        ]
                           :env {:port 9088
                                 :nrepl-port 7000}
                           }

             :profiles/dev {}
             }
  )
