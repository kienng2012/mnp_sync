package vn.vnet;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.comparator.NameFileComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.vnet.entity.MNPFileSync;
import vn.vnet.entity.MNPQueue;
import vn.vnet.repository.MNPFileSyncRepository;
import vn.vnet.repository.MNPQueueRepository;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Ref call api :https://www.baeldung.com/java-ftp-client
 * https://github.com/eugenp/tutorials/tree/master/core-java-modules/core-java-networking-2
 */
@Component
@Log4j2
public class ScheduleMonitorSMS {

    final SimpleDateFormat sdfFormatDayStr = new SimpleDateFormat("yyyyMMdd");
    final SimpleDateFormat SDF_FORMAT_DATETIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    final String EXTENSION_FTP_FILE = ".txt";
    @Value("${sms.smpp.user-id}")
    private String username;
    @Value("${sms.smpp.password}")
    private String password;
    @Value("${sms.smpp.host}")
    private String host;
    @Value("${sms.smpp.port}")
    private int port;

    @Value("${sms.smpp.srcServer}")
    private String srcServer;

    @Value("${sms.smpp.srcClient}")
    private String srcClient;

    @Value("${sms.smpp.srcClientBackup}")
    private String srcClientBackup;

    private FtpClient ftpClient;

    @Autowired
    private MNPQueueRepository mnpQueueRepository;
    @Autowired
    private MNPFileSyncRepository mnpFileSyncRepository;

    @Scheduled(initialDelayString = "${sms.async.initial-delay}", fixedDelayString = "${sms.async.time-monitor}")
//    @Scheduled(initialDelayString = "${sms.async.initial-delay}", fixedDelayString = "1000")
    public void scheduleDownloadFileFtp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date now = new Date();

