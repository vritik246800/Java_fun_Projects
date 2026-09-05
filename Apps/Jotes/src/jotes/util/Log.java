package jotes.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Ponto único de logging. Configura o Logback para escrever em
 * {@code <pasta de dados>/logs/jotes.log} (com rotação diária) antes de qualquer
 * logger ser criado, e substitui os {@code printStackTrace} espalhados pelo código.
 * <p>{@link #init(Path)} tem de ser chamado no arranque, antes do primeiro
 * {@link #of(Class)}; sem isso o Logback só escreve na consola.</p>
 */
public final class Log {

    /** Propriedade lida pelo {@code logback.xml} para saber onde escrever. */
    private static final String DIR_PROPERTY = "jotes.log.dir";

    private Log() {}

    /**
     * Aponta os logs para {@code dataDir/logs}. Chamar uma vez, o mais cedo possível
     * (antes de qualquer classe pedir um logger).
     */
    public static void init(Path dataDir) {
        System.setProperty(DIR_PROPERTY, dataDir.resolve("logs").toAbsolutePath().toString());
    }

    public static Logger of(Class<?> type) {
        return LoggerFactory.getLogger(type);
    }

    /**
     * Regista uma exceção que não interrompe a aplicação — o substituto direto de
     * {@code ex.printStackTrace()}.
     */
    public static void warn(Class<?> type, String message, Throwable error) {
        of(type).warn(message, error);
    }

    public static void error(Class<?> type, String message, Throwable error) {
        of(type).error(message, error);
    }

    /** Instala um handler global para exceções não apanhadas (incluindo a thread do Swing). */
    public static void installGlobalHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) ->
                of(Log.class).error("Exceção não tratada em " + thread.getName(), error));
        // o EDT usa esta propriedade em vez do handler por omissão
        System.setProperty("sun.awt.exception.handler", AwtHandler.class.getName());
    }

    /** Handler das exceções da thread do Swing (instanciado por nome pelo AWT). */
    public static final class AwtHandler {
        @SuppressWarnings("unused")
        public void handle(Throwable error) {
            of(AwtHandler.class).error("Exceção na thread do Swing", error);
        }
    }
}
