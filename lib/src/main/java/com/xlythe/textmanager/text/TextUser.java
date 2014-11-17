package com.xlythe.textmanager.text;

import com.xlythe.textmanager.User;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a phone number.
 */
public class TextUser implements User {
    // Keeps track of users. We don't want to make a new user very often - that wastes memory!
    // So we have 1 user for every 1 phone number, and map them together.
    private static final Map<String, TextUser> USERS = new HashMap<String, TextUser>();

    /**
     * Get a User for a phone number
     * */
    public static TextUser getUser(String phoneNumber) {
        if(!USERS.containsKey(phoneNumber)) {
            USERS.put(phoneNumber, new TextUser(phoneNumber));
        }
        return USERS.get(phoneNumber);
    }

    private final String phoneNumber;
    private String name;

    /**
     * This method is the constructor. It it called when we call "new TextUser()".
     * We make it private because we want to force coders to use "TextUser.getUser(number)".
     * */
    private TextUser(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getName() {
        return name == null ? phoneNumber : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns true if a custom name has been set
     * */
    public boolean hasName() {
        return name != null;
    }
}
