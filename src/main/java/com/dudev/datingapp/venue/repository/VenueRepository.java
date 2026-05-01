package com.dudev.datingapp.venue.repository;

import com.dudev.datingapp.venue.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface VenueRepository extends JpaRepository<Venue, UUID> {

    List<Venue> findByArea(String area);

    List<Venue> findByIdIn(Collection<UUID> ids);
}
