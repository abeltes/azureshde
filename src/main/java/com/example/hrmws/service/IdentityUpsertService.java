package com.example.hrmws.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.hrmws.api.dto.Identity;
import com.example.hrmws.persistence.IdentityEntity;
import com.example.hrmws.persistence.IdentityRepository;

@Service
public class IdentityUpsertService {

    private final IdentityRepository identityRepository;

    public IdentityUpsertService(IdentityRepository identityRepository) {
        this.identityRepository = identityRepository;
    }

    @Transactional
    public int upsertIdentities(List<Identity> identities) {
        if (identities == null || identities.isEmpty()) {
            return 0;
        }

        Map<String, Identity> incomingById = new LinkedHashMap<>();
        for (Identity identity : identities) {
            incomingById.put(identity.afm(), identity);
        }

        int existingCount = identityRepository.findAllById(incomingById.keySet()).size();

        List<IdentityEntity> entities = incomingById.values().stream()
                .map(identity -> new IdentityEntity(
                        identity.afm(),
                        identity.issueDate(),
                        identity.identityId()))
                .collect(Collectors.toList());

        identityRepository.saveAll(entities);
        return incomingById.size() - existingCount;
    }
}
