(ns transerver.core
  (:import java.net.InetSocketAddress
           java.nio.ByteBuffer
           [java.nio.channels SelectionKey Selector ServerSocketChannel SocketChannel]))

(defn -main [& args]
  (println "Hello, World!")
  (let [selector (Selector/open)
        server (doto (ServerSocketChannel/open) (.configureBlocking false))
        buffer (ByteBuffer/allocate 2048)]
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
                            (if (pos? (.remaining buf)) (recur) (.interestOps k SelectionKey/OP_READ)))
                         ))
                    :else nil)))
                    (recur))))
