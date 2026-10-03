package com.universidad.reservaslabs.controller;

import com.universidad.reservaslabs.exception.RecursoNoEncontradoException;
import com.universidad.reservaslabs.exception.ReservaConflictException;
import com.universidad.reservaslabs.model.Laboratorio;
import com.universidad.reservaslabs.repository.LaboratorioRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/laboratorios")
public class LaboratorioController {

    private final LaboratorioRepository laboratorioRepository;

    public LaboratorioController(LaboratorioRepository laboratorioRepository) {
        this.laboratorioRepository = laboratorioRepository;
    }

    @GetMapping
    public List<Laboratorio> listar() {
        return laboratorioRepository.findAll();
    }

    @GetMapping("/{id}")
    public Laboratorio obtenerPorId(@PathVariable Long id) {
        return laboratorioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Laboratorio no encontrado con ID: " + id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Laboratorio crear(@Valid @RequestBody Laboratorio laboratorio) {
        if (laboratorioRepository.existsByNombreIgnoreCase(laboratorio.getNombre())) {
            throw new ReservaConflictException("Ya existe un laboratorio con el nombre: " + laboratorio.getNombre());
        }
        return laboratorioRepository.save(laboratorio);
    }
}
