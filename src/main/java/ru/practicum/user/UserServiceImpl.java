package ru.practicum.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.user.dto.UserDto;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
class UserServiceImpl implements UserService {

    private final UserRepository repository;

    @Override
    @Transactional
    public List<UserDto> getAllUsers() {
        return repository.findAll().stream()
                .map(UserMapper::mapToUserDto)
                .toList();
    }

    @Override
    public UserDto saveUser(UserDto request) {
        log.info("Got {} to save", request);
        User user = UserMapper.mapToNewUser(request);
        repository.save(user);
        return UserMapper.mapToUserDto(user);
    }
}