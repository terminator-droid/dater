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
}
