package com.example.demo.Repository;

import com.example.demo.model.SystemAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemAnnouncementRepository extends JpaRepository<SystemAnnouncement, Long> {

    List<SystemAnnouncement> findByIsActiveTrueOrderByCreatedAtDesc();

    List<SystemAnnouncement> findAllByOrderByCreatedAtDesc();
}
