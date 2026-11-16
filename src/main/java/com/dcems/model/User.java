package com.dcems.model;

public class User {
    public int id;
    public String username, fullName, role;

    @Override public String toString() { return fullName; }
}
