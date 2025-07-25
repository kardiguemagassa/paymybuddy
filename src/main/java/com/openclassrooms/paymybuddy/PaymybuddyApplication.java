package com.openclassrooms.paymybuddy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PaymybuddyApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymybuddyApplication.class, args);
	}
}
/**
 * relancer l'analise sonar à chaque modification de code
 *  mvn clean verify sonar:sonar -Dsonar.login=squ_0345639e68a1c439ccc59fe26bf8157fb9b7427a
 */
