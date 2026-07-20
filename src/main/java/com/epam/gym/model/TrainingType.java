package com.epam.gym.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "training_types")
@Getter
@NoArgsConstructor
public class TrainingType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "training_type_name", nullable = false, unique = true)
    private TrainingTypeName trainingTypeName;

    public TrainingType(TrainingTypeName trainingTypeName) {
        this.trainingTypeName = trainingTypeName;
    }
}