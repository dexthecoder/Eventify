package com.example.demo.repository;

import com.example.demo.entity.Event;
import com.example.demo.util.ECategory;
import com.example.demo.util.EStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Integer> {

    boolean existsByTitleIgnoreCaseAndCategoryAndStatus(String title, ECategory category, EStatus status);

    // tüm eventler
    @Query("SELECT e FROM Event e WHERE e.status = :status AND " +
            "(:category IS NULL OR e.category = :category) AND " +
            "(LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(e.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Event> searchEvents(
            @Param("status") EStatus status,
            @Param("category") ECategory category,
            @Param("search") String search,
            Pageable pageable
    );

    // kullanıcının oluşturduğu eventler
    @Query("SELECT e FROM Event e WHERE e.creator.cid = :creatorId AND " +
            "(:category IS NULL OR e.category = :category) AND " +
            "(LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(e.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Event> searchUserEvents(
            @Param("creatorId") Integer creatorId,
            @Param("category") ECategory category,
            @Param("search") String search,
            Pageable pageable
    );


    // kullanıcının beğendiği eventler (JOIN ile likers üzerinden filtreleme yaparak, tüm liker'ları RAM'e çekmeden)
    @Query("SELECT e FROM Event e JOIN e.likers u WHERE u.cid = :userId AND " +
            "e.status = :status AND " +
            "(:category IS NULL OR e.category = :category) AND " +
            "(LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(e.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Event> searchLikedEvents(
            @Param("userId") Integer userId,
            @Param("status") EStatus status,
            @Param("category") ECategory category,
            @Param("search") String search,
            Pageable pageable
    );

    // Beğeni Sayacı (Tüm liker'ları RAM'e çekmez, sadece SQL'den sayıyı alır)
    @Query("SELECT COUNT(u) FROM Event e JOIN e.likers u WHERE e.id = :eventId")
    long countLikesOfEvent(@Param("eventId") Integer eventId);

    // katıldıklarım - status filtreli (null ise hepsi)
    @Query("SELECT e FROM Event e JOIN e.participations p WHERE p.user.cid = :userId AND " +
            "(:status IS NULL OR e.status = :status) AND " +
            "(:category IS NULL OR e.category = :category) AND " +
            "(LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(e.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Event> searchParticipatedEventsWithStatus(
            @Param("userId") Integer userId,
            @Param("status") EStatus status,
            @Param("category") ECategory category,
            @Param("search") String search,
            Pageable pageable
    );

    // Süresi geçmiş ve hâlâ aktif olan etkinlikleri bul
    @Query("SELECT e FROM Event e WHERE e.executionDate < :now AND " +
            "(e.status = com.example.demo.util.EStatus.PUBLISHED OR " +
            "e.status = com.example.demo.util.EStatus.UNPUBLISHED)")
    List<Event> findExpiredActiveEvents(@Param("now") LocalDateTime now);

}