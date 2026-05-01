package com.dudev.datingapp.user.controller;

import com.dudev.datingapp.common.ApiResponse;
import com.dudev.datingapp.user.dto.PhotoDto;
import com.dudev.datingapp.user.dto.UpdateProfileDto;
import com.dudev.datingapp.user.dto.UserProfileDto;
import com.dudev.datingapp.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserProfileDto> getProfile(Authentication auth) {
        return ApiResponse.ok(userService.getProfile(currentUserId(auth)));
    }

    @PutMapping("/me")
    public ApiResponse<UserProfileDto> updateProfile(Authentication auth,
                                                      @Valid @RequestBody UpdateProfileDto dto) {
        return ApiResponse.ok(userService.updateProfile(currentUserId(auth), dto));
    }

    @PostMapping("/me/photos")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PhotoDto> uploadPhoto(Authentication auth,
                                              @RequestPart("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(userService.addPhoto(currentUserId(auth), file));
    }

    @DeleteMapping("/me/photos/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePhoto(Authentication auth, @PathVariable UUID photoId) throws IOException {
        userService.deletePhoto(currentUserId(auth), photoId);
    }

    private UUID currentUserId(Authentication auth) {
        return UUID.fromString(auth.getName());
    }
}
