package com.universidad.reservaslabs.service;

import com.universidad.reservaslabs.exception.RecursoNoEncontradoException;
import com.universidad.reservaslabs.exception.ReservaConflictException;
import com.universidad.reservaslabs.model.EstadoReserva;
import com.universidad.reservaslabs.model.Laboratorio;
import com.universidad.reservaslabs.model.Reserva;
import com.universidad.reservaslabs.repository.LaboratorioRepository;
import com.universidad.reservaslabs.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional
public class ReservaService {

    public static final LocalTime APERTURA = LocalTime.of(7, 0);
    public static final LocalTime CIERRE = LocalTime.of(21, 0);
    public static final Duration DURACION_MINIMA = Duration.ofMinutes(30);
    public static final Duration DURACION_MAXIMA = Duration.ofHours(3);

    private final ReservaRepository reservaRepository;
    private final LaboratorioRepository laboratorioRepository;

    public ReservaService(ReservaRepository reservaRepository, LaboratorioRepository laboratorioRepository) {
        this.reservaRepository = reservaRepository;
        this.laboratorioRepository = laboratorioRepository;
    }

    /**
     * Valida reglas de negocio en memoria para los horarios y duración de una reserva,
     * sin interacción con la base de datos.
     */
    public void validarHorarioYDuracion(LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null || fin == null) {
            throw new ReservaConflictException("Las fechas de inicio y fin son obligatorias");
        }

        if (!fin.isAfter(inicio)) {
            throw new ReservaConflictException("La hora de fin debe ser posterior a la hora de inicio");
        }

        if (!inicio.toLocalDate().isEqual(fin.toLocalDate())) {
            throw new ReservaConflictException("La reserva debe realizarse dentro del mismo día");
        }

        Duration duracion = Duration.between(inicio, fin);
        if (duracion.compareTo(DURACION_MINIMA) < 0) {
            throw new ReservaConflictException("La duración mínima de una reserva es de 30 minutos");
        }
        if (duracion.compareTo(DURACION_MAXIMA) > 0) {
            throw new ReservaConflictException("La duración máxima de una reserva es de 3 horas");
        }

        LocalTime horaInicio = inicio.toLocalTime();
        LocalTime horaFin = fin.toLocalTime();

        if (horaInicio.isBefore(APERTURA) || horaFin.isAfter(CIERRE) || horaInicio.isAfter(CIERRE) || horaFin.isBefore(APERTURA)) {
            throw new ReservaConflictException("El horario de la reserva debe estar dentro del rango permitido (07:00 a 21:00)");
        }
    }

    /**
     * Crea una nueva reserva verificando la existencia del laboratorio,
     * las restricciones de horario/duración y la ausencia de solapamientos.
     */
    public Reserva crear(Reserva reserva) {
        if (reserva.getLaboratorio() == null || reserva.getLaboratorio().getId() == null) {
            throw new RecursoNoEncontradoException("Se debe especificar un laboratorio válido para la reserva");
        }

        Long labId = reserva.getLaboratorio().getId();
        Laboratorio lab = laboratorioRepository.findById(labId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Laboratorio no encontrado con ID: " + labId));

        reserva.setLaboratorio(lab);

        validarHorarioYDuracion(reserva.getInicio(), reserva.getFin());

        List<Reserva> solapamientos = reservaRepository.buscarSolapamientos(lab.getId(), reserva.getInicio(), reserva.getFin());
        if (!solapamientos.isEmpty()) {
            throw new ReservaConflictException("El laboratorio " + lab.getNombre() + " ya tiene una reserva en ese horario");
        }

        reserva.setEstado(EstadoReserva.CONFIRMADA);
        return reservaRepository.save(reserva);
    }

    /**
     * Cancela una reserva existente si aún no ha comenzado su horario de inicio.
     */
    public Reserva cancelar(Long id) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada con ID: " + id));

        if (reserva.getInicio().isBefore(LocalDateTime.now())) {
            throw new ReservaConflictException("No se puede cancelar una reserva cuyo horario de inicio ya pasó");
        }

        reserva.setEstado(EstadoReserva.CANCELADA);
        return reservaRepository.save(reserva);
    }

    @Transactional(readOnly = true)
    public List<Reserva> findAll() {
        return reservaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Reserva findById(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada con ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<Reserva> findByLaboratorio(Long laboratorioId) {
        if (!laboratorioRepository.existsById(laboratorioId)) {
            throw new RecursoNoEncontradoException("Laboratorio no encontrado con ID: " + laboratorioId);
        }
        return reservaRepository.findByLaboratorioId(laboratorioId);
    }
}
