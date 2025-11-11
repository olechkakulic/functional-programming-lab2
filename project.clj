(defproject functional_programming "0.1.0-SNAPSHOT"
  :description "Functional Programming Lab"
  :url "https://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.12.2"]
                 [org.clojure/test.check "1.1.1"]]
  :main ^:skip-aot problem1.main
  :target-path "target/%s"
  :plugins [[lein-cljfmt "0.8.2"]
            [lein-kibit "0.1.8"]
            [lein-bikeshed "0.5.2"]]
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :dev {:dependencies [[clj-kondo "2025.09.22"]]}}
  :aliases {"lint" ["do"
                    ["cljfmt" "check"]
                    ["kibit"]
                    ["bikeshed" "--max-line-length" "120"]
                    ["run" "-m" "clj-kondo.main" "--lint" "src" "test"]]})