package com.assignment.jsondataset.service;

import com.assignment.jsondataset.entity.DatasetRecord;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecordJsonMapperTest {

    private final RecordJsonMapper mapper = new RecordJsonMapper(JsonMapper.builder().build());

    @Test
    void roundTripsARecord() {
        Map<String, Object> original = Map.of("id", 1, "name", "John Doe", "age", 30);

        String json = mapper.toJson(original);
        Map<String, Object> restored = mapper.toMap(new DatasetRecord("employees", 1L, json));

        assertThat(restored).isEqualTo(original);
    }

    @Test
    void corruptJsonFailsWithClearMessage() {
        DatasetRecord corrupt = new DatasetRecord("employees", 1L, "{not json");

        assertThatThrownBy(() -> mapper.toMap(corrupt))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not valid JSON");
    }
}