package com.smartclinic.hms.home;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Controller
@Entity
@Table(name = "home")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

}
