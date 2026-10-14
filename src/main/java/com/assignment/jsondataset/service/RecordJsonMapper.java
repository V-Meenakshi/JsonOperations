package com.assignment.jsondataset.service;

import com.assignment.jsondataset.entity.DatasetRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

/** Converts between the record Map used by the API and the JSON text stored in the database. */
@Component
@RequiredArgsConstructor
public class RecordJsonMapper {

    private final JsonMapper jsonMapper;

    public String toJson(Map<String, Object> record) {
        try {
            return jsonMapper.writeValueAsString(record);
        } catch (JacksonException e) {
            throw new IllegalStateException("Could not serialise record", e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> toMap(DatasetRecord entity) {
        try {
            return jsonMapper.readValue(entity.getData(), Map.class);
        } catch (JacksonException e) {
            // Corrupt stored data is a server problem; never hide it behind an empty record.
            throw new IllegalStateException("Stored record " + entity.getPk() + " is not valid JSON", e);
        }
    }
}