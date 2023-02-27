package com.ydyno;

import com.ydyno.utils.SpringContextHolder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;

@SpringBootApplication
public class OpenAiRun {

    public static void main(String[] args) {
        SpringApplication.run(OpenAiRun.class, args);
    }

    @Bean
    public SpringContextHolder springContextHolder() {
        return new SpringContextHolder();
    }

    /**
     * 跳转到首页
     * @return /
     */
    @GetMapping("/")
    public String redirectIndex() {
        return "redirect:/index";
    }
}
