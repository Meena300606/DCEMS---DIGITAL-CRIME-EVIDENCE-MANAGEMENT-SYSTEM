package com.dcems.model;

import java.time.LocalDateTime;

public class CustodyEntry {
    public int id, evidenceId;
    public String evidenceNo, action, fromPerson, toPerson, remarks, loggedBy;
    public LocalDateTime loggedAt;
}
