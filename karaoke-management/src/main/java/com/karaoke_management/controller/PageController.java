package com.karaoke_management.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping({"/", "/dashboard"})
    public String dashboard() {
        return "dashboard";
    }



    // @GetMapping("/invoice")
    // public String invoice() {
    //     return "invoice/invoice-list";
    // }
}
