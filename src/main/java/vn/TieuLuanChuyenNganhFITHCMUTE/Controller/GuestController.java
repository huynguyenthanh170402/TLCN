package vn.TieuLuanChuyenNganhFITHCMUTE.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GuestController {

	@GetMapping({"/", "/home"})
	public String home(Model model) {
		model.addAttribute("page", "home");
		return "guest/home";
	}

	@GetMapping("/about")
	public String about(Model model) {
		model.addAttribute("page", "about");
		return "guest/about";
	}

	@GetMapping("/faculty")
	public String faculty(Model model) {
		model.addAttribute("page", "faculty");
		return "guest/faculty";
	}

	@GetMapping("/major")
	public String major(Model model) {
		model.addAttribute("page", "major");
		return "guest/major";
	}

	@GetMapping("/center")
	public String center(Model model) {
		model.addAttribute("page", "center");
		return "guest/center";
	}

	@GetMapping("/news")
	public String news(Model model) {
		model.addAttribute("page", "news");
		return "guest/news";
	}

	@GetMapping("/research")
	public String research(Model model) {
		model.addAttribute("page", "research");
		return "guest/research";
	}
}