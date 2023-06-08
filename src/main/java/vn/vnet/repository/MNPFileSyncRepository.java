package vn.vnet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.vnet.entity.MNPFileSync;

public interface MNPFileSyncRepository extends JpaRepository<MNPFileSync, Long> {
    @Query("select case when count(c)> 0 then true else false end from MNPFileSync c where c.fileName =:fileName")
    boolean existFileName(@Param("fileName") String model);

}
