package jotes.ui;

import java.awt.Color;

/**
 * Paleta de cores do tema: tipo-valor imutável com todas as cores de que a UI precisa.
 * Existem duas instâncias pré-definidas: {@link #DARK} (o tema original da aplicação,
 * estilo Apple Notes dark) e {@link #LIGHT} (estilo Apple Notes claro: fundos quase
 * brancos, texto escuro e o mesmo acento amarelo-dourado).
 * <p>A paleta ativa está em {@link Theme#current()} e troca-se com
 * {@link Theme#setDark(boolean)}.</p>
 */
public record ThemeColors(
        Color bg,
        Color sidebarBg,
        Color listBg,
        Color cardBg,
        Color cardHover,
        Color selection,
        Color fieldBg,
        Color separator,
        Color text,
        Color textDim,
        Color accent,
        Color accentHover,
        Color accentPressed,
        Color accentBg,
        Color danger,
        Color textSelection,
        Color scrollThumb,
        Color scrollThumbHover,
        Color onAccent) {

    /** Paleta escura original (estilo Apple Notes / macOS dark). */
    public static final ThemeColors DARK = new ThemeColors(
            new Color(0x1E, 0x1E, 0x1E),        // bg
            new Color(0x26, 0x26, 0x26),        // sidebarBg
            new Color(0x1F, 0x1F, 0x1F),        // listBg
            new Color(0x2B, 0x2B, 0x2D),        // cardBg
            new Color(0x38, 0x38, 0x3A),        // cardHover
            new Color(0x3D, 0x3D, 0x40),        // selection
            new Color(0x2B, 0x2B, 0x2D),        // fieldBg (= cardBg)
            new Color(0x3A, 0x3A, 0x3C),        // separator
            new Color(0xEC, 0xEC, 0xF1),        // text
            new Color(0x9A, 0x9A, 0xA0),        // textDim
            new Color(0xFF, 0xD6, 0x0A),        // accent
            new Color(0xE6, 0xC1, 0x09),        // accentHover
            new Color(0xC9, 0xA8, 0x08),        // accentPressed
            new Color(0xFF, 0xD6, 0x0A, 40),    // accentBg
            new Color(0xFF, 0x45, 0x3A),        // danger
            new Color(0x0A, 0x84, 0xFF),        // textSelection
            new Color(0x55, 0x55, 0x59),        // scrollThumb
            new Color(0x6E, 0x6E, 0x73),        // scrollThumbHover
            new Color(0x1E, 0x1E, 0x1E));       // onAccent (texto sobre o acento)

    /** Paleta clara (estilo Apple Notes light): fundos quase brancos, texto escuro. */
    public static final ThemeColors LIGHT = new ThemeColors(
            new Color(0xFF, 0xFF, 0xFF),        // bg
            new Color(0xF2, 0xF2, 0xF7),        // sidebarBg
            new Color(0xFF, 0xFF, 0xFF),        // listBg
            new Color(0xF5, 0xF5, 0xF7),        // cardBg
            new Color(0xEC, 0xEC, 0xF0),        // cardHover
            new Color(0xE5, 0xE5, 0xEA),        // selection
            new Color(0xF5, 0xF5, 0xF7),        // fieldBg (= cardBg)
            new Color(0xD1, 0xD1, 0xD6),        // separator
            new Color(0x1C, 0x1C, 0x1E),        // text
            new Color(0x6C, 0x6C, 0x70),        // textDim
            new Color(0xFF, 0xD6, 0x0A),        // accent (mesmo tom nas duas paletas)
            new Color(0xE6, 0xC1, 0x09),        // accentHover
            new Color(0xC9, 0xA8, 0x08),        // accentPressed
            new Color(0xFF, 0xD6, 0x0A, 40),    // accentBg
            new Color(0xFF, 0x3B, 0x30),        // danger
            new Color(0x00, 0x7A, 0xFF),        // textSelection
            new Color(0xC1, 0xC1, 0xC6),        // scrollThumb
            new Color(0xA6, 0xA6, 0xAD),        // scrollThumbHover
            new Color(0x1E, 0x1E, 0x1E));       // onAccent (texto sobre o acento)
}
