package jotes.util;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extrai links internos no formato [[Título]] ou [[Título|texto]] do conteúdo. */
public final class LinkParser {
    private static final Pattern PATTERN =
            Pattern.compile("\\[\\[([^\\[\\]|]+?)(?:\\|[^\\[\\]]*?)?\\]\\]");

    private LinkParser() {}

    public static Pattern pattern() { return PATTERN; }

    /** Títulos referenciados no conteúdo, sem duplicados, pela ordem em que aparecem. */
    public static Set<String> extractTitles(String content) {
        Set<String> out = new LinkedHashSet<>();
        if (content == null) return out;
        Matcher m = PATTERN.matcher(content);
        while (m.find()) {
            String title = m.group(1).trim();
            if (!title.isEmpty()) out.add(title);
        }
        return out;
    }
}
