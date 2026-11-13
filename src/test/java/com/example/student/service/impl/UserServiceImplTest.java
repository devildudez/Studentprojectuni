package com.example.student.service.impl;

import com.example.student.dto.user.UserRequest;
import com.example.student.dto.user.UserResponse;
import com.example.student.dto.user.UserResponseList;
import com.example.student.exception.EmailAlreadyExistsException;
import com.example.student.exception.EntityNotFoundException;
import com.example.student.mapper.UserMapper;
import com.example.student.model.Role;
import com.example.student.model.User;
import com.example.student.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("teacher@example.com").password("hashed-password")
                .roles(Set.of(Role.TEACHER)).build();
    }

    @Test
    void existsByEmailDelegatesToRepository() {
        when(userRepository.existsByEmail("teacher@example.com")).thenReturn(true);
        assertTrue(userService.existsByEmail("teacher@example.com"));
        verify(userRepository).existsByEmail("teacher@example.com");
    }

    @Test
    void registerCreatesUserWithEncodedPassword() {
        UserRequest request = new UserRequest("teacher@example.com", "plain", Set.of(Role.TEACHER));
        when(userMapper.toEntity(request)).thenReturn(user);
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(false);
        when(passwordEncoder.encode("hashed-password")).thenReturn("encoded-password");

        assertTrue(userService.register(request));
        assertEquals("encoded-password", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void registerReturnsFalseForDuplicateEmail() {
        UserRequest request = new UserRequest("teacher@example.com", "plain", Set.of(Role.TEACHER));
        when(userMapper.toEntity(request)).thenReturn(user);
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(true);

        assertFalse(userService.register(request));
        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void registerReturnsFalseWhenMapperReturnsNull() {
        UserRequest request = new UserRequest();
        when(userMapper.toEntity(request)).thenReturn(null);
        assertFalse(userService.register(request));
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void registerReturnsFalseWhenMappedEmailIsNull() {
        UserRequest request = new UserRequest();
        User noEmail = new User();
        when(userMapper.toEntity(request)).thenReturn(noEmail);
        assertFalse(userService.register(request));
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void findByEmailReturnsMatchingUser() {
        when(userRepository.findByEmail("teacher@example.com")).thenReturn(Optional.of(user));
        assertSame(user, userService.findByEmail("teacher@example.com"));
    }

    @Test
    void findByEmailThrowsWhenMissing() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> userService.findByEmail("missing@example.com"));
    }

    @Test
    void findAllMapsRepositoryResults() {
        UserResponse response = new UserResponse(1L, user.getEmail(), user.getRoles());
        UserResponseList list = new UserResponseList(List.of(response), 1);
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(userMapper.toListResponse(List.of(user))).thenReturn(list);
        assertSame(list, userService.findAll());
        verify(userMapper).toListResponse(List.of(user));
    }

    @Test
    void saveEncodesPasswordPersistsUserAndMapsResponse() {
        UserRequest request = new UserRequest("new@example.com", "plain", Set.of(Role.ADMIN));
        User persisted = User.builder().id(2L).email("new@example.com").password("encoded")
                .roles(Set.of(Role.ADMIN)).build();
        UserResponse expected = new UserResponse(2L, "new@example.com", Set.of(Role.ADMIN));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plain")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(persisted);
        when(userMapper.toResponse(persisted)).thenReturn(expected);

        assertSame(expected, userService.save(request));
        verify(userRepository).save(argThat(saved -> saved.getEmail().equals("new@example.com")
                && saved.getPassword().equals("encoded") && saved.getRoles().equals(Set.of(Role.ADMIN))));
    }

    @Test
    void saveThrowsWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("teacher@example.com")).thenReturn(true);
        assertThrows(EmailAlreadyExistsException.class, () -> userService.save(
                new UserRequest("teacher@example.com", "plain", Set.of(Role.TEACHER))));
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserPersistsChangedRolesAndReturnsResponse() {
        UserRequest request = new UserRequest("teacher@example.com", "ignored", Set.of(Role.ADMIN));
        UserResponse expected = new UserResponse(1L, "teacher@example.com", Set.of(Role.ADMIN));
        when(userRepository.findByEmail("teacher@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(expected);

        assertSame(expected, userService.updateUser(request));
        assertEquals(Set.of(Role.ADMIN), user.getRoles());
        verify(userRepository).save(user);
        verify(userMapper).toResponse(user);
    }

    @Test
    void deleteByIdDelegatesToRepository() {
        userService.deleteById(9L);
        verify(userRepository).deleteById(9L);
    }
}
