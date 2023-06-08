package vn.vnet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import vn.vnet.entity.MNPQueue;

public interface MNPQueueRepository extends JpaRepository<MNPQueue,Long> {
    @Procedure("SYNC_MNPSA_FROM_MNP_QUEUE")
    void syncMNPSAFromMnpQueue();
}
