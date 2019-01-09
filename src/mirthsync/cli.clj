(ns mirthsync.cli
  (:require [clojure.string :as string]
            [clojure.tools
             [cli :refer [parse-opts]]])
  (:import java.net.URL))

(def verbosity 0)

(defn output
  "Print the message if the verbosity level is high enough"
  ([message]
   (output 0 message))
  ([level message]
   (when (<= level verbosity) (println message))))

(def cli-options
  [["-s" "--server SERVER_URL" "Full HTTP(s) url of the Mirth Connect server"
    :default "https://localhost:8443/api"
    :validate-fn #(URL. %)]
   
   ["-u" "--username USERNAME" "Username used for authentication"
    :default "admin"]

   ["-p" "--password PASSWORD" "Password used for authentication"
    :default ""]

   ["-f" "--force" "Overwrite any conflicting files in the target directory"]

   ["-t" "--target TARGET_DIR" "Base directory used for pushing or pulling files"
    :default "."]

   ["-v" nil "Verbosity level; may be specified multiple times to increase value"
    :id :verbosity
    :default 0
    :assoc-fn (fn [m k _] (update-in m [k] inc))]

   ["-h" "--help"]])

(defn usage [errors summary]
  (str
   (when errors (str "The following errors occurred while parsing your command:\n\n"
                     (string/join \newline errors)
                     "\n\n"))
   (string/join \newline
                ["Usage: mirthsync [options] action"
                 ""
                 "Options:"
                 summary
                 ""
                 "Actions:"
                 "  push     Push local code to remote"
                 "  pull     Pull remote code to local"])))

(defn config
  "Parse the CLI arguments and construct a map representing selected
  options and action with sensible defaults provided if
  necessary."
  [args]
  (let [config (parse-opts args cli-options)

        ;; pull options and first arg into top level
        config (-> config 
                   (into (:options config))
                   (dissoc :options)
                   (assoc :action (first (:arguments config))))

        args-valid? (or (:help config)
                        (and (= 1 (count (:arguments config)))
                             (#{"pull" "push"} (first (:arguments config)))))

        ;; Set up our exit code
        config (assoc config :exit-code
                      (if (or (:errors config)
                              (not args-valid?))
                        1
                        0))
        ;; exit message if errors
        config (assoc config :exit-msg (when (or (> (:exit-code config) 0)
                                                 (:help config))
                                         (usage (:errors config) (:summary config))))
        ;; keep config clean by removing unecessary entries
        config (dissoc config :summary :arguments)]

    (def verbosity (:verbosity config))
    
    config))
