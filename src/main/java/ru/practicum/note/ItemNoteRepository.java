package ru.practicum.note;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ItemNoteRepository extends JpaRepository<ItemNote, Long> {

    List<ItemNote> findAllByItemUrlContainingAndItemUserId(String itemUrl, Long userId);

    @Query("select itn from ItemNote itn\n" +
           "            join itn.item as i\n" +
           "            where i.user.id = ?1 and ?2 member of i.tags")
    List<ItemNote> findItemNotesByUsersTags(Long userId, String tag);

    List<ItemNote> findAllByItemUserId(Long userId, Pageable pageable);
}
