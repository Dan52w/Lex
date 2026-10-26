package com.example.lexcloud.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * P0-B7: freno a fuerza bruta en memoria (sin dependencia nueva como Bucket4j).
 * Cuenta SOLO intentos fallidos, por IP + cuenta, en una ventana de 15 min:
 * a partir de 5 fallos, login responde 429 hasta que pase la ventana.
 * Un login exitoso limpia el contador. En despliegue con varias instancias
 * este contador es por instancia (limitación documentada en README-P0.md;
 * el paso a un store compartido es P1).
 */
@Service
public class LoginAttemptService {

    public static final int MAX_FALLOS = 5;
    public static final long VENTANA_SEGUNDOS = 15 * 60;

    private record Intento(int fallos, Instant ultimoFallo) {}

    private final Map<String, Intento> intentos = new ConcurrentHashMap<>();

    private String clave(String ip, String correoNormalizado) {
        return (ip == null ? "sin-ip" : ip) + "|" + correoNormalizado;
    }

    public boolean bloqueado(String ip, String correoNormalizado) {
        Intento intento = intentos.get(clave(ip, correoNormalizado));
        if (intento == null || intento.fallos() < MAX_FALLOS) {
            return false;
        }
        if (Instant.now().isAfter(intento.ultimoFallo().plusSeconds(VENTANA_SEGUNDOS))) {
            intentos.remove(clave(ip, correoNormalizado));
            return false;
        }
        return true;
    }

    public void registrarFallo(String ip, String correoNormalizado) {
        intentos.compute(clave(ip, correoNormalizado), (k, anterior) -> {
            if (anterior == null
                    || Instant.now().isAfter(anterior.ultimoFallo().plusSeconds(VENTANA_SEGUNDOS))) {
                return new Intento(1, Instant.now());
            }
            return new Intento(anterior.fallos() + 1, Instant.now());
        });
    }

    public void registrarExito(String ip, String correoNormalizado) {
        intentos.remove(clave(ip, correoNormalizado));
    }

    // P0-B7: registro de cuentas también limitado (10 intentos/hora por IP),
    // porque el alta de cuentas es una operación sensible aunque ya sea
    // solo-admin: frena el abuso automatizado desde una IP comprometida.
    private final Map<String, Intento> registros = new ConcurrentHashMap<>();
    public static final int MAX_REGISTROS_HORA = 10;

    public boolean registroBloqueado(String ip) {
        Intento intento = registros.get(ip == null ? "sin-ip" : ip);
        if (intento == null || intento.fallos() < MAX_REGISTROS_HORA) {
            return false;
        }
        if (Instant.now().isAfter(intento.ultimoFallo().plusSeconds(3600))) {
            registros.remove(ip == null ? "sin-ip" : ip);
            return false;
        }
        return true;
    }

    public void registrarIntentoRegistro(String ip) {
        String k = ip == null ? "sin-ip" : ip;
        registros.compute(k, (key, anterior) -> {
            if (anterior == null
                    || Instant.now().isAfter(anterior.ultimoFallo().plusSeconds(3600))) {
                return new Intento(1, Instant.now());
            }
            return new Intento(anterior.fallos() + 1, Instant.now());
        });
    }
}
