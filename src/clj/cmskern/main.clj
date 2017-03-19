(ns cmskern.main
  (:require
   [environ.core :refer [env]]
   [ring.middleware.reload :as reload]
   [org.httpkit.server :as http-kit]
   [clojure.string :as str]
   [clojure.java.shell :as shell]
   [taoensso.timbre :as log]
   [taoensso.tufte :as tufte]
   [mount.core :as mount :refer [defstate]]

   [clojure.tools.nrepl.server :as nrepl]
   [cider.nrepl :refer [cider-nrepl-handler]]

   [cmskern.ui.core :refer [app]]
   [cmskern.ui.websockets :as ws]
   ;; starting the biz-logic components
   [cmskern.handler]
   [conqueress.commands]
   )


  (:gen-class))


(defn with-free-port!
  ""
  [port bind-port!-fn & {:keys [max-attempts sleep-ms]
                         :or {max-attempts 50
                              sleep-ms 150}}]
  (let [binder-pid (str/trim (:out (shell/sh "lsof" "-t" "-sTCP:LISTEN"
                                             (str "-i:" port))))]
    (when-not (str/blank? binder-pid)
      (log/warn "Attempting to kill process" binder-pid "to free port" port)
      (let [kill-resp (shell/sh "kill" binder-pid)]
        (when-not (= (:exit kill-resp) 0)
          (throw (Exception. (str "Failed to kill process " binder-pid
                                  " while trying to free port " port ": "
                                  (:err kill-resp)))))))
    (loop [attempt 1]
      (when (> attempt max-attempts)
        (throw (Exception. (str "Failed to bind to port " port " within "
                                max-attempts " attempts ("
                                (* max-attempts sleep-ms) "ms)"))))
      (let [result (try (bind-port!-fn {:port (Integer/parseInt port)}) (catch java.net.BindException _))]
        (if result
          (do (log/info (str "Bound to port " port " after "
                                attempt " attempt(s)"))
              result)
          (do (Thread/sleep sleep-ms)
              (recur (inc attempt))))))))

(defn- die-with "Terminates app after logging given message."
  ([message] (log/fatal message) (System/exit 1))
  ([exception message] (log/fatal exception message) (System/exit 1)))

(defn- start-server-or-die!
  [port start-server!-fn]
  (when port
    (log/info (str "Attempting to start " " server on port " port))
    (try
      (with-free-port! port start-server!-fn)
      (catch Exception e
        (die-with e (str "Failed to start " " server"))))))



;;;;;;;;;;;;;;;
(defn start-nrepl
  "Start a network repl for debugging on specified port followed by
  an optional parameters map. The :bind, :transport-fn, :handler,
  :ack-port and :greeting-fn will be forwarded to
  clojure.tools.nrepl.server/start-server as they are."
  [{:keys [port bind transport-fn handler ack-port greeting-fn]}]
  (try
    (log/info "starting nREPL server on port" port)
    (nrepl/start-server :port port
                        :bind bind
                        :transport-fn transport-fn
                        :handler handler
                        :ack-port ack-port
                        :greeting-fn greeting-fn)

    (catch Throwable t
      (log/error t "failed to start nREPL")
      (throw t))))

(defn stop-nrepl [server]
  (nrepl/stop-server server)
  (log/info "nREPL server stopped"))

(defstate ^{:on-reload :noop}
  repl-server
  :start
  (when-let [nrepl-port (env :nrepl-port)]
    (start-nrepl {:port (Integer/parseInt nrepl-port) :handler cider-nrepl-handler}))
  :stop
  (when repl-server
    (stop-nrepl repl-server)))

;;;;;;;;;;;;;;;

(defn parse-port [args]
  (Integer/parseInt
   (if-let [port (->> args (remove #{"-dev"}) first)]
     port
     (or (env :port) "3000"))))

(defn stop-app []
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (shutdown-agents))

(defn start-app []
  (doseq [component (:started (mount/start))]
    (log/info component "started"))
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))

(defn- color-out [{:keys [level ?err #_vargs msg_ ?ns-str hostname_
                          timestamp_ ?line]}]

  ;; <timestamp> <hostname> <LEVEL> [<ns>] - <message> <throwable>
  (let [color ({:info "32" :warn "33" :error "31" :fatal "35" :report "34" :debug "0" :trace "36"} level)]
    (format "\u001b[37m%s %s [%s] - \u001b[%sm%s \u001b[0m"
            (force timestamp_)
            (-> level name str/upper-case)
            ?ns-str color (or (force msg_) ""))))

(defn logger-handler
  [m]
  (let [{:keys [stats-str_ ?id ?data]} m
        stats-str (force stats-str_)]
    (log/debug 
     (when ?id   (str "\nid: "   ?id))
     (when ?data (str "\ndata: " ?data))
     "\n"
     stats-str)))

(defn init
  []
  (log/info ::initializing)
  (reset! conqueress.commands/registry cmskern.handler/commands-registry)
  (tufte/add-handler! :logger-handler logger-handler)
  (log/merge-config!
   {:output-fn color-out}))

(defstate ^{:on-reload :noop} server
  :start (do (init)
             (start-server-or-die! (or (env :port) "3000") (partial http-kit/run-server (app ws/sente-socket))))
  :stop (server))


(defn -main [& args]
  (comment
    (doseq [u (-> (ClassLoader/getSystemClassLoader)
                  .getURLs)]
      (println "url " (.getFile u))))
  (init)
  (start-app))
