package com.universidad.reservaslabs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.universidad.reservaslabs.controller.LaboratorioController;
import com.universidad.reservaslabs.exception.GlobalRestExceptionHandler;
import com.universidad.reservaslabs.model.Laboratorio;
import com.universidad.reservaslabs.repository.LaboratorioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LaboratorioController.class)
@Import(GlobalRestExceptionHandler.class)
class LaboratorioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LaboratorioRepository laboratorioRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("GET /api/laboratorios -> 200 OK con listado de laboratorios")
    void debeListarLaboratorios() throws Exception {
        Laboratorio lab1 = Laboratorio.builder().id(1L).nombre("Lab 1").ubicacion("Edif 1").capacidad(20).tipo("Info").build();
        when(laboratorioRepository.findAll()).thenReturn(List.of(lab1));

        mockMvc.perform(get("/api/laboratorios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre", is("Lab 1")));
    }

    @Test
    @DisplayName("GET /api/laboratorios/1 -> 200 OK cuando existe")
    void debeObtenerLaboratorioPorId() throws Exception {
        Laboratorio lab1 = Laboratorio.builder().id(1L).nombre("Lab 1").ubicacion("Edif 1").capacidad(20).tipo("Info").build();
        when(laboratorioRepository.findById(1L)).thenReturn(Optional.of(lab1));

        mockMvc.perform(get("/api/laboratorios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.nombre", is("Lab 1")));
    }

    @Test
    @DisplayName("GET /api/laboratorios/999 -> 404 Not Found cuando no existe")
    void debeRetornar404CuandoNoExiste() throws Exception {
        when(laboratorioRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/laboratorios/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Laboratorio no encontrado con ID: 999")));
    }

    @Test
    @DisplayName("POST /api/laboratorios -> 201 Created al crear nuevo laboratorio")
    void debeCrearLaboratorio() throws Exception {
        Laboratorio lab = Laboratorio.builder().nombre("Lab Nuevo").ubicacion("Edif 2").capacidad(30).tipo("Redes").build();
        Laboratorio labGuardado = Laboratorio.builder().id(2L).nombre("Lab Nuevo").ubicacion("Edif 2").capacidad(30).tipo("Redes").build();

        when(laboratorioRepository.existsByNombreIgnoreCase("Lab Nuevo")).thenReturn(false);
        when(laboratorioRepository.save(any(Laboratorio.class))).thenReturn(labGuardado);

        mockMvc.perform(post("/api/laboratorios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lab)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(2)))
                .andExpect(jsonPath("$.nombre", is("Lab Nuevo")));
    }

    @Test
    @DisplayName("POST /api/laboratorios -> 409 Conflict si el nombre ya existe")
    void debeRetornar409SiNombreExiste() throws Exception {
        Laboratorio lab = Laboratorio.builder().nombre("Lab Existente").ubicacion("Edif 2").capacidad(30).tipo("Redes").build();

        when(laboratorioRepository.existsByNombreIgnoreCase("Lab Existente")).thenReturn(true);

        mockMvc.perform(post("/api/laboratorios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lab)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("Ya existe un laboratorio con el nombre: Lab Existente")));
    }
}
