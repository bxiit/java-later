package ru.practicum.user.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.user.User;
import ru.practicum.user.UserState;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@UtilityClass
public class UserMapper {
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd,hh:mm:ss");

    public static User mapToModel(UserDto userDto) {
        User user = new User();
        user.setId(userDto.getId());
        user.setEmail(userDto.getEmail());
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setRegistrationDate(parseStringToInstant(userDto.getRegistrationDate()));
        user.setState(userDto.getState() == null ? UserState.ACTIVE : userDto.getState());
        return user;
    }

    public static UserDto mapToDto(User user) {
        return new UserDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                parseInstantToString(user.getRegistrationDate()),
                user.getState()
        );
    }

    public static User mapToModel(NewUserRequest request) {
        User user = new User();
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());

        return user;
    }

    private static Instant parseStringToInstant(String registrationDate) {
        return Optional.ofNullable(registrationDate)
                .map(Instant::parse)
                .orElseGet(Instant::now);
    }

    private static String parseInstantToString(Instant registrationDate) {
        return Optional.ofNullable(registrationDate)
                .map(instant -> LocalDateTime.ofInstant(instant, ZoneId.systemDefault()))
                .map(localDateTime -> localDateTime.format(DATE_TIME_FORMATTER))
                .orElse(null);
    }
}
