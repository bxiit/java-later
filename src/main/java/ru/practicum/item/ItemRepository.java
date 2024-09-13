package ru.practicum.item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.practicum.user.User;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long>, QuerydslPredicateExecutor<Item> {

    List<Item> findByUserId(Long userId);

    void deleteItemByUserIdAndId(Long userId, Long itemId);

    @Query("""
            select it
            from Item it
            join it.user as u
            where u.lastName ilike concat(?1, "%")
            """)
    List<Item> findItemsByLastNamePrefix(String lastName);

    Optional<Item> findByUserAndResolvedUrl(User user, String resolvedUrl);
}