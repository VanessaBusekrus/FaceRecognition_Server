package nl.cyberella.hands_on.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
// Imports Lombok annotations to generate boilerplate code automatically:
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;

/**
 * Login is a database entity for authentication.
 * Primary key = email.
 * Field hash = hashed password (never store plain text).
 * Uses Lombok for boilerplate, JPA annotations for database mapping.
*/
@Entity // marks this class as a database entity => JPA will map this class to a table
@Table(name = "login", schema = "public") // maps the entity to a specific table in the database => explicitly sets table name (login) and schema (public).
@Getter // automatically generate getters and setters for all fields
@Setter
@NoArgsConstructor // no-argument constructor for JPA
@AllArgsConstructor // constructor with all fields (email and hash)
public class Login {
    // The email is used as primary key for quick lookup during signin.
    @Id // primary key of the table
    private String email;

    // The BCrypt hashed password (never store plain text here).
    private String hash;
}
