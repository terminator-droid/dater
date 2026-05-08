package com.dudev.datingapp.venue;

import com.dudev.datingapp.common.exception.ResourceNotFoundException;
import com.dudev.datingapp.venue.dto.VenueDto;
import com.dudev.datingapp.venue.entity.Venue;
import com.dudev.datingapp.venue.entity.VenueCategory;
import com.dudev.datingapp.venue.repository.VenueRepository;
import com.dudev.datingapp.venue.service.VenueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VenueServiceTest {

    @Mock private VenueRepository venueRepository;

    private VenueService venueService;

    @BeforeEach
    void setUp() {
        venueService = new VenueService(venueRepository);
    }

    @Test
    void findAll_nullArea_returnsAllVenues() {
        when(venueRepository.findAll()).thenReturn(List.of(buildVenue("32.05", "Патриаршие")));

        List<VenueDto> result = venueService.findAll(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("32.05");
    }

    @Test
    void findAll_withArea_filtersVenues() {
        when(venueRepository.findByArea("Патриаршие")).thenReturn(List.of(buildVenue("32.05", "Патриаршие")));

        List<VenueDto> result = venueService.findAll("Патриаршие");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).area()).isEqualTo("Патриаршие");
    }

    @Test
    void findAll_blankArea_returnsAllVenues() {
        when(venueRepository.findAll()).thenReturn(List.of(buildVenue("Noor", "Патриаршие")));

        List<VenueDto> result = venueService.findAll("  ");

        assertThat(result).hasSize(1);
    }

    @Test
    void findById_returnsVenue() {
        UUID id = UUID.randomUUID();
        Venue venue = buildVenue("32.05", "Патриаршие");
        ReflectionTestUtils.setField(venue, "id", id);
        when(venueRepository.findById(id)).thenReturn(Optional.of(venue));

        VenueDto result = venueService.findById(id);

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.name()).isEqualTo("32.05");
    }

    @Test
    void findById_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(venueRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> venueService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Venue buildVenue(String name, String area) {
        Venue v = new Venue();
        v.setName(name);
        v.setAddress("ул. Тестовая, 1");
        v.setLatitude(55.764);
        v.setLongitude(37.593);
        v.setArea(area);
        v.setCategory(VenueCategory.BAR);
        return v;
    }
}
