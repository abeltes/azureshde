package com.example.hrmws.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.hrmws.api.dto.Tutor;
import com.example.hrmws.persistence.TutorEntity;
import com.example.hrmws.persistence.TutorId;
import com.example.hrmws.persistence.TutorRepository;

@Service
public class TutorUpsertService {

    private final TutorRepository tutorRepository;

    public TutorUpsertService(TutorRepository tutorRepository) {
        this.tutorRepository = tutorRepository;
    }

    @Transactional
    public int upsertTutors(List<Tutor> tutors) {
        if (tutors == null || tutors.isEmpty()) {
            return 0;
        }

        Map<TutorId, Tutor> incomingById = new LinkedHashMap<>();
        for (Tutor tutor : tutors) {
            incomingById.put(new TutorId(tutor.semCode(), tutor.afm()), tutor);
        }

        Map<TutorId, TutorEntity> existingById = new LinkedHashMap<>();
        for (TutorEntity existing : tutorRepository.findAllById(incomingById.keySet())) {
            existingById.put(new TutorId(existing.getSemCode(), existing.getAfm()), existing);
        }

        List<TutorEntity> entitiesToSave = new ArrayList<>();
        int insertedCount = 0;
        for (Map.Entry<TutorId, Tutor> entry : incomingById.entrySet()) {
            Tutor incoming = entry.getValue();
            TutorEntity entity = existingById.get(entry.getKey());
            if (entity == null) {
                insertedCount++;
                entity = new TutorEntity(
                        incoming.semCode(),
                        incoming.afm(),
                        incoming.createdDate(),
                        incoming.deleted(),
                        incoming.modifiedDate());
            } else {
                entity.setCreatedDate(incoming.createdDate());
                entity.setDeleted(incoming.deleted());
                entity.setModifiedDate(incoming.modifiedDate());
            }
            entitiesToSave.add(entity);
        }

        tutorRepository.saveAll(entitiesToSave);
        return insertedCount;
    }
}
