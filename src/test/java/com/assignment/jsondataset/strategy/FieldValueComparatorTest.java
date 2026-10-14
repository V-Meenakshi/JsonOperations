package com.assignment.jsondataset.strategy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FieldValueComparatorTest {

    private final FieldValueComparator comparator = FieldValueComparator.INSTANCE;

    @Test
    void comparesNumbersOfDifferentJavaTypesNumerically() {
        assertThat(comparator.compare(2, 10L)).isNegative();
        assertThat(comparator.compare(2.5, 2)).isPositive();
        assertThat(comparator.compare(new BigDecimal("3.0"), 3)).isZero();
    }

    @Test
    void doesNotLoseLongPrecision() {
        assertThat(comparator.compare(9007199254740993L, 9007199254740992L)).isPositive();
    }

    @Test
    void comparesStringsLexicographically() {
        assertThat(comparator.compare("Alice", "Bob")).isNegative();
    }

    @Test
    void comparesBooleans() {
        assertThat(comparator.compare(false, true)).isNegative();
    }

    @Test
    void mixedTypesAreOrderedNumbersBooleansStringsOthers() {
        List<Object> values = new ArrayList<>(List.of("x", List.of(1), true, 5));

        values.sort(comparator);

        assertThat(values).containsExactly(5, true, "x", List.of(1));
    }
}
