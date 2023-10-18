package possg.com.a.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import possg.com.a.service.ConvenienceService;

@SpringBootApplication
@EnableScheduling
public class DeleteToken {

	 @Autowired
	 private ConvenienceService service;

	    public static void main(String[] args) {
	        SpringApplication.run(DeleteToken.class, args);
	    }

	    @Scheduled(cron = "0 0 12 * * *")
	    public void dailyDatabaseCleanupTask() {
	    	service.autoTokenClean();
	    }
}
