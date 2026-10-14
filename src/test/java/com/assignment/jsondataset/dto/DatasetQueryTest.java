package com.assignment.jsondataset.dto;

import com.assignment.jsondataset.exception.InvalidQueryException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatasetQueryTest {

    @Test
    void blankFieldsAreTreatedAsNotProvided() {
        DatasetQuery query = DatasetQuery.of("  ", "", null);

        assertThat(query.hasGroupBy()).isFalse();
        assertThat(query.hasSortBy()).isFalse();
        assertThat(query.order()).isEqualTo(SortOrder.ASC);
    }

    @Test
    void fieldsAreTrimmed() {
        DatasetQuery query = DatasetQuery.of(" department ", " age ", "desc");

        assertThat(query.groupBy()).isEqualTo("department");
        assertThat(query.sortBy()).isEqualTo("age");
        assertThat(query.order()).isEqualTo(SortOrder.DESC);
    }

    @Test
    void orderIsCaseInsensitive() {
        assertThat(SortOrder.parse("ASC")).isEqualTo(SortOrder.ASC);
        assertThat(SortOrder.parse("Desc")).isEqualTo(SortOrder.DESC);
    }

    @Test
    void unknownOrderIsRejected() {
        assertThatThrownBy(() -> SortOrder.parse("upwards"))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("upwards");
    }
}
