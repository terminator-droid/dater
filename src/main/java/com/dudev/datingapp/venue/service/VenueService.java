package com.dudev.datingapp.venue.service;

import com.dudev.datingapp.common.exception.ResourceNotFoundException;
import com.dudev.datingapp.venue.dto.VenueDto;
import com.dudev.datingapp.venue.entity.Venue;
import com.dudev.datingapp.venue.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VenueService {

    private final VenueRepository venueRepository;

    @Transactional(readOnly = true)
    public List<VenueDto> findAll(String area) {
        List<Venue> venues = (area != null && !area.isBlank())
                ? venueRepository.findByArea(area)
                : venueRepository.findAll();
        return venues.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public VenueDto findById(UUID id) {
        return venueRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<VenueDto> findNearby(double lat, double lon, double radiusKm) {
        return venueRepository.findAll().stream()
                .filter(v -> haversineKm(lat, lon, v.getLatitude(), v.getLongitude()) <= radiusKm)
                .sorted(java.util.Comparator.comparingDouble(
                        v -> haversineKm(lat, lon, v.getLatitude(), v.getLongitude())))
                .map(this::toDto)
                .toList();
    }

    public VenueDto toDto(Venue venue) {
        return new VenueDto(
                venue.getId(),
                venue.getName(),
                venue.getAddress(),
                venue.getLatitude(),
                venue.getLongitude(),
                venue.getArea(),
                venue.getCategory()
        );
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
