package com.universidad.reservaslabs;

import com.universidad.reservaslabs.model.EstadoReserva;
import com.universidad.reservaslabs.model.Laboratorio;
import com.universidad.reservaslabs.model.Reserva;
import com.universidad.reservaslabs.repository.LaboratorioRepository;
import com.universidad.reservaslabs.repository.ReservaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private final LaboratorioRepository laboratorioRepository;
    private final ReservaRepository reservaRepository;

    public DataInitializer(LaboratorioRepository laboratorioRepository, ReservaRepository reservaRepository) {
        this.laboratorioRepository = laboratorioRepository;
        this.reservaRepository = reservaRepository;
    }

    @Override
    public void run(String... args) {
        if (laboratorioRepository.count() == 0) {
            Laboratorio lab1 = laboratorioRepository.save(Laboratorio.builder()
                    .nombre("Laboratorio de Computación e IA")
                    .ubicacion("Edificio A - Aula 101")
                    .capacidad(30)
                    .tipo("Informática")
                    .build());

            Laboratorio lab2 = laboratorioRepository.save(Laboratorio.builder()
                    .nombre("Laboratorio de Redes y Telecomunicaciones")
                    .ubicacion("Edificio B - Aula 204")
                    .capacidad(25)
                    .tipo("Telecomunicaciones")
                    .build());

            Laboratorio lab3 = laboratorioRepository.save(Laboratorio.builder()
                    .nombre("Laboratorio de Robótica y Circuitos")
                    .ubicacion("Edificio C - Aula 305")
                    .capacidad(20)
                    .tipo("Electrónica y Robótica")
                    .build());

            if (reservaRepository.count() == 0) {
                // Reserva de prueba para mañana de 09:00 a 11:00 en Lab 1 para validar solapamientos
                LocalDate manana = LocalDate.now().plusDays(1);
                reservaRepository.save(Reserva.builder()
                        .laboratorio(lab1)
                        .nombreSolicitante("Prof. Carlos Rodríguez")
                        .correoSolicitante("carlos.rodriguez@universidad.edu")
                        .inicio(LocalDateTime.of(manana, LocalTime.of(9, 0)))
                        .fin(LocalDateTime.of(manana, LocalTime.of(11, 0)))
                        .motivo("Práctica de Programación Concurrente y Distribuida")
                        .estado(EstadoReserva.CONFIRMADA)
                        .build());
            }
        }
    }
}
