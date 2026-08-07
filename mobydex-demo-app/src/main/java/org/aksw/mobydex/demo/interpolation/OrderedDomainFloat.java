package org.aksw.mobydex.demo.interpolation;

import java.util.Comparator;

public class OrderedDomainFloat
    implements OrderedDomain<Float>
{
    private static final OrderedDomain<Float> INSTANCE = new OrderedDomainFloat();

    public static OrderedDomain<Float> get() {
        return INSTANCE;
    }

    @Override
    public Comparator<? super Float> comparator() {
        return Comparator.naturalOrder();
    }

    @Override
    public double position(Float key) {
        return key.doubleValue();
    }
}
