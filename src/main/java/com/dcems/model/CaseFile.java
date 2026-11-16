package com.dcems.model;

import java.time.LocalDate;

public class CaseFile {
    public int id, assignedTo, evidenceCount;
    public String caseNo, title, description, category, status, priority, assignee;
    public LocalDate openedOn;

    @Override public String toString() { return caseNo + " – " + title; }
}
