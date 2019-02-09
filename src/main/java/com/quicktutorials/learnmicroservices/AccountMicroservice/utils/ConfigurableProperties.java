package com.quicktutorials.learnmicroservices.AccountMicroservice.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

@Configuration
@PropertySource("classpath:config.properties")
public class ConfigurableProperties {

    @Autowired
    private Environment env;

    public String sayHelloWorld() {
        return env.getProperty("helloworld.text");
    }

}