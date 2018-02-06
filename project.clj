(defproject habibibot "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.6.0"]
                 [ring/ring-core "1.6.3"]
                 [ring/ring-jetty-adapter "1.6.3"]
                 [ring/ring-defaults "0.3.1"]
                 [ring/ring-json "0.5.0-beta1"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/core.async "0.4.474"]
                 [amazonica "0.3.118"]
                 [org.clojure/java.jdbc "0.7.5"]
                 [org.postgresql/postgresql "42.1.4"]]
  :plugins [[lein-ring "0.12.3"]]
  :profiles {:uberjar {:aot :all}})
