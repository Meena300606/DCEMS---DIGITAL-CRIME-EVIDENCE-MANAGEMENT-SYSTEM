package com.dcems.util;

import com.dcems.model.User;

/** Holds the currently logged-in user. */
public final class Session {
    public static User user;
    private Session() {}
    public static boolean isAdmin() { return user != null && "ADMIN".equals(user.role); }
    public static String name() { return user == null ? "system" : user.fullName; }
}
