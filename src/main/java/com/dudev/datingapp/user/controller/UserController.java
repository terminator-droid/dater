package com.dudev.datingapp.user.controller;

import com.dudev.datingapp.common.ApiResponse;
import com.dudev.datingapp.user.dto.PhotoDto;
import com.dudev.datingapp.user.dto.PublicProfileDto;
import com.dudev.datingapp.user.dto.UpdateProfileDto;
import com.dudev.datingapp.user.dto.UserProfileDto;
import com.dudev.datingapp.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Profile management and photos")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get own profile")
    public ApiResponse<UserProfileDto> getProfile(Authentication auth) {
        return ApiResponse.ok(userService.getProfile(currentUserId(auth)));
    }

    @PutMapping("/me")
    @Operation(summary = "Update own profile")
    public ApiResponse<UserProfileDto> updateProfile(Authentication auth,
                                                     @Valid @RequestBody UpdateProfileDto dto) {
        return ApiResponse.ok(userService.updateProfile(currentUserId(auth), dto));
    }

    @PostMapping(value = "/me/photos",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a profile photo")
    public ApiResponse<PhotoDto> uploadPhoto(Authentication auth,
                                             @RequestPart("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(userService.addPhoto(currentUserId(auth), file));
    }

    @DeleteMapping("/me/photos/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a profile photo")
    public void deletePhoto(Authentication auth, @PathVariable UUID photoId) throws IOException {
        userService.deletePhoto(currentUserId(auth), photoId);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get another user's public profile (photos only)")
    public ApiResponse<PublicProfileDto> getPublicProfile(@PathVariable UUID userId) {
        return ApiResponse.ok(userService.getPublicProfile(userId));
    }

    @GetMapping("/me/online")
    @Operation(summary = "Check own online status")
    public ApiResponse<Boolean> isOnline(Authentication auth) {
        return ApiResponse.ok(userService.isOnline(currentUserId(auth)));
    }

    @PatchMapping("/me/location")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Update current location in geo index (for discovery without a full plan)")
    public void updateLocation(Authentication auth,
                               @Valid @RequestBody com.dudev.datingapp.user.dto.LocationUpdateDto dto) {
        userService.updateLocation(currentUserId(auth), dto);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete own account (cascades plans, swipes, matches)")
    public void deleteAccount(Authentication auth) {
        userService.deleteAccount(currentUserId(auth));
    }

    private UUID currentUserId(Authentication auth) {
        return UUID.fromString(auth.getName());
    }
}
