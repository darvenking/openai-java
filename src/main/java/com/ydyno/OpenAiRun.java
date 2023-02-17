package com.ydyno;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;

@SpringBootApplication
public class OpenAiRun {

    public static void main(String[] args) {
        SpringApplication.run(OpenAiRun.class, args);
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
