package io.mywallet.user.interfaces.rest;

import io.mywallet.user.infrastructure.persistence.RoleEntity;
import io.mywallet.user.infrastructure.persistence.UserEntity;
import io.mywallet.user.infrastructure.persistence.UserJpaRepository;
import io.mywallet.user.interfaces.rest.dto.UserProfileResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users")
public class UserController {

    private final UserJpaRepository userRepository;

    public UserController(UserJpaRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public UserProfileResponse me(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("Authenticated user not found: " + userId));

        return new UserProfileResponse(
            user.getId(),
            user.getEmail(),
            user.getRoles().stream().map(RoleEntity::getName).toList()
        );
    }
}
