package com.example.hrmws.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.hrmws.api.dto.Participant;
import com.example.hrmws.persistence.ParticipantEntity;
import com.example.hrmws.persistence.ParticipantId;
import com.example.hrmws.persistence.ParticipantRepository;

@Service
public class ParticipantUpsertService {

    private final ParticipantRepository participantRepository;

    public ParticipantUpsertService(ParticipantRepository participantRepository) {
        this.participantRepository = participantRepository;
    }

    @Transactional
    public int upsertParticipants(List<Participant> participants) {
        if (participants == null || participants.isEmpty()) {
            return 0;
        }

        Map<ParticipantId, Participant> incomingById = new LinkedHashMap<>();
        for (Participant participant : participants) {
            incomingById.put(new ParticipantId(participant.semCode(), participant.afm()), participant);
        }

        int existingCount = participantRepository.findAllById(incomingById.keySet()).size();

        List<ParticipantEntity> entities = incomingById.values().stream()
                .map(participant -> new ParticipantEntity(
                        participant.semCode(),
                        participant.afm(),
                        participant.attendance(),
                        participant.unitDuringSeminar(),
                        participant.deleted(),
                        participant.createdDate(),
                        participant.modifiedDate()))
                .collect(Collectors.toList());

        participantRepository.saveAll(entities);
            return incomingById.size() - existingCount;
    }
}
