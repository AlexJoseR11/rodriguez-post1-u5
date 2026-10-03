package com.universidad.reservaslabs;

import com.universidad.reservaslabs.exception.RecursoNoEncontradoException;
import com.universidad.reservaslabs.exception.ReservaConflictException;
import com.universidad.reservaslabs.model.EstadoReserva;
import com.universidad.reservaslabs.model.Laboratorio;
import com.universidad.reservaslabs.model.Reserva;
import com.universidad.reservaslabs.repository.LaboratorioRepository;
import com.universidad.reservaslabs.service.ReservaService;
import com.universidad.reservaslabs.web.ReservaWebController;
import com.universidad.reservaslabs.web.ReservaWebExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = ReservaWebController.class)
@Import(ReservaWebExceptionHandler.class)
class ReservaWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservaService reservaService;

    @MockBean
    private LaboratorioRepository laboratorioRepository;

    @Test
    @DisplayName("GET /reservas -> retorna vista reservas/lista con atributos en el modelo")
    void debeListarReservasEnVista() throws Exception {
        when(reservaService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/reservas"))
                .andExpect(status().isOk())
                .andExpect(view().name("reservas/lista"))
                .andExpect(model().attributeExists("reservas"));
    }

    @Test
    @DisplayName("GET /reservas/nueva -> retorna vista reservas/nueva con formulario y laboratorios")
    void debeMostrarFormularioNueva() throws Exception {
        Laboratorio lab = Laboratorio.builder().id(1L).nombre("Lab 1").ubicacion("Edificio A").capacidad(20).tipo("Info").build();
        when(laboratorioRepository.findAll()).thenReturn(List.of(lab));

        mockMvc.perform(get("/reservas/nueva"))
                .andExpect(status().isOk())
                .andExpect(view().name("reservas/nueva"))
                .andExpect(model().attributeExists("reserva"))
                .andExpect(model().attributeExists("laboratorios"));
    }

    @Test
    @DisplayName("POST /reservas -> redirecciona a /reservas con mensaje flash de éxito")
    void debeCrearReservaYRedirigirConMensaje() throws Exception {
        when(reservaService.crear(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/reservas")
                        .param("laboratorio.id", "1")
                        .param("nombreSolicitante", "Carlos")
                        .param("correoSolicitante", "carlos@uni.edu")
                        .param("inicio", "2026-10-10T09:00")
                        .param("fin", "2026-10-10T11:00")
                        .param("motivo", "Clase"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservas"))
                .andExpect(flash().attribute("mensaje", "¡Reserva creada exitosamente!"));
    }

    @Test
    @DisplayName("POST /reservas con conflicto -> ReservaWebExceptionHandler captura y redirige a /reservas/nueva con flash attribute error")
    void debeManejarConflictoRedirigiendoAFormulario() throws Exception {
        when(reservaService.crear(any(Reserva.class)))
                .thenThrow(new ReservaConflictException("El laboratorio ya tiene una reserva en ese horario"));

        mockMvc.perform(post("/reservas")
                        .param("laboratorio.id", "1")
                        .param("nombreSolicitante", "Carlos")
                        .param("correoSolicitante", "carlos@uni.edu")
                        .param("inicio", "2026-10-10T09:00")
                        .param("fin", "2026-10-10T11:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservas/nueva"))
                .andExpect(flash().attribute("error", "El laboratorio ya tiene una reserva en ese horario"));
    }

    @Test
    @DisplayName("POST /reservas/{id}/cancelar -> cancela y redirige a /reservas con mensaje flash")
    void debeCancelarReservaYRedirigir() throws Exception {
        Reserva reserva = Reserva.builder().id(5L).estado(EstadoReserva.CANCELADA).build();
        when(reservaService.cancelar(5L)).thenReturn(reserva);

        mockMvc.perform(post("/reservas/5/cancelar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservas"))
                .andExpect(flash().attribute("mensaje", "¡Reserva cancelada exitosamente!"));
    }

    @Test
    @DisplayName("POST /reservas/999/cancelar con RecursoNoEncontradoException -> ReservaWebExceptionHandler redirige a /reservas con flash error")
    void debeManejarRecursoNoEncontradoAlCancelar() throws Exception {
        when(reservaService.cancelar(999L))
                .thenThrow(new RecursoNoEncontradoException("Reserva no encontrada con ID: 999"));

        mockMvc.perform(post("/reservas/999/cancelar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservas"))
                .andExpect(flash().attribute("error", "Reserva no encontrada con ID: 999"));
    }
}
