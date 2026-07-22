package com.intellisec.phishsim.target;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface TargetGroupRepository extends JpaRepository<TargetGroup, UUID> {
}