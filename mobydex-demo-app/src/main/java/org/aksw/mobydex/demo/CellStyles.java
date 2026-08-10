package org.aksw.mobydex.demo;

import software.xdev.vaadin.maps.leaflet.layer.vector.LPolylineOptions;

public class CellStyles {
        public static LPolylineOptions grey() {
            return grey(new LPolylineOptions());
        }

        public static LPolylineOptions blue() {
            return blue(new LPolylineOptions());
        }

        public static LPolylineOptions green() {
            return green(new LPolylineOptions());
        }

        public static LPolylineOptions red() {
            return red(new LPolylineOptions());
        }

        public static LPolylineOptions grey(LPolylineOptions options) {
            return options.withColor("grey").withFillColor("lightgrey").withFillOpacity(0.5);
        }

        public static LPolylineOptions blue(LPolylineOptions options) {
            return options.withColor("blue").withFillColor("lightblue").withFillOpacity(0.5);
        }

        public static LPolylineOptions green(LPolylineOptions options) {
            return options.withColor("green").withFillColor("lightgreen").withFillOpacity(0.5);
        }

        public static LPolylineOptions red(LPolylineOptions options) {
            return options.withColor("red").withFillColor("orange").withFillOpacity(0.5);
        }

        public static LPolylineOptions purple(LPolylineOptions options) {
            return options.withColor("purple").withOpacity(0.8).withFillColor("purple").withFillOpacity(0.8);
        }

//        public static LPolylineOptions selected(LPolylineOptions options) {
//            return options.withStroke(true).withColor("orange").withOpacity(0.5);
//        }
    }
