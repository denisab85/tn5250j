package org.tn5250j.cli;

import java.util.ArrayList;
import java.util.List;

/** Quote-aware split/join for persisted session argument strings. */
public final class StoredArguments {
    private StoredArguments() { }

    public static String[] split(String text) {
        List<String> args = new ArrayList<>();
        if (text == null) return new String[0];
        StringBuilder value = new StringBuilder();
        char quote = 0;
        boolean started = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (quote != 0) {
                if (ch == quote) quote = 0;
                else if (ch == '\\' && quote == '"' && i + 1 < text.length()
                        && (text.charAt(i + 1) == '"' || text.charAt(i + 1) == '\\')) value.append(text.charAt(++i));
                else value.append(ch);
            } else if (ch == '"' || ch == '\'') {
                quote = ch;
                started = true;
            } else if (Character.isWhitespace(ch)) {
                if (started) { args.add(value.toString()); value.setLength(0); started = false; }
            } else {
                value.append(ch);
                started = true;
            }
        }
        if (quote != 0) throw new IllegalArgumentException("Unclosed quote in saved session arguments");
        if (started) args.add(value.toString());
        return args.toArray(new String[0]);
    }

    public static String join(String[] args) {
        List<String> values = new ArrayList<>();
        for (String arg : args) if (arg != null) values.add(quote(arg));
        return String.join(" ", values);
    }

    public static String quote(String value) {
        if (!value.isEmpty() && !value.matches(".*[\\s\"'].*")) return value;
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
