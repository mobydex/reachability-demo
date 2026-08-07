package org.aksw.mobydex.demo.interpolation;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.TreeMap;

public class Scale<K extends Comparable<K>> {
    private int n;
    private NavigableMap<K, float[]> map;
    private OrderedDomain<K> orderedDomain;

    protected Scale(int n, NavigableMap<K, float[]> map, OrderedDomain<K> orderedDomain) {
        super();
        this.n = n;
        this.map = map;
        this.orderedDomain = orderedDomain;
    }

    public static Scale<Float> ofFloat(int n) {
        return new Scale<>(n, new TreeMap<>(), OrderedDomainFloat.get());
    }

    public static <K extends Comparable<K>> Scale<K> of(int n, OrderedDomain<K> orderedDomain) {
        return new Scale<>(n, new TreeMap<>(), orderedDomain);
    }

    public Scale<K> put(K key, float[] vector) {
        int l = vector.length;
        if (l != n) {
            throw new IllegalArgumentException("vector.length != " + n);
        }
        map.put(key, vector);
        return this;
    }

    public float[] interpolate(K key) {
        if (map.isEmpty()) {
            throw new NoSuchElementException();
        }

        Entry<K, float[]> lower = map.floorEntry(key);
        Entry<K, float[]> upper = map.ceilingEntry(key);

        float[] vector;

        if (lower == null) {
            vector = upper.getValue();
        } else if (upper == null) {
            vector = lower.getValue();
        } else {
            float t = (float)orderedDomain.fraction(lower.getKey(), upper.getKey(), key);

            vector = interpolate(
                lower.getValue(),
                upper.getValue(),
                t
            );
        }
        return vector;
    }

    public static float[] interpolate(float[] min, float[] max, float t) {
        int n = min.length;
        if (max.length != n) {
            throw new IllegalArgumentException("min.length != max.length");
        }

        float[] result = new float[n];
        for (int i = 0; i < n; ++i) {
            float m = min[i];
            float d = max[i] - m;
            result[i] = m + d * t;
        }
        return result;
    }

    public static void main(String[] args) {
        float[] x = Scale
            .ofFloat(3)
            .put(0f, new float[]{0, 10, 100})
            .put(1f, new float[]{100, 20, 110})
            .interpolate(0.5f);
        System.out.println(Arrays.toString(x));
    }
}
