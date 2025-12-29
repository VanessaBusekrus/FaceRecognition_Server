package nl.cyberella.hands_on.repositories;

import nl.cyberella.hands_on.models.Login;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginRepository extends JpaRepository<Login, String> {
	/**
	 * Repository for authentication records. Extends JpaRepository to inherit
	 * common CRUD operations. The generic parameters are <Login, String>
	 * where Login is the entity type and String is the type of its id
	 * (in this design the email is used as the primary key for Login).
	 *
	 * No method declarations are required here for basic persistence —
	 * Spring Data JPA will provide the implementation for standard
	 * operations like save(), findById(), delete(), etc.
	 */
}
