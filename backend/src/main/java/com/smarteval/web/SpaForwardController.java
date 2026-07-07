package com.smarteval.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {
    @GetMapping({
            "/",
            "/admin",
            "/admin/{*path}",
            "/mobile",
            "/mobile/{*path}"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
