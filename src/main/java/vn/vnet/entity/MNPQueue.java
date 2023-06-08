package vn.vnet.entity;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "MNP_QUEUE")
@AllArgsConstructor
@NoArgsConstructor
public class MNPQueue
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(name="MSISDN")
    private String msisdn;
    @Column(name="TELCO_CODE_DEST")
    private String telcoCodeDest;
    @Column(name="TELCO_CODE_ORIGIN")
    private String telcoCodeOrigin;
    @Column(name="VSYSDATE")
    private Timestamp vsysdate;
    @Column(name="FILE_NAME")
    private String fileName;

    public MNPQueue(String msisdn, String telcoCodeDest, String telcoCodeOrigin, Timestamp vsysdate, String fileName) {
        this.msisdn = msisdn;
        this.telcoCodeDest = telcoCodeDest;
        this.telcoCodeOrigin = telcoCodeOrigin;
        this.vsysdate = vsysdate;
        this.fileName = fileName;
    }
}
