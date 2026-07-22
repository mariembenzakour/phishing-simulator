package com.intellisec.phishsim.target;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TargetRepository extends JpaRepository<Target, UUID> {
    List<Target> findByGroupId(UUID groupId);
}