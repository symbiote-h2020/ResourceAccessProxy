package eu.h2020.symbiote.resources.db;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebsocketSessionRepository extends MongoRepository<WebsocketSessionInfo, String> {
}
