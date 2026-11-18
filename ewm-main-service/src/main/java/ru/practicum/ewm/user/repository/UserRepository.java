package ru.practicum.ewm.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.user.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    @Query(value = "SELECT u.id as id, u.name as name, u.email as email " +
            "FROM users u " +
            "ORDER BY u.id " +
            "LIMIT :size OFFSET :offset", nativeQuery = true)
    List<UserProjection> findAll(@Param("offset") int offset,
                                 @Param("size") int size);

}
