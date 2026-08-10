package org.aksw.mobydex.demo.interpolation;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.TreeMap;

public class Scale<K extends Number> {
    private int n;
    private NavigableMap<K, double[]> map;
    // private OrderedDomain<K> orderedDomain;

    protected Scale(int n, NavigableMap<K, double[]> map) {
        super();
        this.n = n;
        this.map = map;
    }


    public NavigableMap<K, double[]> getMap() {
        return map;
    }

    public static <K extends Number> Scale<K> of(int n) {
        return new Scale<>(n, new TreeMap<>());
    }

    public Scale<K> put(K key, double... vector) {
        int l = vector.length;
        if (l != n) {
            throw new IllegalArgumentException("vector.length != " + n);
        }
        map.put(key, vector);
        return this;
    }

//    public Scale put(Number key, Number... vector) {
//        return put(key, Arrays.asList(vector));
//    }

    public Scale<K> put(K key, List<Double> vector) {
        map.put(key, vector.stream().mapToDouble(Double::doubleValue).toArray());
        return this;
    }

    public double[] interpolate(K key) {
        if (map.isEmpty()) {
            throw new NoSuchElementException();
        }

        Entry<K, double[]> lower = map.floorEntry(key);
        Entry<K, double[]> upper = map.ceilingEntry(key);

        double[] vector;

        if (lower == null) {
            vector = upper.getValue();
        } else if (upper == null) {
            vector = lower.getValue();
        } else {
            double t = fraction(lower.getKey(), upper.getKey(), key);

            vector = interpolate(
                lower.getValue(),
                upper.getValue(),
                t
            );
        }
        return vector;
    }

    public static double[] interpolate(double[] min, double[] max, double t) {
        int n = min.length;
        if (max.length != n) {
            throw new IllegalArgumentException("min.length != max.length");
        }

        double[] result = new double[n];
        for (int i = 0; i < n; ++i) {
            double m = min[i];
            double d = max[i] - m;
            result[i] = m + d * t;
        }
        return result;
    }

    public static void main(String[] args) {
        double[] x = Scale
            .of(3)
            .put(0f, 0, 10, 100)
            .put(1f, 100, 20, 110)
            .interpolate(0.5f);
        System.out.println(Arrays.toString(x));
    }


    private static double position(Number number) {
        return number.doubleValue();
    }

    private static double fraction(Number lower, Number upper, Number value) {
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
