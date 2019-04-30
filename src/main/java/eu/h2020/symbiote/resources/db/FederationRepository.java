package eu.h2020.symbiote.resources.db;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import eu.h2020.symbiote.model.mim.Federation;

/**
 * @author Pavle Skocir
 * 
 * MongoDB repository interface for Federation objects providing CRUD operations.
 */

@Repository
public interface FederationRepository extends MongoRepository<Federation, String> {

	Optional<Federation> findById(String federationId);
}
