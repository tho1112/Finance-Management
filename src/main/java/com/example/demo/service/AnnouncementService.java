package com.example.demo.service;

import com.example.demo.Repository.SystemAnnouncementRepository;
import com.example.demo.dto.admin.AnnouncementRequest;
import com.example.demo.model.SystemAnnouncement;
import com.example.demo.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnouncementService {

    private final SystemAnnouncementRepository announcementRepository;
    private final AdminService adminService;

    public List<SystemAnnouncement> getActiveAnnouncements() {
        return announcementRepository.findByIsActiveTrueOrderByCreatedAtDesc();
    }

    public List<SystemAnnouncement> getAllAnnouncements() {
        return announcementRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public SystemAnnouncement createAnnouncement(User adminUser, AnnouncementRequest request, String ipAddress) {
        SystemAnnouncement announcement = SystemAnnouncement.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .type(request.getType() != null ? request.getType() : "INFO")
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .createdBy(adminUser)
                .build();

        SystemAnnouncement saved = announcementRepository.save(announcement);

        adminService.recordAuditLog(adminUser, "CREATE_ANNOUNCEMENT", "ANNOUNCEMENT", saved.getId(),
                "Tạo thông báo toàn hệ thống: " + saved.getTitle(), ipAddress);

        return saved;
    }

    @Transactional
    public SystemAnnouncement toggleAnnouncement(Long id, User adminUser, String ipAddress) {
        SystemAnnouncement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo"));

        announcement.setIsActive(!Boolean.TRUE.equals(announcement.getIsActive()));
        SystemAnnouncement saved = announcementRepository.save(announcement);

        adminService.recordAuditLog(adminUser, "TOGGLE_ANNOUNCEMENT", "ANNOUNCEMENT", id,
                "Đổi trạng thái thông báo thành: " + saved.getIsActive(), ipAddress);

        return saved;
    }

    @Transactional
    public void deleteAnnouncement(Long id, User adminUser, String ipAddress) {
        SystemAnnouncement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo"));

        announcementRepository.delete(announcement);
        adminService.recordAuditLog(adminUser, "DELETE_ANNOUNCEMENT", "ANNOUNCEMENT", id,
                "Xóa thông báo: " + announcement.getTitle(), ipAddress);
    }
}
