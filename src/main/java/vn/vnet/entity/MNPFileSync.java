package vn.vnet.entity;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "MNP_FILE_SYNC")
@AllArgsConstructor
@NoArgsConstructor
public class MNPFileSync
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(name="SYNC_DATE")
    private Timestamp syncDate;
    @Column(name="FILE_NAME")
    private String fileName;

    public MNPFileSync(Timestamp syncDate, String fileName) {
        this.syncDate = syncDate;
        this.fileName = fileName;
    }
}
