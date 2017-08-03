(ns opencv3.utils
  (:use [gorilla-repl.image])
  (:require
    [opencv3.core :as cv]
    [opencv3.video :as vid]
    )
  (:import [org.opencv.core Size CvType Core Mat MatOfByte]
    [org.opencv.imgcodecs Imgcodecs]
    [org.opencv.videoio VideoCapture]
    [org.opencv.imgproc Imgproc]
    [javax.imageio ImageIO]
    [javax.swing ImageIcon JFrame JLabel]
    [java.awt.event KeyListener MouseAdapter]
    [java.awt FlowLayout]))

;;;
; CLEAN
;;;
(defn clean-up-namespace[]
  (map #(ns-unmap *ns* %) (keys (ns-interns *ns*))))

;;;
; MAT OPERATIONS
;;;
(defn resize-by[ mat factor]
  (let [height (.rows mat) width (.cols mat)]
    (cv/resize! mat (cv/new-size (* width factor) (* height factor)) )))

(defn matrix-to-mat [matrix mat array-fn]
(map-indexed
 (fn[i line]
  (map-indexed
    (fn[j val] (.put mat i j (array-fn [val])))
    line))
matrix))

(defn matrix-to-matofpoint2f[pts]
(cv/new-matofpoint2f
(into-array org.opencv.core.Point
  (map #(cv/new-point (first %) (second %)) pts))))

(defn matrix-to-mat [matrix]
  (let[
    flat (flatten matrix)
    rows (count matrix)
    cols (count (first matrix))
    mat (Mat. rows cols CvType/CV_32F)
    total (.total mat)
    bytes (float-array total)
    ]
    (doseq [^int i (range 0 total)]
      (aset-float bytes i (nth flat i)))
    (.put mat 0 0 bytes)
    mat))

(defn mat-from [src]
  (Mat. (.rows src) (.cols src) (.type src)))

(defn mat-to-buffered-image [src]
  (let[ matOfBytes (MatOfByte.)]
  (Imgcodecs/imencode ".jpg" src matOfBytes)
  (ImageIO/read
    (java.io.ByteArrayInputStream. (.toArray matOfBytes)))))

(defn buffered-image-to-mat[bi]
  (let [mat (Mat. (.getHeight bi) (.getWidth bi) (CvType/CV_8UC3))
  bytes
  (-> bi
    (.getRaster)
    (.getDataBuffer)
    (.getData)
    )]
    (.put mat 0 0 bytes)
    mat))

; points
(defn middle-of-two-points [p1 p2]
  (cv/new-point
    (/ (+ (.-x p1) (.-x p2)) 2)
    (/ (+ (.-y p1) (.-y p2)) 2)))

; gorilla repl
(defn mat-view[img]
  	(image-view (mat-to-buffered-image img)))

(defn image-from-url[url]
  (let[ connection (->  url
	(java.net.URL.)
    (.openConnection))]
    (.setRequestProperty connection
      "User-Agent"
      "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_5) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.65 Safari/537.31")
      (ImageIO/read (.getInputStream connection))))

(defn mat-from-url[url]
  (buffered-image-to-mat (image-from-url url)))

;;;
; CONTOURS
;;;
(defn draw-contours-with-rect!
([buffer contours] (draw-contours-with-rect! buffer contours false))
([buffer contours save-frame]
  (doseq [c contours]
  (let [area (cv/contour-area c)]
     (if (> area 1000)
       (let [rect (cv/bounding-rect c)]
       (if save-frame (cv/imwrite (.submat buffer rect) (str "video/" (java.util.Date.) ".png")))
       (cv/rectangle
         buffer
         (cv/new-point (.x rect) (.y rect))
         (cv/new-point (+ (.width rect) (.x rect)) (+ (.y rect) (.height rect)))
         (cv/new-scalar 255 0 0) 3))))
         buffer
         )))

(defn draw-contours-with-line! [img contours]
(dotimes [i (.size contours)]
   (let [c (.get contours i)
      m2f (cv/new-matofpoint2f (.toArray c))
      len (cv/arc-length m2f true)
      ret (cv/new-matofpoint2f)
      approx (cv/approx-poly-dp m2f ret (* 0.005 len) true)
      nb-sides (.size (.toList ret))]
; (println ">" nb-sides)
(cv/draw-contours img contours i
 (condp = nb-sides
  3  (cv/new-scalar 56.09 68.05 66.27)
  4  (cv/new-scalar 356.09 51.57 43.73)
  7   (cv/new-scalar 26.09 51.57 43.73)
  8 (cv/new-scalar 146.09 51.57 43.73)
  9 (cv/new-scalar 266.09 51.57 43.73)
  10  (cv/new-scalar 236.09 51.57 43.73)
  11 (cv/new-scalar 206.09 68.05 66.27)
  12 (cv/new-scalar 0 0 255)
     (cv/new-scalar 127 50 0))
  -1)))
 img)

;;;
; SWING
;;;

(defn re-show[pane mat]
  (let[image (.getIcon (first (.getComponents pane)))]
  (.setImage image (mat-to-buffered-image mat))
  (doto pane
    (.revalidate)
    (.repaint))))
(defn show
  ([src] (show src {}))
  ([src _options]
  (let [
    options (merge-with merge {:frame {:color 0 :title "image" :width 400 :height 400}} _options)
    is-atom? (= (class src) clojure.lang.Atom)
    buf (if is-atom? (mat-to-buffered-image @src) (mat-to-buffered-image src))
    frame (JFrame. (-> options :frame :title))
    pane (.getContentPane frame)
    image (ImageIcon. buf)
    label (JLabel. image)
    history (cv/new-arraylist)
    get-src (fn[] (buffered-image-to-mat (.getImage  (.getIcon (first (.getComponents pane))))))
    ]
    (println options)
    (doto pane
     (.setOpaque true)
     (.setPreferredSize (java.awt.Dimension. (-> options :frame :width)  (-> options :frame :height) ))
     (.setBackground (java.awt.Color. (-> options :frame :color)))
     (.setLayout (FlowLayout.))
     (.add label))
    ; (.addComponentListener frame
    ;   (proxy [java.awt.event.ComponentListener] []
    ;     (componentMoved [event])
    ;     (componentResized [event]
    ;       (re-show pane (cv/resize! (get-src) (cv/new-size (.getWidth frame) (.getHeight frame)))))))
    (.addKeyListener frame
      (proxy [KeyListener] []
        (keyTyped [event])
        (keyReleased [event])
        (keyPressed [event]
          (let [c (.getKeyCode event) handler (-> options :handlers (get c))]
          (println c ">" handler)
          (if (not (nil? handler))
             (re-show pane (handler (get-src))))
           (condp = c
             32 (if (.getClientProperty pane "paused")
                  (.putClientProperty pane "paused" false)
                  (.putClientProperty pane "paused" true))
             83 (ImageIO/write
                 (.getImage (.getIcon label))
                 "png" (clojure.java.io/as-file (str (-> options :frame :title) "_" (System/currentTimeMillis) ".png")))
             70  (let [ dsd   (->
                      (java.awt.GraphicsEnvironment/getLocalGraphicsEnvironment)
                      (.getDefaultScreenDevice)) ]
                    (if (.getClientProperty pane "fullscreen")
                    (do
                      (.putClientProperty pane "fullscreen" nil)
                      (.setFullScreenWindow dsd nil))
                    (do
                      (.putClientProperty pane "fullscreen" true)
                      (.setFullScreenWindow dsd frame))))
             81  (do
               (.putClientProperty pane "quit" true)
               (.dispose frame))
             (do)
             )))))
    (.addMouseListener label
      (proxy [MouseAdapter] []
       (mousePressed [event])))
    (if is-atom?
       (add-watch src :cat
         (fn [key ref old new-s]
           (if (empty? history) (.add history old))
           (.add history new-s)
           (re-show pane @src))))
    (doto frame
      (.setPreferredSize (java.awt.Dimension. (-> options :frame :width)  (-> options :frame :height) ))
      (.setVisible true)
      (.pack)
      (.setDefaultCloseOperation JFrame/DISPOSE_ON_CLOSE))
      pane)))


(defn simple-cam-window
  ([myvideofn] (simple-cam-window {} myvideofn ))
  ([_options myvideofn]

  (let [
    options (merge-with merge {:frame {:color 0 :title "video"} :video {:device 0 :width 200 :height 220}} _options )
    capture (vid/new-videocapture)
    window (show (cv/new-mat (-> options :video :width) (-> options :video :height)   cv/CV_8UC3 (cv/new-scalar 255 255 255)) options)
    buffer (cv/new-mat)

    ]

    (doto capture
      (.open (int (-> options :video :device)))
      (.set vid/CAP_PROP_FRAME_WIDTH (-> options :video :width))
      (.set vid/CAP_PROP_FRAME_HEIGHT (-> options :video :height)))

    (.start (Thread.
      (fn []
      (while (nil? (.getClientProperty window "quit"))
       (if (.read capture buffer)
        (if (not (.getClientProperty window "paused"))
         (re-show window (myvideofn (cv/clone buffer))))))
       (.release capture)))))))


(defn two-cams-window
 ([_options]
 (let [
   options (merge-with merge {:frame {:color 0 :title "video"}} _options )
   device1 (->  options :devices first)
   device2 (->  options :devices second)

   capture1 (vid/new-videocapture)
   capture2 (vid/new-videocapture)
   window (show (cv/new-mat 100 100 cv/CV_8UC3 (cv/new-scalar 0 0 0)) options)
   buffer-left (atom (cv/new-mat))
   buffer-right (atom (cv/new-mat))
   output (cv/new-mat)
   buffer1 (cv/new-mat)
   buffer2 (cv/new-mat)
   ]

   (doto capture1
     (.open (int (-> device1 :device)))
     (.open 0)
     (.set vid/CAP_PROP_FRAME_WIDTH (-> device1 :width))
     (.set vid/CAP_PROP_FRAME_HEIGHT (-> device1 :height)))

   (doto capture2
      (.open (int (-> device2 :device)))
     (.set vid/CAP_PROP_FRAME_WIDTH (-> device2 :width))
     (.set vid/CAP_PROP_FRAME_HEIGHT (-> device2 :height)))

   (.start (Thread.
     (fn []
     (while (nil? (.getClientProperty window "quit"))
      (if (.read capture1 buffer1)
       (if (not (.getClientProperty window "paused"))
        (reset! buffer-left ((-> device1 :fn) (cv/clone buffer1))))))
      (.release capture1))))

   (.start (Thread.
     (fn []
     (while (nil? (.getClientProperty window "quit"))
      (if (.read capture2 buffer2)
       (if (not (.getClientProperty window "paused"))
        (reset! buffer-right ((-> device2 :fn) (cv/clone buffer2))))))
      (.release capture2))))

  (.start (Thread.
    (fn []
    (while (nil? (.getClientProperty window "quit"))
      (if (not (.getClientProperty window "paused"))
          (if (not
            (or (= 0 (.cols @buffer-left))
                (= 0 (.cols @buffer-right))))
          (do
          (re-show window ((-> options :video :fn) @buffer-left @buffer-right)))))))))

      )))
