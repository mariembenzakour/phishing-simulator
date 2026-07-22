package com.intellisec.phishsim.target;


import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Entity
@Table(name = "target_groups")
@Data
public class TargetGroup {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    private UUID createdBy;
}