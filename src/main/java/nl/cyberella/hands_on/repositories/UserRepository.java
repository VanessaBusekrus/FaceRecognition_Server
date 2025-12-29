package nl.cyberella.hands_on.repositories;

import nl.cyberella.hands_on.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    /**
     * Find a user by their email address.
     *
     * Note: this method is only declared here — Spring Data JPA will
     * automatically generate the implementation at runtime based on the
     * method name ("findByEmail"). It returns an Optional to clearly
     * indicate the user may be absent.
     *
     * Equivalent JPQL: {@code SELECT u FROM User u WHERE u.email = :email}
     *
     * @param email the email to search for
     * @return Optional containing the User if found, otherwise empty
     */
    Optional<User> findByEmail(String email);
}
