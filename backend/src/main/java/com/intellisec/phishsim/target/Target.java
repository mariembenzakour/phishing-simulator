package com.intellisec.phishsim.target;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Entity
@Table(name = "targets")
@Data
public class Target {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String email;

    private String firstName;
    private String lastName;

    private UUID groupId;
}
