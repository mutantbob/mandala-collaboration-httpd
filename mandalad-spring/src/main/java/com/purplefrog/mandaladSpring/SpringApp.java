package com.purplefrog.mandaladSpring;

import com.purplefrog.mandalad.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.web.servlet.*;
import org.springframework.context.annotation.*;
import org.springframework.web.multipart.*;
import org.springframework.web.multipart.commons.*;
import org.springframework.web.multipart.support.*;

@SpringBootApplication
@EnableAutoConfiguration(exclude = {MultipartAutoConfiguration.class})
public class SpringApp
{
    public static void main(String[] argv)
    {
        SpringApplication.run(SpringApp.class);
    }

    // without this Spring isn't smart enough to parse the multipart forms used for file upload
    @Configuration
    public static class MyConfig {
        @Bean
        public MultipartResolver multipartResolver()
        {
            if (false) {
                return new StandardServletMultipartResolver();
            } else {
                return new CommonsMultipartResolver();
            }
        }
    }

    @Bean
    public StorageService storageService()
    {
        return new FileStorageService();
    }
}
