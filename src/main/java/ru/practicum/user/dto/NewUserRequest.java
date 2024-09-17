package ru.practicum.user.dto;

import lombok.Data;

@Data
public class NewUserRequest {
    String firstName;
    String lastName;
    String email;
}
