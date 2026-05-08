package com.dudev.datingapp.user.entity;

public enum Gender {
    MALE, FEMALE, OTHER;

    public static Gender getOpposite(Gender gender) {
        if (gender == null) {
            return OTHER;
        }
        return switch (gender) {
            case MALE -> FEMALE;
            case FEMALE -> MALE;
            case OTHER -> OTHER;
        };
    }
}
