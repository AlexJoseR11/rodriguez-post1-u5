package com.universidad.reservaslabs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.universidad.reservaslabs.model.EstadoReserva;
import com.universidad.reservaslabs.model.Laboratorio;
import com.universidad.reservaslabs.model.Reserva;
import com.universidad.reservaslabs.repository.LaboratorioRepository;
import com.universidad.reservaslabs.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReservaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LaboratorioRepository laboratorioRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private Laboratorio laboratorio;
    private LocalDate fechaTest;

    @BeforeEach
    void setUp() {
        reservaRepository.deleteAll();
        laboratorioRepository.deleteAll();

        laboratorio = laboratorioRepository.save(Laboratorio.builder()
                .nombre("Laboratorio de Software y Pruebas")
                .ubicacion("Edificio D - Aula 102")
                .capacidad(25)
                .tipo("Informática")
                .build());

        fechaTest = LocalDate.now().plusDays(3);
    }

    @Test
    @DisplayName("POST /api/reservas -> 201 Created al enviar una reserva válida")
    void debeCrearReservaRetornando201() throws Exception {
        LocalDateTime inicio = LocalDateTime.of(fechaTest, LocalTime.of(8, 0));
        LocalDateTime fin = LocalDateTime.of(fechaTest, LocalTime.of(10, 0));

        Reserva reserva = Reserva.builder()
                .laboratorio(Laboratorio.builder().id(laboratorio.getId()).build())
                .nombreSolicitante("Ing. Andrea Gómez")
                .correoSolicitante("andrea.gomez@universidad.edu")
                .inicio(inicio)
                .fin(fin)
                .motivo("Taller de Arquitectura Web")
                .build();

        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reserva)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nombreSolicitante", is("Ing. Andrea Gómez")))
                .andExpect(jsonPath("$.correoSolicitante", is("andrea.gomez@universidad.edu")))
                .andExpect(jsonPath("$.estado", is("CONFIRMADA")));
    }

    @Test
    @DisplayName("POST /api/reservas -> 409 Conflict al enviar una reserva que se solapa con una existente")
    void debeRetornar409PorSolapamiento() throws Exception {
        LocalDateTime inicioExistente = LocalDateTime.of(fechaTest, LocalTime.of(10, 0));
        LocalDateTime finExistente = LocalDateTime.of(fechaTest, LocalTime.of(12, 0));

        // Reserva existente previa
        reservaRepository.save(Reserva.builder()
                .laboratorio(laboratorio)
                .nombreSolicitante("Prof. Mario Casas")
                .correoSolicitante("mario@universidad.edu")
                .inicio(inicioExistente)
                .fin(finExistente)
                .motivo("Clase existente")
                .estado(EstadoReserva.CONFIRMADA)
                .build());

        // Nueva reserva solapada (11:00 a 13:00)
        LocalDateTime inicioSolapado = LocalDateTime.of(fechaTest, LocalTime.of(11, 0));
        LocalDateTime finSolapado = LocalDateTime.of(fechaTest, LocalTime.of(13, 0));

        Reserva reservaConflictiva = Reserva.builder()
                .laboratorio(Laboratorio.builder().id(laboratorio.getId()).build())
                .nombreSolicitante("Prof. Carla Díaz")
                .correoSolicitante("carla@universidad.edu")
                .inicio(inicioSolapado)
                .fin(finSolapado)
                .motivo("Taller paralelo")
                .build();

        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reservaConflictiva)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("El laboratorio " + laboratorio.getNombre() + " ya tiene una reserva en ese horario")));
    }

    @Test
    @DisplayName("GET /api/reservas -> 200 OK con listado de reservas")
    void debeListarReservas() throws Exception {
        reservaRepository.save(Reserva.builder()
                .laboratorio(laboratorio)
                .nombreSolicitante("Prof. Lucia")
                .correoSolicitante("lucia@uni.edu")
                .inicio(LocalDateTime.of(fechaTest, LocalTime.of(8, 0)))
                .fin(LocalDateTime.of(fechaTest, LocalTime.of(9, 30)))
                .motivo("Reunión")
                .estado(EstadoReserva.CONFIRMADA)
                .build());

        mockMvc.perform(get("/api/reservas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombreSolicitante", is("Prof. Lucia")));
    }

    @Test
    @DisplayName("DELETE /api/reservas/{id} -> 204 No Content al cancelar reserva futura")
    void debeCancelarReservaRetornando204() throws Exception {
        Reserva reserva = reservaRepository.save(Reserva.builder()
                .laboratorio(laboratorio)
                .nombreSolicitante("Prof. Roberto")
                .correoSolicitante("roberto@uni.edu")
                .inicio(LocalDateTime.of(fechaTest, LocalTime.of(14, 0)))
                .fin(LocalDateTime.of(fechaTest, LocalTime.of(16, 0)))
                .motivo("Seminario")
                .estado(EstadoReserva.CONFIRMADA)
                .build());

        mockMvc.perform(delete("/api/reservas/" + reserva.getId()))
                .andExpect(status().isNoContent());

        Reserva cancelada = reservaRepository.findById(reserva.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(EstadoReserva.CANCELADA, cancelada.getEstado());
    }

    @Test
    @DisplayName("GET /api/reservas/999 -> 404 Not Found cuando no existe la reserva")
    void debeRetornar404SiNoExisteReserva() throws Exception {
        mockMvc.perform(get("/api/reservas/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Reserva no encontrada con ID: 999")));
    }
}
