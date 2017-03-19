(ns css.cmskern
  (:require
           [garden.def :refer [defstyles]]
           [garden.core :refer [css]]
           [garden.units :refer [em px]]
           [garden.stylesheet :refer [at-media at-import at-keyframes at-font-face]]
           [garden.color :as color :refer [rgb]]

           [css.fa :as fa]
           [garden-libs.purecss.core :as purecss]
           ;[garden-libs.normalize.core :as normalize]

           ))

(defn set-font-pure
  ""
  [font]
  [[:html :button :input :select :textarea ".pure-g [class*=\"pure-u\"]"
    {:font-family font}
    ]
   ])

(defn init-buttons
  ""
  [colors states]
  [(conj (vec (map #(str ".button-" %) states))
         {:color "white"
          :border-radius 0})
   (vec (map #(vector (str ".button-" (name (first %)))
                      {:background (second %)}) colors))]
  )

(defn fa-icon
  ""
  [icon]
  [:&:before {:content (str "\"" icon "\"")}])

(def font-awesome
  {
   :display "inline-block"
   :font "normal normal normal 14px/1 FontAwesome"
   :font-size "inherit"
   :text-rendering "auto"
   :transform "translate(0, 0)"
   }
)

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; VARS

(def max-width (em 80))
;$gray-darker: lighten(#000,15%);
;$gray-dark:              lighten(#000, 20%);   // #333
;$gray:                   lighten(#000, 33.5%); // #555
;$gray-lighter:           lighten(#000, 93.5%); // #eee


(def larger-than-tablet {:min-width (px 750)})
(def larger-than-desktop {:min-width (px 1000)})
(def smaller-than-desktop {:max-width (px 1000)})
(def smaller-than-tablet {:max-width (px 750)})


(def gray-lighter (color/hsl->hex (color/lighten "#000000" 88.5)))
(def gray-light (color/hsl->hex (color/lighten "#000000" 46.7)))
(def gray-darker (color/hsl->hex (color/lighten "#000000" 15)))  

(def base-color "#efece7")

(def container-color "#efece7")
(def white-almost container-color)
(def done-color "#457a1a")

(def gallery-colors {
                     :beige (color/hsl->hex (color/darken "#EFECE7" 3))
                     :beige-light (color/hsl->hex (color/darken "#EFECE7" 8))
                     ;:gray (color/hsl->hex (color/lighten "#000000" 75))
                     ;:gray-light (color/hsl->hex (color/lighten "#000000" 85))
;                     :gelb "#f7db6d"
;                     :gruen "#80bf63"
;                     :rot (color/hsl->hex (color/lighten (rgb 151 0 34) 35))
;                     :blau (color/hsl->hex (color/lighten (rgb 0 119 177) 8))
;                     :orange (rgb 243 113 33)
;                     :pink (rgb 201 125 138)
                     })

(def gallery-height-big (px 420))
(def gallery-height-small (px 300))
(def gallery-teaser-small (px 220))
(def gallery-teaser-big (px 346))
(def gallery-base-color ;"#f7db6d"
  white-almost
  ) ; gelb

(def button-states ["error" "success" "warning" "default" "primary" "secondary"])
(def state-colors {
                   :error (rgb 202 60 60)
                   :success "#009541"
                   :warning (rgb 223 117 20)
                   :secondary (rgb 66 184 221)
                   :default (rgb 66 184 221)
                   })

(defstyles cmskern ;{:output-to "resources/bisley-public/css/bisley-garden.css"}
  [

  ;;(at-import "http://fonts.googleapis.com/css?family=Lato:400,300,100,700")
  ;;(set-font-pure "Lato, Helvetica, Arial, sans-serif")
  ;;(at-import "https://fonts.googleapis.com/css?family=Montserrat:400,700")
  ;;(set-font-pure "Montserrat, Helvetica, Arial, sans-serif")
   (init-buttons state-colors button-states)
   [:.vcomp {:display :inline-block}
    [:.help-block {
                   :margin-bottom "1rem"
                   :color (:error state-colors)
                   :font-size "0.8em"
                   :margin-top 0
                   }]
    [:.has-warning
     [:input :select {
                      :border-color (:warning state-colors)
                      }]
     [:.help-block {:color (:warning state-colors)}]
     ]
    [:.has-success
     [:input :select {
                      :border-color (:success state-colors)
                      }]
     [:.help-block {:color (:success state-colors)}]
]

    [:.has-error
     [:input :select {
                      :border-color (:error state-colors)
                      }]
     [:.help-block {:color (:error state-colors)}]
]
    ]
   [:.login-page {:justify-content :center
                  :margin-top (em 10)}]
   [:textarea
    {:min-height (em 10)}]
   [:.large [:textarea
             {:min-height (em 20)}]]
   [:.file-upload
    [:.preview {:text-align :center
                :justify-content :center
                :display :inline-block

                :margin (px 3)}]]
   [:.dropzone {:padding (em 1)
                :border-width (px 2)
                :border-style :dashed
                :border-color :black
                }
    ]
   [:img.preview {
                      :height (px 80)}]
   [:.spinner
    {
     :margin "100px auto 0"
     :width "70px"
     :text-align :center
     }
    ["> div" {
              :width "18px"
              :height "18px"
              :background-color "#333"

              :border-radius "100%"
              :display "inline-block"
              :-webkit-animation "sk-bouncedelay 1.4s infinite ease-in-out both"
              :animation "sk-bouncedelay 1.4s infinite ease-in-out both"
              }
     ]
    [:.bounce1 {
                :-webkit-animation-delay "-0.32s";
                :animation-delay "-0.32s"
                }]

    [:.bounce2 {
                :-webkit-animation-delay "-0.16s";
                :animation-delay "-0.16s"
                }]


    ]
   (at-keyframes "sk-bouncedelay"
                 ["0%, 80%, 100%" {:-webkit-transform "scale(0)"
                                   :transform "scale(0)"
                                   }]
                 ["40%" {:-webkit-transform "scale(1.0)"
                         :transform "scale(1.0)"
                         }]
                                   )
   [:.fold
    [:.fold-ctrl {:position :absolute
                  }]]

]
  )

(defstyles purecss-cmskern [purecss/v0.6.1 cmskern])

(css {:output-to "resources/public/css/cmskern.css"} purecss-cmskern)
