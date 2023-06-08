package vn.vnet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * https://juliuskrah.com/blog/2018/12/28/building-an-smpp-application-using-spring-boot/
 * https://github.com/juliuskrah/smpp/blob/master/README.md
 */
@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(ApplicationProperties.class)
public class Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);
    @Autowired
    private ApplicationProperties properties;

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(Application.class, args);
//		SmppSession session = ctx.getBean(SmppSession.class);
//		new Application().sendTextMessage(session, "VNet_Monitor", "Hello World", "84967891610");
    }






}
