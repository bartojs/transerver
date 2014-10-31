(ns transerver.core
  (:import java.net.InetSocketAddress
           java.nio.ByteBuffer
           [java.nio.channels SelectionKey Selector ServerSocketChannel SocketChannel])
  (:require [clojure.core.async :refer [chan <! >! go dropping-buffer]]))

(def xlog (map (fn [b] (println "xlog" (String. b)) b)))
(def xxlog (map (fn [b] (println "xxlog" (String. b)) b)))

(defn -main [& args]
  (println "Hello, World!")
  (let [selector (Selector/open)
        server (doto (ServerSocketChannel/open) (.configureBlocking false))
        buffer (ByteBuffer/allocate 2048)
        reqchan (chan (dropping-buffer 10) (comp xlog xxlog))]

        (.bind (.socket server) (InetSocketAddress. "localhost" 5555))
        (.register server selector SelectionKey/OP_ACCEPT)
        (loop []
         (println "Select")
         (.select selector 20000)
         (doseq [k (.selectedKeys selector)]
           (println "Key")
           (when (.isValid k)
               (cond (.isAcceptable k)
                     (do (println "Accept")
                        (when-let [sock (.accept (.channel k))]
                            (.configureBlocking sock false)
                            (.register sock selector SelectionKey/OP_READ)))
                     (.isReadable k)
                     (do (println "Read")
                         (.clear buffer)
                         (let [sock (.channel k)
                               n (.read sock buffer)]
                            (println "read " n " bytes")
                            (go (>! reqchan (.array buffer)))
                            (if (neg? n)
                                (do (.close sock)
                                    (.cancel k))
                                (.interestOps k SelectionKey/OP_WRITE))))
                     (.isWritable k)
                     (do (println "Write")
                         (let [sock (.channel k)
                               buf (ByteBuffer/wrap (.array buffer))]
                          (loop []
                            (.write sock buf)
                            (if-not (pos? (.remaining buf))
                               (.interestOps k SelectionKey/OP_READ)
                               (recur)))))
                    :else nil)))
                    (recur))))
