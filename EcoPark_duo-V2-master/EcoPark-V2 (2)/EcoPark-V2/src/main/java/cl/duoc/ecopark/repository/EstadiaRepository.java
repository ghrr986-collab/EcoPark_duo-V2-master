package cl.duoc.ecopark.repository;

import cl.duoc.ecopark.dao.EstadiaDAO;
import cl.duoc.ecopark.model.Estadia;
import cl.duoc.ecopark.model.ResumenTipo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * REPOSITORY: mantiene las estadías en memoria y delega al DAO.
 * La interfaz EstadiaDAO permite cambiar JSON por MySQL sin cambiar los modelos.
 */
public class EstadiaRepository {

    private final EstadiaDAO dao;
    private final List<Estadia> estadias = new ArrayList<>();

    public EstadiaRepository(EstadiaDAO dao) {
        this.dao = dao;
    }

    /** Carga los registros desde MySQL, reemplazando la lista SOLO si no hubo error. */
    public void cargar() throws IOException {
        List<Estadia> recuperadas = dao.cargar();
        estadias.clear();
        estadias.addAll(recuperadas);
    }

    /** Devuelve una copia: el Controller no puede alterar la lista interna. */
    public List<Estadia> listar() {
        return List.copyOf(estadias);
    }

    /**
     * REGISTRAR: valida patente duplicada y solicita INSERT al DAO.
     * Primero confirmamos MySQL y DESPUÉS cambiamos la lista en memoria.
     */
    public void registrar(Estadia estadia) throws IOException {
        if (estadia == null) {
            throw new IllegalArgumentException("Debe indicar una estadía válida.");
        }
        for (Estadia existente : estadias) {
            if (existente.getPatente().equalsIgnoreCase(estadia.getPatente())) {
                throw new IllegalArgumentException("La patente " + estadia.getPatente()
                        + " ya está registrada.");
            }
        }
        dao.registrar(estadia);
        estadias.add(estadia);
    }

    /**
     * ELIMINAR: solicita DELETE y luego actualiza la lista.
     * Si MySQL falla, la tabla visual no perderá el registro.
     */
    public void eliminar(Estadia seleccionada) throws IOException {
        if (seleccionada == null) {
            throw new IllegalArgumentException("Seleccione una estadía para eliminar.");
        }
        if (!estadias.contains(seleccionada)) {
            throw new IllegalArgumentException("La estadía ya no existe en la lista.");
        }
        dao.eliminar(seleccionada.getId());
        estadias.removeIf(e -> e.getId() == seleccionada.getId()); //
    }

    /** ACTUALIZAR HORAS: solicita UPDATE y luego sincroniza la copia en memoria. */
    public void actualizarHoras(Estadia seleccionada, int horas) throws IOException {
        if (seleccionada == null || seleccionada.getId() <= 0) {
            throw new IllegalArgumentException("Seleccione una estadia para actualizar.");
        }
        if (horas < 1 || horas > 24) {
            throw new IllegalArgumentException("La hora no es valida.");
        }
        dao.actualizarHoras(seleccionada.getId(), horas);

        boolean encontrada = false;
        for (Estadia actual : estadias) {
            if (actual.getId() == seleccionada.getId()) {
                actual.setHoras(horas);
                encontrada = true;
                break;
            }
        }
        if (!encontrada) {
            cargar();
        }
    }

    /** BUSCAR: busca una estadía por patente en la copia en memoria. */
    public Optional<Estadia> buscarPorPatente(String patente) throws IOException {
        if (patente == null || patente.isBlank()) {
            throw new IllegalArgumentException("Debe indicar una patente.");
        }
        String criterio = patente.trim().toUpperCase(Locale.ROOT);
        for (Estadia e : estadias) {
            if (e.getPatente().equals(criterio)) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    /** RESUMEN: delega en el DAO el conteo y suma de horas por tipo. */
    public List<ResumenTipo> resumenPorTipo(int cantidadMinima) throws IOException {
        return dao.resumenPorTipo(cantidadMinima);
    }
}