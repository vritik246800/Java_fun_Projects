package jotes.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON mínimo e autossuficiente (escrita + leitura), sem dependências externas.
 * Suporta {@code Map<String,Object>}, {@code List}, {@code String}, {@code Number},
 * {@code Boolean} e {@code null}. Na leitura, números inteiros vêm como {@link Long}
 * e números com fração/expoente como {@link Double}.
 */
public final class Json {
    private Json() {}

    // ---------- escrita ----------

    /** Serializa um valor para texto JSON (compacto). */
    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(value, sb);
        return sb.toString();
    }

    private static void writeValue(Object v, StringBuilder sb) {
        switch (v) {
            case null -> sb.append("null");
            case String s -> writeString(s, sb);
            case Boolean b -> sb.append(b);
            case Number n -> writeNumber(n, sb);
            case Map<?, ?> m -> writeMap(m, sb);
            case List<?> l -> writeList(l, sb);
            default -> throw new IllegalArgumentException(
                    "Tipo não suportado em JSON: " + v.getClass().getName());
        }
    }

    private static void writeMap(Map<?, ?> map, StringBuilder sb) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!(e.getKey() instanceof String key)) {
                throw new IllegalArgumentException("Chaves JSON têm de ser strings");
            }
            if (!first) sb.append(',');
            first = false;
            writeString(key, sb);
            sb.append(':');
            writeValue(e.getValue(), sb);
        }
        sb.append('}');
    }

    private static void writeList(List<?> list, StringBuilder sb) {
        sb.append('[');
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            writeValue(list.get(i), sb);
        }
        sb.append(']');
    }

    private static void writeNumber(Number n, StringBuilder sb) {
        if (n instanceof Double d) {
            sb.append(Double.isFinite(d) ? d.toString() : "null");
        } else if (n instanceof Float f) {
            sb.append(Float.isFinite(f) ? f.toString() : "null");
        } else {
            sb.append(n);
        }
    }

    private static void writeString(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }

    // ---------- leitura ----------

    /**
     * Faz parse de texto JSON para Map/List/String/Long/Double/Boolean/null.
     * Tolera espaços em branco à volta do valor. Lança {@link IllegalArgumentException}
     * com a posição do erro quando o texto é inválido.
     */
    public static Object parse(String text) {
        if (text == null) throw new IllegalArgumentException("Texto JSON nulo");
        Parser p = new Parser(text);
        p.skipWhitespace();
        Object v = p.parseValue();
        p.skipWhitespace();
        if (!p.atEnd()) throw p.error("Dados extra após o valor JSON");
        return v;
    }

    // ---------- helpers tipados ----------

    /** Converte para Map; lança IllegalArgumentException se não for um objeto JSON. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> asMap(Object v) {
        if (v instanceof Map<?, ?> m) return (Map<String, Object>) m;
        throw new IllegalArgumentException("Esperado objeto JSON, recebido: " + typeName(v));
    }

    /** Converte para List; null devolve lista vazia, outro tipo não-lista lança erro. */
    @SuppressWarnings("unchecked")
    public static List<Object> asList(Object v) {
        if (v == null) return List.of();
        if (v instanceof List<?> l) return (List<Object>) l;
        throw new IllegalArgumentException("Esperado array JSON, recebido: " + typeName(v));
    }

    /** Converte para String; null devolve null, Number/Boolean são convertidos. */
    public static String asString(Object v) {
        if (v == null) return null;
        if (v instanceof String s) return s;
        if (v instanceof Number || v instanceof Boolean) return String.valueOf(v);
        throw new IllegalArgumentException("Esperada string JSON, recebido: " + typeName(v));
    }

    /** Converte para long; null ou valor não numérico devolve {@code def}. */
    public static long asLong(Object v, long def) {
        if (v == null) return def;
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                return def;
            }
        }
        return def;
    }

    /** Converte para boolean; null devolve {@code def}. */
    public static boolean asBool(Object v, boolean def) {
        if (v == null) return def;
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.longValue() != 0;
        if (v instanceof String s) return Boolean.parseBoolean(s.trim());
        return def;
    }

    private static String typeName(Object v) {
        if (v == null) return "null";
        if (v instanceof Map) return "objeto";
        if (v instanceof List) return "array";
        if (v instanceof String) return "string";
        if (v instanceof Number) return "número";
        if (v instanceof Boolean) return "booleano";
        return v.getClass().getName();
    }

    // ---------- parser ----------

    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) { this.s = s; }

        boolean atEnd() { return pos >= s.length(); }

        IllegalArgumentException error(String msg) {
            return new IllegalArgumentException(msg + " (posição " + pos + ")");
        }

        void skipWhitespace() {
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') pos++;
                else break;
            }
        }

        Object parseValue() {
            if (atEnd()) throw error("Fim inesperado do texto JSON");
            char c = s.charAt(pos);
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> {
                    if (c == '-' || (c >= '0' && c <= '9')) yield parseNumber();
                    throw error("Valor JSON inválido");
                }
            };
        }

        private Object parseLiteral(String literal, Object value) {
            if (pos + literal.length() > s.length() || !s.startsWith(literal, pos)) {
                throw error("Literal inválido, esperado " + literal);
            }
            pos += literal.length();
            return value;
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (tryConsume('}')) return map;
            while (true) {
                skipWhitespace();
                if (atEnd() || s.charAt(pos) != '"') throw error("Esperada chave (string) no objeto");
                String key = parseString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                map.put(key, parseValue());
                skipWhitespace();
                if (tryConsume('}')) return map;
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (tryConsume(']')) return list;
            while (true) {
                skipWhitespace();
                list.add(parseValue());
                skipWhitespace();
                if (tryConsume(']')) return list;
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (atEnd()) throw error("String não terminada");
                char c = s.charAt(pos++);
                if (c == '"') return sb.toString();
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                if (atEnd()) throw error("Escape não terminado");
                char e = s.charAt(pos++);
                switch (e) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (pos + 4 > s.length()) throw error("Escape unicode incompleto");
                        String hex = s.substring(pos, pos + 4);
                        try {
                            sb.append((char) Integer.parseInt(hex, 16));
                        } catch (NumberFormatException ex) {
                            throw error("Escape unicode inválido: " + hex);
                        }
                        pos += 4;
                    }
                    default -> throw error("Escape inválido: \\" + e);
                }
            }
        }

        private Object parseNumber() {
            int start = pos;
            if (!atEnd() && s.charAt(pos) == '-') pos++;
            int digits = 0;
            while (!atEnd() && Character.isDigit(s.charAt(pos))) {
                pos++;
                digits++;
            }
            if (digits == 0) throw error("Número inválido");
            boolean real = false;
            if (!atEnd() && s.charAt(pos) == '.') {
                real = true;
                pos++;
                if (atEnd() || !Character.isDigit(s.charAt(pos))) throw error("Fração vazia no número");
                while (!atEnd() && Character.isDigit(s.charAt(pos))) pos++;
            }
            if (!atEnd() && (s.charAt(pos) == 'e' || s.charAt(pos) == 'E')) {
                real = true;
                pos++;
                if (!atEnd() && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) pos++;
                if (atEnd() || !Character.isDigit(s.charAt(pos))) throw error("Expoente vazio no número");
                while (!atEnd() && Character.isDigit(s.charAt(pos))) pos++;
            }
            String text = s.substring(start, pos);
            if (!real) {
                try {
                    return Long.valueOf(text);
                } catch (NumberFormatException ignored) {
                    // inteiro demasiado grande: fica como Double
                }
            }
            try {
                return Double.valueOf(text);
            } catch (NumberFormatException e) {
                throw error("Número inválido: " + text);
            }
        }

        private void expect(char c) {
            if (atEnd() || s.charAt(pos) != c) throw error("Esperado '" + c + "'");
            pos++;
        }

        private boolean tryConsume(char c) {
            if (!atEnd() && s.charAt(pos) == c) {
                pos++;
                return true;
            }
            return false;
        }
    }
}
