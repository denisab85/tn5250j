package org.tn5250j.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Quote-aware split/join for persisted session argument strings. */
public final class StoredArguments {
    private static final Map<String, String> LEGACY_FLAGS = legacyFlags();

    private StoredArguments() { }

    private static Map<String, String> legacyFlags() {
        Map<String, String> flags = new LinkedHashMap<>();
        flags.put("-server", "--server");
        flags.put("-remote", "--remote");
        flags.put("-remoteToken", "--remote-token");
        flags.put("-nc", "--new-instance");
        flags.put("-width", "--width");
        flags.put("-height", "--height");
        flags.put("-cp", "--code-page");
        flags.put("-132", "--wide");
        flags.put("-usp", "--proxy");
        flags.put("-sph", "--proxy-host");
        flags.put("-spp", "--proxy-port");
        flags.put("-sslType", "--ssl-type");
        flags.put("-dn=hostname", "--device-name-from-hostname");
        flags.put("-dn", "--device-name");
        flags.put("-hb", "--heartbeat");
        flags.put("-noembed", "--new-window");
        return flags;
    }

    /** Rewrites pre-POSIX flags found in saved session files before picocli parsing. */
    public static String[] normalizeLegacy(String[] args) {
        if (args == null || args.length == 0) return new String[0];
        List<String> normalized = new ArrayList<>();
        for (String arg : args) {
            if (arg == null) continue;
            if (!arg.startsWith("-") || arg.startsWith("--")) {
                normalized.add(arg);
                continue;
            }
            if ("-dn=hostname".equals(arg)) {
                normalized.add("--device-name-from-hostname");
                continue;
            }
            boolean matched = false;
            for (Map.Entry<String, String> entry : LEGACY_FLAGS.entrySet()) {
                String legacy = entry.getKey();
                if ("-dn=hostname".equals(legacy)) continue;
                if (arg.equals(legacy)) {
                    normalized.add(entry.getValue());
                    matched = true;
                    break;
                }
                if (arg.startsWith(legacy) && arg.length() > legacy.length()) {
                    normalized.add(entry.getValue());
                    normalized.add(arg.substring(legacy.length()));
                    matched = true;
                    break;
                }
            }
            if (!matched) normalized.add(arg);
        }
        return normalized.toArray(new String[0]);
    }

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
        if (value.isEmpty()) {
            return "\"\"";
        }
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isWhitespace(ch) || ch == '"' || ch == '\'') {
                return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
            }
        }
        return value;
    }
}
