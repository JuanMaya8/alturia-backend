package co.edu.ucc.alturia.impresion;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

/** Comandos ESC/POS básicos para impresoras térmicas de 80 mm (DigitalPOS y compatibles). */
public final class EscPos {
    private static final Charset CP1252 = Charset.forName("windows-1252");
    private EscPos() {}

    public static byte[] documento(String texto) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[]{0x1B, 0x40});        // ESC @  inicializar
        out.writeBytes(new byte[]{0x1B, 0x74, 16});    // ESC t 16  página de códigos WPC1252 (tildes y ñ)
        for (String linea : texto.split("\n")) {
            boolean destacada = linea.startsWith("!!") || linea.startsWith("==");
            if (destacada) out.writeBytes(new byte[]{0x1B, 0x45, 1, 0x1D, 0x21, 0x11}); // negrita + doble tamaño
            out.writeBytes((linea + "\n").getBytes(CP1252));
            if (destacada) out.writeBytes(new byte[]{0x1B, 0x45, 0, 0x1D, 0x21, 0x00});
        }
        out.writeBytes("\n\n\n\n".getBytes(CP1252));
        out.writeBytes(new byte[]{0x1D, 0x56, 0x01});  // GS V 1  corte parcial
        return out.toByteArray();
    }
}
