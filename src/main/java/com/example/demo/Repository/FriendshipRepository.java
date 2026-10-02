package com.example.demo.Repository;

import com.example.demo.model.Friendship;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    List<Friendship> findByUser(User user);

    Optional<Friendship> findByUserAndFriend(User user, User friend);

    boolean existsByUserAndFriend(User user, User friend);
}
