package com.example.demo.controller;

import com.example.demo.Repository.FriendshipRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.model.Friendship;
import com.example.demo.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FriendController {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    /**
     * 1. LẤY DANH SÁCH BẠN BÈ CỦA TÔI
     */
    @GetMapping
    public ResponseEntity<?> getMyFriends(Principal principal) {
        try {
            User currentUser = userRepository.findByUsername(principal.getName())
                    .or(() -> userRepository.findByEmail(principal.getName()))
                    .orElseThrow(() -> new RuntimeException("Chưa đăng nhập"));

            List<Friendship> friendships = friendshipRepository.findByUser(currentUser);

            List<Map<String, Object>> responseList = friendships.stream().map(f -> {
                User friend = f.getFriend();
                Map<String, Object> map = new HashMap<>();
                map.put("friendshipId", f.getId());
                map.put("userId", friend.getId());
                map.put("username", friend.getUsername());
                map.put("fullName", friend.getFullName() != null ? friend.getFullName() : friend.getUsername());
                map.put("email", friend.getEmail());
                map.put("userCode", friend.getUserCode());
                map.put("avatarUrl", friend.getAvatarUrl());
                map.put("status", f.getStatus());
                map.put("createdAt", f.getCreatedAt());
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi tải bạn bè: " + e.getMessage()));
        }
    }

    /**
     * 2. KẾT BẠN (Từ Quét QR hoặc Nhập Mã số riêng / Username / Email)
     */
    @PostMapping("/add")
    @Transactional
    public ResponseEntity<?> addFriend(@RequestBody Map<String, String> body, Principal principal) {
        try {
            User currentUser = userRepository.findByUsername(principal.getName())
                    .or(() -> userRepository.findByEmail(principal.getName()))
                    .orElseThrow(() -> new RuntimeException("Chưa đăng nhập"));

            String identifier = body.get("identifier");
            if (identifier == null || identifier.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng cung cấp mã QR, mã số riêng hoặc username/email"));
            }

            String raw = identifier != null ? identifier.trim() : "";
            final String cleanIdentifier = raw.startsWith("AURA_USER:") 
                    ? raw.substring("AURA_USER:".length()).trim() 
                    : raw;
            final String cleanCode = cleanIdentifier.startsWith("#") 
                    ? cleanIdentifier.substring(1).trim() 
                    : cleanIdentifier;

            User targetFriend = userRepository.findByIdentifier(cleanCode)
                    .or(() -> userRepository.findByIdentifier(cleanIdentifier))
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với mã hoặc thông tin: " + cleanIdentifier));

            if (currentUser.getId().equals(targetFriend.getId())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Bạn không thể kết bạn với chính mình!"));
            }

            // Kiểm tra xem đã kết bạn chưa
            if (friendshipRepository.existsByUserAndFriend(currentUser, targetFriend)) {
                return ResponseEntity.ok(Map.of(
                        "message", "Hai bạn đã là bạn bè từ trước!",
                        "alreadyFriend", true
                ));
            }

            // Tạo quan hệ bạn bè 2 chiều để cả 2 cùng thấy nhau trong danh sách bạn bè
            Friendship f1 = Friendship.builder()
                    .user(currentUser)
                    .friend(targetFriend)
                    .status("ACCEPTED")
                    .build();
            friendshipRepository.save(f1);

            if (!friendshipRepository.existsByUserAndFriend(targetFriend, currentUser)) {
                Friendship f2 = Friendship.builder()
                        .user(targetFriend)
                        .friend(currentUser)
                        .status("ACCEPTED")
                        .build();
                friendshipRepository.save(f2);
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Kết bạn thành công với " + (targetFriend.getFullName() != null ? targetFriend.getFullName() : targetFriend.getUsername()) + "!",
                    "friend", Map.of(
                            "userId", targetFriend.getId(),
                            "username", targetFriend.getUsername(),
                            "fullName", targetFriend.getFullName() != null ? targetFriend.getFullName() : targetFriend.getUsername(),
                            "email", targetFriend.getEmail(),
                            "userCode", targetFriend.getUserCode()
                    )
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi kết bạn: " + e.getMessage()));
        }
    }

    /**
     * 3. XEM THÔNG TIN NGƯỜI DÙNG TỪ MÃ QR HOẶC MÃ SỐ RIÊNG
     */
    @GetMapping("/qr-info")
    public ResponseEntity<?> getQRUserInfo(@RequestParam("code") String code, Principal principal) {
        try {
            User currentUser = userRepository.findByUsername(principal.getName())
                    .or(() -> userRepository.findByEmail(principal.getName()))
                    .orElseThrow(() -> new RuntimeException("Chưa đăng nhập"));

            String rawCode = code != null ? code.trim() : "";
            final String cleanPrefix = rawCode.startsWith("AURA_USER:")
                    ? rawCode.substring("AURA_USER:".length()).trim()
                    : rawCode;
            final String cleanCode = cleanPrefix.startsWith("#")
                    ? cleanPrefix.substring(1).trim()
                    : cleanPrefix;

            User target = userRepository.findByIdentifier(cleanCode)
                    .or(() -> userRepository.findByIdentifier(cleanPrefix))
                    .orElseThrow(() -> new RuntimeException("Mã QR hoặc mã số riêng không hợp lệ / không tìm thấy người dùng!"));

            boolean isSelf = currentUser.getId().equals(target.getId());
            boolean isAlreadyFriend = friendshipRepository.existsByUserAndFriend(currentUser, target);

            Map<String, Object> map = new HashMap<>();
            map.put("userId", target.getId());
            map.put("username", target.getUsername());
            map.put("fullName", target.getFullName() != null ? target.getFullName() : target.getUsername());
            map.put("email", target.getEmail());
            map.put("userCode", target.getUserCode());
            map.put("avatarUrl", target.getAvatarUrl());
            map.put("isSelf", isSelf);
            map.put("isAlreadyFriend", isAlreadyFriend);

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 4. HỦY KẾT BẠN
     */
    @DeleteMapping("/{friendId}")
    @Transactional
    public ResponseEntity<?> removeFriend(@PathVariable Long friendId, Principal principal) {
        try {
            User currentUser = userRepository.findByUsername(principal.getName())
                    .or(() -> userRepository.findByEmail(principal.getName()))
                    .orElseThrow(() -> new RuntimeException("Chưa đăng nhập"));

            User friend = userRepository.findById(friendId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng này"));

            friendshipRepository.findByUserAndFriend(currentUser, friend).ifPresent(friendshipRepository::delete);
            friendshipRepository.findByUserAndFriend(friend, currentUser).ifPresent(friendshipRepository::delete);

            return ResponseEntity.ok(Map.of("message", "Đã hủy kết bạn!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }
}
