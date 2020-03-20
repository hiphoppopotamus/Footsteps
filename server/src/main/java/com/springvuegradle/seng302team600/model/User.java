package com.springvuegradle.seng302team600.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.springvuegradle.seng302team600.exception.InvalidDateOfBirthException;
import com.springvuegradle.seng302team600.exception.InvalidUserNameException;
import com.springvuegradle.seng302team600.exception.UserTooYoungException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

@Entity
public class User {

    private static Log log = LogFactory.getLog(User.class);

    private static PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Id
    @GeneratedValue
    @JsonProperty("id")
    private Long userId;

    @JsonProperty("fitness")
    private int fitnessLevel;

    @NotNull(message = "Please provide a first name")
    @JsonProperty("firstname")
    private String firstName;

    @JsonProperty("middlename")
    private String middleName;

    @NotNull(message = "Please provide a last name")
    @JsonProperty("lastname")
    private String lastName;

    @JsonProperty("nickname")
    private String nickName;

    @JsonProperty("bio")
    private String bio;

    @OneToOne(fetch = FetchType.EAGER,cascade=CascadeType.ALL)
    @NotNull(message = "Please provide a primary email address")
    @JsonProperty("primary_email")
    private Emails emails;

    @NotNull(message = "Please provide a password")
    @JsonProperty("password")
    private String password;

    @NotNull(message = "Please provide a date of birth")
    @JsonFormat(pattern="yyyy-MM-dd")
    @JsonProperty("date_of_birth")
    private Date dateOfBirth;

    @NotNull(message = "Please provide a gender from the following: male, female, non_binary")
    @Enumerated(EnumType.STRING)
    @JsonProperty("gender")
    private Gender gender;

    @Transient
    @JsonProperty("passports")
    private ArrayList<String> passports;


    public enum Gender {
        @JsonProperty("male")
        MALE,
        @JsonProperty("female")
        FEMALE,
        @JsonProperty("non_binary")
        NON_BINARY
    }

    //Can implement later, makes more sense in the long run
    public enum FitnessLevel {
        SEDENTARY, LOW, MEDIUM, HIGH, VERY_HIGH
    }

    public User() {}


    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long id) {
        this.userId = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Emails getEmails() {
        return emails;
    }

    public void setEmails(Emails emails) {
        this.emails = emails;
    }

    public boolean checkPassword(String password) {
        return encoder.matches(password, this.password);
    }

    public void setPassword(String password) {
        this.password = encoder.encode(password);
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public int getFitnessLevel() {
        return fitnessLevel;
    }

    public void setFitnessLevel(int fitnessLevel) {
        this.fitnessLevel = fitnessLevel;
    }

    public ArrayList<String> getPassports() {
        return passports;
    }

    public void setPassports(ArrayList<String> passports) {
        this.passports = passports;
    }

    public void addPassport(String passport) {
        passports.add(passport);
    }

    public boolean removePassport(String passport) {
        return passports.remove(passport);
    }


    @PrePersist
    public void logNewUserAttempt() {
        log.info("Attempting to add new user with email: " + emails.getPrimaryEmail());
    }

    @PostPersist
    public void logNewUserAdded() {
        log.info("Added user '" + firstName + " " + lastName + "' with primary email: " + emails.getPrimaryEmail());
    }

    @PreRemove
    public void logUserRemovalAttempt() {
        log.info("Attempting to delete user: " + emails.getPrimaryEmail());
    }

    @PostRemove
    public void logUserRemoval() {
        log.info("Deleted user: " + emails.getPrimaryEmail());
    }

    @PreUpdate
    public void logUserUpdateAttempt() {
        log.info("Attempting to update user: " + emails.getPrimaryEmail());
    }

    @PostUpdate
    public void logUserUpdate() {
        log.info("Updated user: " + emails.getPrimaryEmail());
    }

    @Override
    public String toString() {
        return String.format("%s %s, ID: %d, %s", getFirstName(), getLastName(), getUserId(), super.toString());
    }

    public void isValid() throws InvalidUserNameException, UserTooYoungException, InvalidDateOfBirthException {
        if (! firstName.matches("[a-zA-Z]+") ) { throw new InvalidUserNameException(); }
        if (! lastName.matches("[a-zA-Z]+") ) { throw new InvalidUserNameException(); }
        if (! middleName.isEmpty()) {
            if (! middleName.matches("[a-zA-Z]+") ) { throw new InvalidUserNameException(); }
        }
        if (ageCheck(dateOfBirth, 13, true)) { throw new UserTooYoungException(); }
        if (ageCheck(dateOfBirth, 150, false)) { throw new InvalidDateOfBirthException(); }
        //fitnessLevel
        //this.gender

    }

    /**
     *
     * @param DoB date of birth for prespective new user
     * @param age age to check against
     * @param younger boolean tag to determine if checking if the person is younger (false checks if older)
     * @return boolean tag denoting how given DoB compares to given age with respect to younger tag
     */
    private boolean ageCheck(Date DoB, int age, boolean younger) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -age);
        Date AGE = calendar.getTime();
        if ( younger ) {
            return AGE.before(DoB);
        } else {
            return AGE.after(DoB);
        }
    }
}
