package com.intellisec.phishsim.target;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TargetGroupService {

    private final TargetGroupRepository targetGroupRepository;

    public List<TargetGroup> getAll() {
        return targetGroupRepository.findAll();
    }

    public TargetGroup create(TargetGroup group) {
        return targetGroupRepository.save(group);
    }

    public void delete(UUID id) {
        targetGroupRepository.deleteById(id);
    }
}