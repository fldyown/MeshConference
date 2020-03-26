package com.fldys.mesh.conference.repository;

import com.fldys.mesh.conference.domain.Conference;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ConferenceRepository extends MongoRepository<Conference, String> {
}
