package com.dulithadabare.dosomething;

import com.dulithadabare.dosomething.resource.FirebaseCloudMessaging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class DosomethingApplication {

	public static void main(String[] args) {
		SpringApplication.run(DosomethingApplication.class, args);
	}

	@PostConstruct
	private void postConstruct()
	{
		FirebaseCloudMessaging.init();
	}

}
