package com.example.demo.controller;

import com.example.demo.Repository.*;
import com.example.demo.dto.*;
import com.example.demo.model.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GroupController {

    private final AppGroupRepository appGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupTransactionRepository groupTransactionRepository;
    private final GroupTransactionSplitRepository groupTransactionSplitRepository;
    private final TransactionCommentRepository transactionCommentRepository;
    private final UserRepository userRepository;

    /**
     * 1. TẠO NHÓM CHI TIÊU
     */
    @PostMapping
    @Transactional
    public ResponseEntity<?> createGroup(
            @Valid @RequestBody GroupRequest request,
            BindingResult result,
            Principal principal) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("message", result.getAllErrors().get(0).getDefaultMessage()));
        }

        try {
            User user = userRepository.findByUsername(principal.getName())
                    .or(() -> userRepository.findByEmail(principal.getName()))
                    .orElseThrow();

            AppGroup group = AppGroup.builder()
                    .name(request.getName().trim())
                    .createdBy(user)
                    .build();
            AppGroup savedGroup = appGroupRepository.save(group);

            // Thêm người tạo làm ADMIN
            GroupMember member = GroupMember.builder()
                    .id(new GroupMemberId(savedGroup.getId(), user.getId()))
                    .group(savedGroup)
                    .user(user)
                    .role("ADMIN")
                    .build();
            groupMemberRepository.save(member);

            return ResponseEntity.ok(Map.of(
                    "message", "Tạo nhóm thành công!",
                    "groupId", savedGroup.getId(),
                    "groupName", savedGroup.getName()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi tạo nhóm: " + e.getMessage()));
        }
    }

    /**
     * 2. LẤY DANH SÁCH NHÓM CỦA NGƯỜI DÙNG
     */
    @GetMapping
    public ResponseEntity<?> getMyGroups(Principal principal) {
        try {
            User user = userRepository.findByUsername(principal.getName())
                    .or(() -> userRepository.findByEmail(principal.getName()))
                    .orElseThrow();

            List<AppGroup> groups = appGroupRepository.findAllByMemberUserId(user.getId());

            List<GroupResponse> responseList = groups.stream().map(g -> {
                List<GroupMember> members = groupMemberRepository.findByGroupId(g.getId());
                List<GroupMemberDTO> memberDTOs = members.stream().map(m ->
                        GroupMemberDTO.builder()
                                .userId(m.getUser().getId())
                                .username(m.getUser().getUsername())
                                .fullName(m.getUser().getFullName())
                                .avatarUrl(m.getUser().getAvatarUrl())
                                .role(m.getRole())
                                .nickname(m.getNickname())
                                .userCode(m.getUser().getUserCode())
                                .joinedAt(m.getJoinedAt())
                                .build()
                ).collect(Collectors.toList());

                return GroupResponse.builder()
                        .id(g.getId())
                        .name(g.getName())
                        .createdById(g.getCreatedBy().getId())
                        .createdByName(g.getCreatedBy().getFullName() != null ? g.getCreatedBy().getFullName() : g.getCreatedBy().getUsername())
                        .createdAt(g.getCreatedAt())
                        .members(memberDTOs)
                        .build();
            }).collect(Collectors.toList());

            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    /**
     * 3. THÊM THÀNH VIÊN VÀO NHÓM (Theo mã số riêng, email hoặc username)
     */
    @PostMapping("/{groupId}/members")
    public ResponseEntity<?> addMember(
            @PathVariable Long groupId,
            @RequestBody Map<String, String> body,
            Principal principal) {
        try {
            AppGroup group = appGroupRepository.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Nhóm không tồn tại"));

            String identifier = body.get("identifier");
            if (identifier == null || identifier.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng nhập mã số riêng, email hoặc username của thành viên"));
            }

            String raw = identifier.trim();
            String clean = raw.startsWith("#") ? raw.substring(1).trim() : raw;
            if (clean.startsWith("AURA_USER:")) clean = clean.substring("AURA_USER:".length()).trim();

            User targetUser = userRepository.findByIdentifier(clean)
                    .or(() -> userRepository.findByIdentifier(raw))
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với mã hoặc thông tin: " + raw));

            GroupMemberId memberId = new GroupMemberId(group.getId(), targetUser.getId());
            if (groupMemberRepository.existsById(memberId)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Thành viên này đã ở trong nhóm!"));
            }

            GroupMember newMember = GroupMember.builder()
                    .id(memberId)
                    .group(group)
                    .user(targetUser)
                    .role("MEMBER")
                    .build();
            groupMemberRepository.save(newMember);

            return ResponseEntity.ok(Map.of(
                    "message", "Đã thêm thành viên vào nhóm!",
                    "member", Map.of(
                            "userId", targetUser.getId(),
                            "username", targetUser.getUsername(),
                            "fullName", targetUser.getFullName() != null ? targetUser.getFullName() : targetUser.getUsername(),
                            "userCode", targetUser.getUserCode()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    /**
     * 4. TẠO GIAO DỊCH NHÓM & CHIA TIỀN
     */
    @PostMapping("/{groupId}/transactions")
    @Transactional
    public ResponseEntity<?> createGroupTransaction(
            @PathVariable Long groupId,
            @Valid @RequestBody GroupTransactionRequest request,
            BindingResult result,
            Principal principal) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("message", result.getAllErrors().get(0).getDefaultMessage()));
        }

        try {
            User currentUser = userRepository.findByUsername(principal.getName())
                    .or(() -> userRepository.findByEmail(principal.getName()))
                    .orElseThrow();

            AppGroup group = appGroupRepository.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm!"));

            // Kiểm tra phân quyền: currentUser phải là thành viên nhóm
            if (!groupMemberRepository.existsById(new GroupMemberId(group.getId(), currentUser.getId()))) {
                return ResponseEntity.status(403).body(Map.of("message", "Bạn không có quyền tạo giao dịch trong nhóm này!"));
            }

            User payer = currentUser;
            if (request.getPayerId() != null) {
                payer = userRepository.findById(request.getPayerId()).orElse(currentUser);
            }

            LocalDateTime txDate = request.getTransactionDate() != null && !request.getTransactionDate().isBlank()
                    ? (request.getTransactionDate().length() == 10 ? LocalDateTime.parse(request.getTransactionDate() + "T00:00:00") : LocalDateTime.parse(request.getTransactionDate()))
                    : LocalDateTime.now();

            GroupTransaction groupTx = GroupTransaction.builder()
                    .group(group)
                    .payer(payer)
                    .amount(request.getAmount())
                    .description(request.getDescription())
                    .transactionDate(txDate)
                    .splitType(request.getSplitType() != null ? request.getSplitType() : "EQUAL")
                    .build();

            GroupTransaction savedGroupTx = groupTransactionRepository.save(groupTx);

            // Xử lý chia tiền: Nếu không chỉ định cụ thể, chia đều cho tất cả thành viên trong nhóm
            List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
            List<GroupTransactionSplit> splits = new ArrayList<>();

            if (request.getSplits() != null && !request.getSplits().isEmpty()) {
                for (SplitDetailDTO s : request.getSplits()) {
                    User u = userRepository.findById(s.getUserId()).orElseThrow();
                    splits.add(GroupTransactionSplit.builder()
                            .groupTransaction(savedGroupTx)
                            .user(u)
                            .owedAmount(s.getOwedAmount())
                            .isSettled(s.getIsSettled() != null ? s.getIsSettled() : u.getId().equals(payer.getId()))
                            .build());
                }
            } else if (!members.isEmpty()) {
                BigDecimal count = BigDecimal.valueOf(members.size());
                // Chia theo số nguyên VNĐ (làm tròn xuống)
                BigDecimal perPerson = request.getAmount().divide(count, 0, RoundingMode.DOWN);
                BigDecimal totalDistributed = perPerson.multiply(count);
                BigDecimal remainder = request.getAmount().subtract(totalDistributed);

                for (int i = 0; i < members.size(); i++) {
                    GroupMember m = members.get(i);
                    boolean isPayer = m.getUser().getId().equals(payer.getId());
                    // Người đầu tiên nhận phần dư lẻ nếu có để tổng tiền khớp 100%
                    BigDecimal owed = (i == 0) ? perPerson.add(remainder) : perPerson;
                    splits.add(GroupTransactionSplit.builder()
                            .groupTransaction(savedGroupTx)
                            .user(m.getUser())
                            .owedAmount(owed)
                            .isSettled(isPayer)
                            .build());
                }
            }
            groupTransactionSplitRepository.saveAll(splits);

            return ResponseEntity.ok(Map.of(
                    "message", "Đã tạo giao dịch nhóm và phân bổ chia tiền thành công!",
                    "groupTransactionId", savedGroupTx.getId()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    /**
     * 5. LẤY DANH SÁCH GIAO DỊCH NHÓM
     */
    @GetMapping("/{groupId}/transactions")
    public ResponseEntity<?> getGroupTransactions(@PathVariable Long groupId, Principal principal) {
        try {
            User currentUser = userRepository.findByUsername(principal.getName())
                    .or(() -> userRepository.findByEmail(principal.getName()))
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại!"));

            if (!groupMemberRepository.existsById(new GroupMemberId(groupId, currentUser.getId()))) {
                return ResponseEntity.status(403).body(Map.of("message", "Bạn không có quyền xem giao dịch nhóm này!"));
            }

            List<GroupTransaction> transactions = groupTransactionRepository.findByGroupIdOrderByTransactionDateDesc(groupId);

            List<GroupTransactionResponse> responseList = transactions.stream().map(gt -> {
                List<GroupTransactionSplit> splits = groupTransactionSplitRepository.findByGroupTransactionId(gt.getId());
                List<SplitDetailDTO> splitDTOs = splits.stream().map(s ->
                        SplitDetailDTO.builder()
                                .userId(s.getUser().getId())
                                .username(s.getUser().getUsername())
                                .fullName(s.getUser().getFullName())
                                .owedAmount(s.getOwedAmount())
                                .isSettled(s.getIsSettled())
                                .build()
                ).collect(Collectors.toList());

                List<TransactionComment> comments = transactionCommentRepository.findByGroupTransactionIdOrderByCreatedAtAsc(gt.getId());
                List<CommentResponse> commentDTOs = comments.stream().map(c -> {
                    String authorName = (c.getUser().getFullName() != null && !c.getUser().getFullName().isBlank())
                            ? c.getUser().getFullName() : c.getUser().getUsername();
                    String payer = (gt.getPayer().getFullName() != null && !gt.getPayer().getFullName().isBlank())
                            ? gt.getPayer().getFullName() : gt.getPayer().getUsername();
                    return CommentResponse.builder()
                            .id(c.getId())
                            .groupId(groupId)
                            .groupTransactionId(gt.getId())
                            .transactionDescription(gt.getDescription())
                            .transactionAmount(gt.getAmount() != null ? gt.getAmount().doubleValue() : null)
                            .transactionPayerName(payer)
                            .userId(c.getUser().getId())
                            .userName(authorName)
                            .userAvatar(c.getUser().getAvatarUrl())
                            .content(c.getContent())
                            .createdAt(c.getCreatedAt())
                            .build();
                }).collect(Collectors.toList());

                return GroupTransactionResponse.builder()
                        .id(gt.getId())
                        .groupId(groupId)
                        .payerId(gt.getPayer().getId())
                        .payerName(gt.getPayer().getFullName() != null ? gt.getPayer().getFullName() : gt.getPayer().getUsername())
                        .payerAvatar(gt.getPayer().getAvatarUrl())
                        .amount(gt.getAmount())
                        .description(gt.getDescription())
                        .transactionDate(gt.getTransactionDate())
                        .splitType(gt.getSplitType())
                        .createdAt(gt.getCreatedAt())
                        .splits(splitDTOs)
                        .comments(commentDTOs)
                        .build();
            }).collect(Collectors.toList());

            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    /**
     * 6. LẤY TẤT CẢ BÌNH LUẬN / THẢO LUẬN CỦA NHÓM (Group Discussion)
     */
    @GetMapping("/{groupId}/comments")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getGroupComments(@PathVariable Long groupId, Principal principal) {
        try {
            User currentUser = userRepository.findByUsername(principal.getName())
                    .or(() -> userRepository.findByEmail(principal.getName()))
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại!"));

            if (!groupMemberRepository.existsById(new GroupMemberId(groupId, currentUser.getId()))) {
                return ResponseEntity.status(403).body(Map.of("message", "Bạn không có quyền xem bình luận nhóm này!"));
            }

            List<TransactionComment> comments = transactionCommentRepository.findByGroupIdOrderByCreatedAtAsc(groupId);

            Map<Long, String> memberNicknames = groupMemberRepository.findByGroupId(groupId).stream()
                    .filter(m -> m.getNickname() != null && !m.getNickname().isBlank())
                    .collect(Collectors.toMap(m -> m.getUser().getId(), GroupMember::getNickname, (existing, replacement) -> existing));

            List<CommentResponse> responseList = comments.stream().map(c -> {
                GroupTransaction tx = c.getGroupTransaction();
                String authorName = (c.getUser().getFullName() != null && !c.getUser().getFullName().isBlank())
                        ? c.getUser().getFullName() : c.getUser().getUsername();
                String payer = null;
                if (tx != null && tx.getPayer() != null) {
                    payer = (tx.getPayer().getFullName() != null && !tx.getPayer().getFullName().isBlank())
                            ? tx.getPayer().getFullName() : tx.getPayer().getUsername();
                }

                return CommentResponse.builder()
                        .id(c.getId())
                        .groupId(groupId)
                        .groupTransactionId(tx != null ? tx.getId() : null)
                        .transactionDescription(tx != null ? tx.getDescription() : null)
                        .transactionAmount(tx != null && tx.getAmount() != null ? tx.getAmount().doubleValue() : null)
                        .transactionPayerName(payer)
                        .userId(c.getUser().getId())
                        .userName(authorName)
                        .userNickname(memberNicknames.get(c.getUser().getId()))
                        .userAvatar(c.getUser().getAvatarUrl())
                        .content(c.getContent())
                        .createdAt(c.getCreatedAt())
                        .build();
            }).collect(Collectors.toList());

            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    /**
     * 7. THÊM BÌNH LUẬN VÀO NHÓM (Group Discussion)
     */
    @PostMapping("/{groupId}/comments")
    @Transactional
    public ResponseEntity<?> addGroupComment(
            @PathVariable Long groupId,
            @Valid @RequestBody CommentRequest request,
            BindingResult result,
            Principal principal) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("message", result.getAllErrors().get(0).getDefaultMessage()));
        }

        try {
            User user = userRepository.findByUsername(principal.getName())
                    .or(() -> userRepository.findByEmail(principal.getName()))
                    .orElseThrow();

            AppGroup group = appGroupRepository.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Nhóm không tồn tại!"));

            if (!groupMemberRepository.existsById(new GroupMemberId(groupId, user.getId()))) {
                return ResponseEntity.status(403).body(Map.of("message", "Bạn không có quyền bình luận trong nhóm này!"));
            }

            GroupTransaction tx = null;
            if (request.getGroupTransactionId() != null) {
                tx = groupTransactionRepository.findById(request.getGroupTransactionId()).orElse(null);
            }

            TransactionComment comment = TransactionComment.builder()
                    .group(group)
                    .groupTransaction(tx)
                    .user(user)
                    .content(request.getContent().trim())
                    .build();

            TransactionComment saved = transactionCommentRepository.save(comment);

            String authorName = (user.getFullName() != null && !user.getFullName().isBlank())
                    ? user.getFullName() : user.getUsername();

            return ResponseEntity.ok(Map.of(
                    "message", "Đã gửi bình luận thảo luận thành công!",
                    "commentId", saved.getId(),
                    "userName", authorName,
                    "content", saved.getContent(),
                    "createdAt", saved.getCreatedAt()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    /**
     * 8. THÊM BÌNH LUẬN VÀO GIAO DỊCH CỤ THỂ
     */
    @PostMapping("/transactions/{txId}/comments")
    @Transactional
    public ResponseEntity<?> addTransactionComment(
            @PathVariable Long txId,
            @Valid @RequestBody CommentRequest request,
            BindingResult result,
            Principal principal) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("message", result.getAllErrors().get(0).getDefaultMessage()));
        }

        try {
            User user = userRepository.findByUsername(principal.getName())
                    .or(() -> userRepository.findByEmail(principal.getName()))
                    .orElseThrow();

            GroupTransaction tx = groupTransactionRepository.findById(txId)
                    .orElseThrow(() -> new RuntimeException("Giao dịch không tồn tại!"));

            if (!groupMemberRepository.existsById(new GroupMemberId(tx.getGroup().getId(), user.getId()))) {
                return ResponseEntity.status(403).body(Map.of("message", "Bạn không có quyền bình luận giao dịch này!"));
            }

            TransactionComment comment = TransactionComment.builder()
                    .group(tx.getGroup())
                    .groupTransaction(tx)
                    .user(user)
                    .content(request.getContent().trim())
                    .build();

            TransactionComment saved = transactionCommentRepository.save(comment);

            String authorName = (user.getFullName() != null && !user.getFullName().isBlank())
                    ? user.getFullName() : user.getUsername();

            return ResponseEntity.ok(Map.of(
                    "message", "Bình luận thành công!",
                    "commentId", saved.getId(),
                    "userName", authorName,
                    "content", saved.getContent(),
                    "createdAt", saved.getCreatedAt()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    /**
     * 9. ĐẶT BIỆT DANH CHO THÀNH VIÊN TRONG NHÓM
     */
    @PutMapping("/{groupId}/members/{userId}/nickname")
    @Transactional
    public ResponseEntity<?> updateMemberNickname(
            @PathVariable Long groupId,
            @PathVariable Long userId,
            @RequestBody Map<String, String> body,
            Principal principal) {
        try {
            User currentUser = userRepository.findByUsername(principal.getName())
                    .or(() -> userRepository.findByEmail(principal.getName()))
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại!"));

            if (!groupMemberRepository.existsById(new GroupMemberId(groupId, currentUser.getId()))) {
                return ResponseEntity.status(403).body(Map.of("message", "Bạn không phải thành viên nhóm!"));
            }

            GroupMember member = groupMemberRepository.findById(new GroupMemberId(groupId, userId))
                    .orElseThrow(() -> new RuntimeException("Thành viên không tồn tại trong nhóm!"));

            String nickname = body != null ? body.get("nickname") : null;
            if (nickname != null) {
                nickname = nickname.trim();
                if (nickname.isEmpty()) {
                    nickname = null;
                }
            }
            member.setNickname(nickname);
            groupMemberRepository.save(member);

            return ResponseEntity.ok(Map.of("message", "Cập nhật biệt danh thành công!", "nickname", nickname != null ? nickname : ""));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    /**
     * 10. RỜI NHÓM
     */
    @DeleteMapping("/{groupId}/leave")
    @Transactional
    public ResponseEntity<?> leaveGroup(@PathVariable Long groupId, Principal principal) {
        try {
            User currentUser = userRepository.findByUsername(principal.getName())
                    .or(() -> userRepository.findByEmail(principal.getName()))
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại!"));

            AppGroup group = appGroupRepository.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Nhóm không tồn tại!"));

            GroupMember member = groupMemberRepository.findById(new GroupMemberId(groupId, currentUser.getId()))
                    .orElseThrow(() -> new RuntimeException("Bạn không phải thành viên của nhóm này!"));

            List<GroupMember> allMembers = groupMemberRepository.findByGroupId(groupId);

            if (allMembers.size() <= 1) {
                deleteGroupCascade(group);
                return ResponseEntity.ok(Map.of("message", "Bạn đã rời nhóm và nhóm đã được giải tán!"));
            }

            if ("ADMIN".equalsIgnoreCase(member.getRole()) || (group.getCreatedBy() != null && group.getCreatedBy().getId().equals(currentUser.getId()))) {
                GroupMember nextLeader = allMembers.stream()
                        .filter(m -> !m.getUser().getId().equals(currentUser.getId()))
                        .findFirst()
                        .orElse(null);
                if (nextLeader != null) {
                    nextLeader.setRole("ADMIN");
                    groupMemberRepository.save(nextLeader);
                    group.setCreatedBy(nextLeader.getUser());
                    appGroupRepository.save(group);
                }
            }

            groupMemberRepository.delete(member);
            return ResponseEntity.ok(Map.of("message", "Rời nhóm thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi khi rời nhóm: " + e.getMessage()));
        }
    }

    /**
     * 11. XOÁ NHÓM (DÀNH CHO TRƯỞNG NHÓM)
     */
    @DeleteMapping("/{groupId}")
    @Transactional
    public ResponseEntity<?> deleteGroup(@PathVariable Long groupId, Principal principal) {
        try {
            User currentUser = userRepository.findByUsername(principal.getName())
                    .or(() -> userRepository.findByEmail(principal.getName()))
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại!"));

            AppGroup group = appGroupRepository.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Nhóm không tồn tại!"));

            GroupMember member = groupMemberRepository.findById(new GroupMemberId(groupId, currentUser.getId()))
                    .orElseThrow(() -> new RuntimeException("Bạn không phải thành viên của nhóm này!"));

            boolean isLeader = "ADMIN".equalsIgnoreCase(member.getRole()) ||
                    (group.getCreatedBy() != null && group.getCreatedBy().getId().equals(currentUser.getId()));

            if (!isLeader) {
                return ResponseEntity.status(403).body(Map.of("message", "Chỉ có trưởng nhóm mới có quyền xoá nhóm!"));
            }

            deleteGroupCascade(group);
            return ResponseEntity.ok(Map.of("message", "Xoá nhóm thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi khi xoá nhóm: " + e.getMessage()));
        }
    }

    private void deleteGroupCascade(AppGroup group) {
        Long groupId = group.getId();
        transactionCommentRepository.deleteByGroupId(groupId);

        List<GroupTransaction> txList = groupTransactionRepository.findByGroupIdOrderByTransactionDateDesc(groupId);
        for (GroupTransaction tx : txList) {
            transactionCommentRepository.deleteByGroupTransactionId(tx.getId());
            groupTransactionSplitRepository.deleteByGroupTransactionId(tx.getId());
        }

        groupTransactionRepository.deleteByGroupId(groupId);
        groupMemberRepository.deleteByGroupId(groupId);
        appGroupRepository.delete(group);
    }
}
