package com.smartclinic.hms.admin.item;

import com.smartclinic.hms.item.ItemManagerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/item")
public class AdminItemController {

    private final ItemManagerRepository itemManagerRepository;

    @GetMapping("/list")
    public String list(Model model) {
        List<AdminItemDto> items = itemManagerRepository.findAllByOrderByNameAsc()
                .stream()
                .map(AdminItemDto::new)
                .collect(Collectors.toList());
        model.addAttribute("items", items);
        model.addAttribute("hasItems", !items.isEmpty());
        model.addAttribute("pageTitle", "물품 관리");
        return "admin/item-list";
    }

    @GetMapping("/form")
    public String form(Model model) {
        model.addAttribute("pageTitle", "물품 등록");
        return "admin/item-form";
    }
}
