package com.xlythe.textmanager;

/**
 * Represents a person who can send or receive messages.
 */
public interface User {

        String sender = "";
        int PhoneNumber = 0;

        /**
         * Return the user's name
         * */
        public String getName(String sender, int PhoneNumber);
    }

