package com.example.demo.repository;

import com.example.demo.entity.Participation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipationRepository extends JpaRepository<Participation, Integer> {

    // Bir etkinliğe katılan tüm kullanıcıları bulmak için
    List<Participation> findByEvent_Id(Integer eventId);

    // Bir kullanıcının katıldığı tüm etkinlikleri bulmak için
    List<Participation> findByUser_Cid(Integer userCid);

    // Bir kullanıcının aynı etkinliğe ikinci kez katılmasını engellemek için kontrol
    Optional<Participation> findByUser_CidAndEvent_Id(Integer userCid, Integer eventId);
}