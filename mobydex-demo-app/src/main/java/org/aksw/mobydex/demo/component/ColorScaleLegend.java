package org.aksw.mobydex.demo.component;

import java.util.NavigableMap;
import java.util.stream.Collectors;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import org.aksw.mobydex.demo.interpolation.Scale;

public class ColorScaleLegend extends VerticalLayout {

    public ColorScaleLegend(Scale<?> scale) {
        setPadding(false);
        setSpacing(false);
        setWidth("300px");

        Div gradient = new Div();
        gradient.setWidthFull();
        gradient.setHeight("16px");
        gradient.getStyle()
                .set("background", toCssGradient(scale.getMap()))
                .set("border-radius", "3px");

        HorizontalLayout labels = new HorizontalLayout();
        labels.setWidthFull();
        labels.setPadding(false);
        labels.setSpacing(false);
        labels.setJustifyContentMode(
                FlexComponent.JustifyContentMode.BETWEEN);

        labels.add(
                new Span(scale.getMap().firstKey().toString()),
                new Span(scale.getMap().lastKey().toString()));

        add(gradient, labels);
    }

    public static <T extends Number> String toCssGradient(NavigableMap<T, double[]> colors) {

        double min = colors.firstKey().doubleValue();
        double max = colors.lastKey().doubleValue();

        return colors.entrySet().stream()
                .map(entry -> {
                    double position = (entry.getKey().doubleValue() - min) / (max - min);

                    return "%s %.2f%%".formatted(
                            toColorHexString(entry.getValue()),
                            position * 100.0f);
                })
                .collect(Collectors.joining(
                        ", ",
                        "linear-gradient(to right, ",
                        ")"));
    }

    public static String toColorHexString(double[] color) {
        if (color == null || color.length != 3) {
            throw new IllegalArgumentException(
                    "Color must be a 3-component RGB array");
        }

        int red = (int)Math.round(clamp01(color[0]) * 255.0);
        int green = (int)Math.round(clamp01(color[1]) * 255.0);
        int blue = (int)Math.round(clamp01(color[2]) * 255.0f);

        return String.format("#%02X%02X%02X", red, green, blue);
    }

    private static double clamp01(double value) {
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException(
                    "Color components must not be NaN");
        }

        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
