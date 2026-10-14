package com.assignment.jsondataset.strategy;

import java.math.BigDecimal;
import java.util.Comparator;

/**
 * Compares mixed JSON value types without throwing ClassCastException.
 * Orders numbers, booleans, strings, then other values.
 */
public final class FieldValueComparator implements Comparator<Object> {

    public static final FieldValueComparator INSTANCE = new FieldValueComparator();

    private static final int NUMBER = 0;
    private static final int BOOLEAN = 1;
    private static final int STRING = 2;
    private static final int OTHER = 3;

    private FieldValueComparator() {
    }

    @Override
    public int compare(Object a, Object b) {
        int rankA = rank(a);
        int rankB = rank(b);
        if (rankA != rankB) {
            return Integer.compare(rankA, rankB);
        }
        return switch (rankA) {
            case NUMBER -> toBigDecimal(a).compareTo(toBigDecimal(b));
            case BOOLEAN -> Boolean.compare((Boolean) a, (Boolean) b);
            case STRING -> ((String) a).compareTo((String) b);
            default -> a.toString().compareTo(b.toString());
        };
    }

    private static int rank(Object value) {
        if (value instanceof Number) {
            return NUMBER;
        }
        if (value instanceof Boolean) {
            return BOOLEAN;
        }
        if (value instanceof String) {
            return STRING;
        }
        return OTHER;
    }

    /** BigDecimal avoids the precision loss of comparing large longs through double. */
    private static BigDecimal toBigDecimal(Object number) {
        return number instanceof BigDecimal bd ? bd : new BigDecimal(number.toString());
    }
}
