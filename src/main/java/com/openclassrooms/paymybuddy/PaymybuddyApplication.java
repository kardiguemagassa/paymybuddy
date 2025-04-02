package com.openclassrooms.paymybuddy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@SpringBootApplication
public class PaymybuddyApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymybuddyApplication.class, args);

//		LocalDateTime now = LocalDateTime.now();
//		System.out.println(now);
//
//		LocalDateTime localDateTime = LocalDateTime.now();
//		Date date = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
//		System.out.println(date);


	}

}
