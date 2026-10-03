package com.universidad.reservaslabs;

import com.universidad.reservaslabs.exception.RecursoNoEncontradoException;
import com.universidad.reservaslabs.exception.ReservaConflictException;
import com.universidad.reservaslabs.model.EstadoReserva;
import com.universidad.reservaslabs.model.Laboratorio;
import com.universidad.reservaslabs.model.Reserva;
import com.universidad.reservaslabs.repository.LaboratorioRepository;
import com.universidad.reservaslabs.repository.ReservaRepository;
import com.universidad.reservaslabs.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private LaboratorioRepository laboratorioRepository;

    @InjectMocks
    private ReservaService reservaService;

    private Laboratorio laboratorioPrueba;
    private LocalDate fechaPrueba;

    @BeforeEach
    void setUp() {
        laboratorioPrueba = Laboratorio.builder()
                .id(1L)
                .nombre("Lab de Computación")
                .ubicacion("Edificio A")
                .capacidad(30)
                .tipo("Informática")
                .build();
        fechaPrueba = LocalDate.now().plusDays(2);
    }

    @Nested
    @DisplayName("Pruebas de Creación de Reservas")
    class CreacionReservasTests {

        @Test
        @DisplayName("1. Debe crear exitosamente una reserva en un horario libre")
        void debeCrearReservaExitosamente() {
            LocalDateTime inicio = LocalDateTime.of(fechaPrueba, LocalTime.of(10, 0));
            LocalDateTime fin = LocalDateTime.of(fechaPrueba, LocalTime.of(12, 0));

            Reserva nuevaReserva = Reserva.builder()
                    .laboratorio(laboratorioPrueba)
                    .nombreSolicitante("Juan Rodríguez")
                    .correoSolicitante("juan.rodriguez@universidad.edu")
                    .inicio(inicio)
                    .fin(fin)
                    .motivo("Investigación")
                    .build();

            when(laboratorioRepository.findById(1L)).thenReturn(Optional.of(laboratorioPrueba));
            when(reservaRepository.buscarSolapamientos(eq(1L), eq(inicio), eq(fin))).thenReturn(Collections.emptyList());
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> {
                Reserva r = invocation.getArgument(0);
                r.setId(100L);
                return r;
            });

            Reserva creada = reservaService.crear(nuevaReserva);

            assertNotNull(creada);
            assertEquals(100L, creada.getId());
            assertEquals(EstadoReserva.CONFIRMADA, creada.getEstado());
            assertEquals(laboratorioPrueba, creada.getLaboratorio());
            verify(reservaRepository).save(nuevaReserva);
        }

        @Test
        @DisplayName("2. Debe lanzar ReservaConflictException cuando existe solapamiento de horario")
        void debeLanzarExcepcionPorSolapamiento() {
            LocalDateTime inicio = LocalDateTime.of(fechaPrueba, LocalTime.of(10, 0));
            LocalDateTime fin = LocalDateTime.of(fechaPrueba, LocalTime.of(12, 0));

            Reserva nuevaReserva = Reserva.builder()
                    .laboratorio(laboratorioPrueba)
                    .nombreSolicitante("María López")
                    .correoSolicitante("maria.lopez@universidad.edu")
                    .inicio(inicio)
                    .fin(fin)
                    .build();

            Reserva reservaExistente = Reserva.builder()
                    .id(50L)
                    .laboratorio(laboratorioPrueba)
                    .inicio(LocalDateTime.of(fechaPrueba, LocalTime.of(11, 0)))
                    .fin(LocalDateTime.of(fechaPrueba, LocalTime.of(13, 0)))
                    .estado(EstadoReserva.CONFIRMADA)
                    .build();

            when(laboratorioRepository.findById(1L)).thenReturn(Optional.of(laboratorioPrueba));
            when(reservaRepository.buscarSolapamientos(eq(1L), eq(inicio), eq(fin)))
                    .thenReturn(List.of(reservaExistente));

            ReservaConflictException ex = assertThrows(ReservaConflictException.class, () ->
                    reservaService.crear(nuevaReserva)
            );

            assertEquals("El laboratorio " + laboratorioPrueba.getNombre() + " ya tiene una reserva en ese horario", ex.getMessage());
            verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("3.1. Debe lanzar ReservaConflictException si la hora de fin es menor o igual a la de inicio")
        void debeLanzarExcepcionSiFinEsMenorOIgualAInicio() {
            LocalDateTime inicio = LocalDateTime.of(fechaPrueba, LocalTime.of(10, 0));
            LocalDateTime fin = LocalDateTime.of(fechaPrueba, LocalTime.of(9, 0));

            Reserva nuevaReserva = Reserva.builder()
                    .laboratorio(laboratorioPrueba)
                    .nombreSolicitante("Juan")
                    .correoSolicitante("juan@uni.edu")
                    .inicio(inicio)
                    .fin(fin)
                    .build();

            when(laboratorioRepository.findById(1L)).thenReturn(Optional.of(laboratorioPrueba));

            ReservaConflictException ex = assertThrows(ReservaConflictException.class, () ->
                    reservaService.crear(nuevaReserva)
            );

            assertEquals("La hora de fin debe ser posterior a la hora de inicio", ex.getMessage());
        }

        @Test
        @DisplayName("3.2. Debe lanzar ReservaConflictException si la duración es menor a 30 minutos")
        void debeLanzarExcepcionSiDuracionMenorA30Minutos() {
            LocalDateTime inicio = LocalDateTime.of(fechaPrueba, LocalTime.of(10, 0));
            LocalDateTime fin = LocalDateTime.of(fechaPrueba, LocalTime.of(10, 20));

            Reserva nuevaReserva = Reserva.builder()
                    .laboratorio(laboratorioPrueba)
                    .nombreSolicitante("Juan")
                    .correoSolicitante("juan@uni.edu")
                    .inicio(inicio)
                    .fin(fin)
                    .build();

            when(laboratorioRepository.findById(1L)).thenReturn(Optional.of(laboratorioPrueba));

            ReservaConflictException ex = assertThrows(ReservaConflictException.class, () ->
                    reservaService.crear(nuevaReserva)
            );

            assertEquals("La duración mínima de una reserva es de 30 minutos", ex.getMessage());
        }

        @Test
        @DisplayName("3.3. Debe lanzar ReservaConflictException si la duración es superior a 3 horas")
        void debeLanzarExcepcionSiDuracionMayorA3Horas() {
            LocalDateTime inicio = LocalDateTime.of(fechaPrueba, LocalTime.of(10, 0));
            LocalDateTime fin = LocalDateTime.of(fechaPrueba, LocalTime.of(13, 30));

            Reserva nuevaReserva = Reserva.builder()
                    .laboratorio(laboratorioPrueba)
                    .nombreSolicitante("Juan")
                    .correoSolicitante("juan@uni.edu")
                    .inicio(inicio)
                    .fin(fin)
                    .build();

            when(laboratorioRepository.findById(1L)).thenReturn(Optional.of(laboratorioPrueba));

            ReservaConflictException ex = assertThrows(ReservaConflictException.class, () ->
                    reservaService.crear(nuevaReserva)
            );

            assertEquals("La duración máxima de una reserva es de 3 horas", ex.getMessage());
        }

        @Test
        @DisplayName("3.4. Debe lanzar ReservaConflictException si está fuera del rango 07:00 a 21:00")
        void debeLanzarExcepcionSiHorarioFueraDeRango() {
            // Reserva antes de las 07:00
            LocalDateTime inicioTemprano = LocalDateTime.of(fechaPrueba, LocalTime.of(6, 30));
            LocalDateTime finTemprano = LocalDateTime.of(fechaPrueba, LocalTime.of(8, 0));

            Reserva reservaTemprana = Reserva.builder()
                    .laboratorio(laboratorioPrueba)
                    .nombreSolicitante("Juan")
                    .correoSolicitante("juan@uni.edu")
                    .inicio(inicioTemprano)
                    .fin(finTemprano)
                    .build();

            when(laboratorioRepository.findById(1L)).thenReturn(Optional.of(laboratorioPrueba));

            assertThrows(ReservaConflictException.class, () ->
                    reservaService.crear(reservaTemprana)
            );

            // Reserva después de las 21:00
            LocalDateTime inicioTardio = LocalDateTime.of(fechaPrueba, LocalTime.of(20, 30));
            LocalDateTime finTardio = LocalDateTime.of(fechaPrueba, LocalTime.of(21, 30));

            Reserva reservaTardia = Reserva.builder()
                    .laboratorio(laboratorioPrueba)
                    .nombreSolicitante("Juan")
                    .correoSolicitante("juan@uni.edu")
                    .inicio(inicioTardio)
                    .fin(finTardio)
                    .build();

            assertThrows(ReservaConflictException.class, () ->
                    reservaService.crear(reservaTardia)
            );
        }

        @Test
        @DisplayName("3.5. Debe lanzar RecursoNoEncontradoException si el laboratorio no existe")
        void debeLanzarExcepcionSiLaboratorioNoExiste() {
            LocalDateTime inicio = LocalDateTime.of(fechaPrueba, LocalTime.of(10, 0));
            LocalDateTime fin = LocalDateTime.of(fechaPrueba, LocalTime.of(12, 0));

            Reserva nuevaReserva = Reserva.builder()
                    .laboratorio(Laboratorio.builder().id(999L).build())
                    .nombreSolicitante("Juan")
                    .correoSolicitante("juan@uni.edu")
                    .inicio(inicio)
                    .fin(fin)
                    .build();

            when(laboratorioRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(RecursoNoEncontradoException.class, () ->
                    reservaService.crear(nuevaReserva)
            );
        }
    }

    @Nested
    @DisplayName("Pruebas de Cancelación de Reservas")
    class CancelacionReservasTests {

        @Test
        @DisplayName("4. Debe lanzar ReservaConflictException al intentar cancelar una reserva cuyo horario ya pasó")
        void debeLanzarExcepcionAlCancelarReservaPasada() {
            LocalDateTime inicioPasado = LocalDateTime.now().minusHours(2);
            LocalDateTime finPasado = LocalDateTime.now().minusHours(1);

            Reserva reservaPasada = Reserva.builder()
                    .id(10L)
                    .laboratorio(laboratorioPrueba)
                    .nombreSolicitante("Pedro")
                    .correoSolicitante("pedro@uni.edu")
                    .inicio(inicioPasado)
                    .fin(finPasado)
                    .estado(EstadoReserva.CONFIRMADA)
                    .build();

            when(reservaRepository.findById(10L)).thenReturn(Optional.of(reservaPasada));

            ReservaConflictException ex = assertThrows(ReservaConflictException.class, () ->
                    reservaService.cancelar(10L)
            );

            assertEquals("No se puede cancelar una reserva cuyo horario de inicio ya pasó", ex.getMessage());
            verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("5. Debe cancelar exitosamente una reserva futura")
        void debeCancelarReservaFuturaExitosamente() {
            LocalDateTime inicioFuturo = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
            LocalDateTime finFuturo = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0);

            Reserva reservaFutura = Reserva.builder()
                    .id(20L)
                    .laboratorio(laboratorioPrueba)
                    .nombreSolicitante("Laura")
                    .correoSolicitante("laura@uni.edu")
                    .inicio(inicioFuturo)
                    .fin(finFuturo)
                    .estado(EstadoReserva.CONFIRMADA)
                    .build();

            when(reservaRepository.findById(20L)).thenReturn(Optional.of(reservaFutura));
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Reserva cancelada = reservaService.cancelar(20L);

            assertNotNull(cancelada);
            assertEquals(EstadoReserva.CANCELADA, cancelada.getEstado());
            verify(reservaRepository).save(reservaFutura);
        }
    }
}
