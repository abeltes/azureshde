package com.example.hrmws.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.hrmws.api.dto.Seminar;
import com.example.hrmws.persistence.SeminarEntity;
import com.example.hrmws.persistence.SeminarRepository;

@Service
public class SeminarUpsertService {

    private final SeminarRepository seminarRepository;

    public SeminarUpsertService(SeminarRepository seminarRepository) {
        this.seminarRepository = seminarRepository;
    }

    @Transactional
    public int upsertSeminars(List<Seminar> seminars) {
        if (seminars == null || seminars.isEmpty()) {
            return 0;
        }

        Map<String, Seminar> incomingById = new LinkedHashMap<>();
        for (Seminar seminar : seminars) {
            incomingById.put(seminar.semCode(), seminar);
        }

        int existingCount = seminarRepository.findAllById(incomingById.keySet()).size();

        List<SeminarEntity> entities = incomingById.values().stream()
                .map(seminar -> new SeminarEntity(
                        seminar.semCode(),
                        seminar.semDescr(),
                        seminar.beginDate(),
                        seminar.endDate(),
                        seminar.createdDate(),
                        seminar.deleted(),
                        seminar.modifiedDate()))
                .collect(Collectors.toList());

        seminarRepository.saveAll(entities);
        return incomingById.size() - existingCount;
    }
}
