package com.universidad.reservaslabs.web;

import com.universidad.reservaslabs.model.Reserva;
import com.universidad.reservaslabs.repository.LaboratorioRepository;
import com.universidad.reservaslabs.service.ReservaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reservas")
public class ReservaWebController {

    private final ReservaService reservaService;
    private final LaboratorioRepository laboratorioRepository;

    public ReservaWebController(ReservaService reservaService, LaboratorioRepository laboratorioRepository) {
        this.reservaService = reservaService;
        this.laboratorioRepository = laboratorioRepository;
    }

    @GetMapping
    public String listarReservas(Model model) {
        model.addAttribute("reservas", reservaService.findAll());
        return "reservas/lista";
    }

    @GetMapping("/nueva")
    public String mostrarFormularioNueva(Model model) {
        if (!model.containsAttribute("reserva")) {
            model.addAttribute("reserva", new Reserva());
        }
        model.addAttribute("laboratorios", laboratorioRepository.findAll());
        return "reservas/nueva";
    }

    @PostMapping
    public String crearReserva(@ModelAttribute("reserva") Reserva reserva, RedirectAttributes redirectAttributes) {
        reservaService.crear(reserva);
        redirectAttributes.addFlashAttribute("mensaje", "¡Reserva creada exitosamente!");
        return "redirect:/reservas";
    }

    @PostMapping("/{id}/cancelar")
    public String cancelarReserva(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        reservaService.cancelar(id);
        redirectAttributes.addFlashAttribute("mensaje", "¡Reserva cancelada exitosamente!");
        return "redirect:/reservas";
    }
}
