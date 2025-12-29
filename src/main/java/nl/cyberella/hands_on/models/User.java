package nl.cyberella.hands_on.models;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;

/*
User is a database entity for application users.
Mapped to public.users table.
Contains:
- Primary key (id)
- Basic profile info (name, email, joined, entries, phone)
- Two-factor authentication fields (twoFactorEnabled, twoFactorSecret, tempTwoFactorSecret)
Lightweight and safe for returning user profile data without exposing sensitive info like passwords.
*/

@Entity // JPA will map this class to a table
@Table(name = "users", schema = "public") // explicitly sets table name and schema
// Lombok annotations:
@Getter // generate getter and setter methods
@Setter
@NoArgsConstructor // no-argument constructor
@AllArgsConstructor // constructor with all fields
public class User {

    // Primary key (auto-incremented in H2 by IDENTITY strategy)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // id = primary key, auto-incremented in H2 database

    // Basic profile fields
    private String name;
    private String email;

    // Date the user joined the system (stored as DATE in the DB)
    private LocalDate joined;

    // How many images/entries the user has submitted (used by the application)
    private Integer entries;

    // Optional contact phone number
    private String phone;

    // Two-factor (TOTP) fields
    // Indicates whether 2FA is enabled for this account
    private Boolean twoFactorEnabled;
    // The permanent TOTP secret (base32) used to validate tokens
    private String twoFactorSecret;
    // Temporary TOTP secret used during setup until the user verifies it
    private String tempTwoFactorSecret;
}
