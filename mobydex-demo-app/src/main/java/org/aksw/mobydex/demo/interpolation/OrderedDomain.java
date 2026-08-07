package org.aksw.mobydex.demo.interpolation;

import java.util.Comparator;

public interface OrderedDomain<K> {
    Comparator<? super K> comparator();
    double position(K key);

    default double fraction(K lower, K upper, K value) {
        double lowerPosition = position(lower);
        double upperPosition = position(upper);
        double valuePosition = position(value);

        double d = upperPosition - lowerPosition;

        double result = Math.abs(d) < 0.001f
            ? lowerPosition
            : (valuePosition - lowerPosition) / d;

        return result;
    }
}

