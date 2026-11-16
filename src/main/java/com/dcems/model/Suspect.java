package com.dcems.model;

public class Suspect {
    public int id, age, caseId;
    public String fullName, alias, phone, address, notes, caseNo;

    @Override public String toString() { return fullName; }
}
