package com.intellisec.phishsim.target;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TargetGroupService {

    private final TargetGroupRepository targetGroupRepository;

    public List<TargetGroup> getAll() {
        return targetGroupRepository.findAll();
    }

    public TargetGroup getById(UUID id) {
        return targetGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("TargetGroup not found"));
    }

    @Transactional
    public TargetGroup create(TargetGroup group) {
        return targetGroupRepository.save(group);
    }

    @Transactional
    public TargetGroup update(UUID id, TargetGroup groupData) {
        TargetGroup group = getById(id);
        group.setName(groupData.getName());
        group.setCreatedBy(groupData.getCreatedBy());
        return targetGroupRepository.save(group);
    }

    @Transactional
    public void delete(UUID id) {
        TargetGroup group = getById(id);
        targetGroupRepository.delete(group);
    }
}