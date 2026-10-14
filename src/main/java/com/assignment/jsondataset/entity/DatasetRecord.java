package com.assignment.jsondataset.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One JSON record that belongs to a named dataset.
 * The JSON itself is kept as text, so any fields can be stored.
 */
@Entity
@Table(
    name = "dataset_record",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_dataset_record",
        columnNames = {"dataset_name", "record_id"}),
    indexes = @Index(name = "idx_dataset_name", columnList = "dataset_name")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)   // JPA needs it, others shouldn't use it
public class DatasetRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pk;                       // internal database primary key

    @Column(name = "dataset_name", nullable = false, length = 100)
    private String datasetName;            

    @Column(name = "record_id", nullable = false)
    private Long recordId;                 

    @Column(name = "data", nullable = false, columnDefinition = "TEXT")
    private String data;                   

    public DatasetRecord(String datasetName, Long recordId, String data) {
        this.datasetName = datasetName;
        this.recordId = recordId;
        this.data = data;
    }
}