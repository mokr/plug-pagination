(defproject net.clojars.mokr/plug-pagination "0.1.0-SNAPSHOT"
  :description "Pagination for re-frame and Bulma based app"
  :url "https://github.com/mokr/plug-pagination"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3" :scope "provided"]
                 [org.clojure/clojurescript "1.10.893" :scope "provided"]
                 [re-frame "1.2.0" :scope "provided"]]
  :repl-options {:init-ns plug-pagination.core}
  :profiles
  {:dev
   {:dependencies [[thheller/shadow-cljs "2.16.4"]]}})