        ftpClient = new FtpClient(host, port, username, password);
        try {
            ftpClient.open();
            Collection<String> files = ftpClient.listFiles(srcServer);
            //Get currentDate & yesterday :
            Calendar c = Calendar.getInstance();
            c.setTime(now);
            c.add(Calendar.DATE, -1);
            Date yesterday = c.getTime();
            c.add(Calendar.DATE, -1);
            Date beforeYesterday = c.getTime();
            String strCurrentDate = sdfFormatDayStr.format(now);
            String strYesterdayDate = sdfFormatDayStr.format(yesterday);
            String strBeforeYesterdayDate = sdfFormatDayStr.format(beforeYesterday);
            List<String> lstFilesToDownload = files.stream().filter(s -> s.endsWith(EXTENSION_FTP_FILE) && (s.contains(strCurrentDate) || s.contains(strYesterdayDate) || s.contains(strBeforeYesterdayDate))).collect(Collectors.toList());
            lstFilesToDownload.stream().forEach(s -> {
                try {
                    Path pathToSave = Paths.get(srcClient);
                    Files.createDirectories(pathToSave);
//                    ftpClient.downloadFile(s, Paths.get(srcClient + "\\" + s).toAbsolutePath().toString()); //TODO : OPEN CMT to DOWLOAD FILE
                    ftpClient.downloadFile(s, Paths.get(srcClient  + s).toAbsolutePath().toString()); //TODO : OPEN CMT to DOWLOAD FILE
                } catch (IOException e) {
                    log.error(" [ERROR]Cannot download file. Detail ={}", e);
//                    throw new RuntimeException(e);
                }
            });
            ftpClient.close();
            this.syncMnpToDB();
            //IMPORTANT: Sau khi dong bo du lieu vao bang MNP_QUEUE ==> goi thu tuc [SYNC_MNPSA_FROM_MNP_QUEUE] de dong bo vao bang MNPSA
            mnpQueueRepository.syncMNPSAFromMnpQueue();
        } catch (IOException e) {
            log.error(" [ERROR]Cannot login ftp. Detail ={}", e);
//            throw new RuntimeException(e);
        }
    }

    /**
     * Read all file from folder & add to db
     */
    private void syncMnpToDB() {
        File folder = new File(srcClient);
        File[] files = folder.listFiles();
        //IMPORTANT : sap xep file theo ten tu cu den moi
        Arrays.sort(files, NameFileComparator.NAME_COMPARATOR);
        for (File file : files) {
            try {
                //IMPORTANT: Kiem tra xem file da dong bo truoc do chua : bang MNP_FILE_SYNC
                boolean isSynced = mnpFileSyncRepository.existFileName(file.getName());
                //Chua dong bo ==> Dong bo ngay
                if (!isSynced) {
                    log.info("BEGIN_SYNC FILE_NAME ={}", file.getName());
                    List<MNPQueue> mnpQueues = new ArrayList<>();
                    if (file.isFile() && file.getName().endsWith(EXTENSION_FTP_FILE)) {
                        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
                            String line = bufferedReader.readLine();
                            while (line != null) {
                                //do something with line
                                line = bufferedReader.readLine();
                                if (line != null && line.trim().length() != 0) {
                                    if (line.contains(",")) {
                                        String[] arr = line.trim().split(",");
                                        if (arr.length >= 9) //Ban ghi hop le : 202306070950580400005790,MOBILE,PORT,84929867986,04,04,05,05,2023-06-07 10:03:04
                                        {
                                            MNPQueue queue = new MNPQueue(arr[3], this.getTelcoCodeFromMNP(arr[4]), this.getTelcoCodeFromMNP(arr[6]), parseFromStr(arr[8]), file.getName());
                                            mnpQueues.add(queue);
                                        }
                                    }
                                }
                            }
                        } catch (FileNotFoundException e) {
                            log.error("[ERROR] Error FileNotFoundException read file={}. Detail ={}", file.getName(), e);
//                    throw new RuntimeException(e);
                        } catch (IOException e) {
                            log.error("[ERROR] Error IOException read file={}. Detail ={}", file.getName(), e);
//                    throw new RuntimeException(e);
                        }
                    }
                    if (mnpQueues.size() > 0)
                        mnpQueueRepository.saveAll(mnpQueues);
                    //Luu lai file da xu ly de ko xu ly lan sau nua
                    MNPFileSync mnpFileSync = new MNPFileSync(new Timestamp(System.currentTimeMillis()), file.getName());
                    mnpFileSyncRepository.save(mnpFileSync);
                } else {
                    log.info("FILE_SYNC_BEFORE => DO NOT SYNC NOW, FILE_NAME ={}", file.getName());
                }

                //Sau khi doc file xong move vao thu muc backup
                Path directoryBackupFile = Paths.get(srcClientBackup);
                Files.createDirectories(directoryBackupFile);
                Path pathBackupFile = Paths.get(directoryBackupFile +File.separator + file.getName());
                //backup ra thu muc moi
                Files.copy(Paths.get(file.getAbsolutePath()), pathBackupFile, StandardCopyOption.REPLACE_EXISTING);
                //Xoa file da xu ly
                file.delete();
            } catch (Exception e) {
                log.error("[ERROR] Error read file={}. Detail ={}", file.getName(), e);
            }
        }
    }


    private String getTelcoCodeFromMNP(String code) {
        if (code == null || code.trim().length() == 0)
            return "unknown";
        String telcoCode;
        code = code.trim();
        switch (code) {
            case "04":
                telcoCode = "vtl";
                break;
            case "01":
                telcoCode = "vms";
                break;
            case "02":
                telcoCode = "vnp";
                break;
            case "05":
                telcoCode = "vnm";
                break;
            case "07":
                telcoCode = "bee";
                break;
            case "08":
                telcoCode = "ddt";
                break;
            case "09":
                telcoCode = "red";
                break;
            default:
                telcoCode = code;
                break;
        }
        return telcoCode;
    }

    private Timestamp parseFromStr(String strDate) {
        Date dateInput;
        try {
            dateInput = SDF_FORMAT_DATETIME.parse(strDate);
        } catch (ParseException e) {
            dateInput = new Date();
        }
        return new Timestamp(dateInput.getTime());
    }

    /*
    @Scheduled(initialDelayString = "${sms.async.initial-delay}", fixedDelayString = "${sms.async.time-monitor}")
//    @Scheduled(initialDelayString = "${sms.async.initial-delay}", fixedDelayString = "1000")
    public void monitorPingGWVnet() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date now = new Date();
        String strDate = sdf.format(now);
        String msg = Application.API_CHANNEL + strDate;
        log.debug("fixedDelayForApiVNet schedule:: " + strDate);
        Long beginTransTime = System.currentTimeMillis();
        String resultApi = null;
        checkTelnet("10.19.10.120", 9696);
        log.info("[fixedDelayForApiVNet] Take along: {} (ms) , Message={}", (System.currentTimeMillis() - beginTransTime), msg);
    }

    protected String sendTextMessage(String urlLink, String username, String password, String sourceAddr, String destAddr, String msg) throws Exception {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("username", username);
        parameters.put("password", password);
        parameters.put("request_id", String.valueOf(System.currentTimeMillis()));
        parameters.put("source_addr", sourceAddr);
        parameters.put("dest_addr", destAddr);
        parameters.put("telco_code", "");
        parameters.put("type", "0");
        parameters.put("message", msg);
        String params = ParameterStringBuilder.getParamsString(parameters);
        String fullUrl = urlLink + params;
//        System.out.println(fullUrl);
        URL url = new URL(fullUrl);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        if (con.getResponseCode() == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            log.debug(content);
            return content.toString();
        } else {
            log.error("[Exception_API_VNet] API return HTTP Code= {}", con.getResponseCode());
        }
        return null;
    }

    private void checkTelnet(String ip, int port) {
        Socket pingSocket = null;
        PrintWriter out = null;
        BufferedReader in = null;

        try {
            pingSocket = new Socket(ip, port);
            out = new PrintWriter(pingSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(pingSocket.getInputStream()));
        } catch (IOException e) {
            return;
        }

        out.println("ping");
        try {
            System.out.println(in.readLine());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        out.close();
        try {
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            pingSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    */
}
