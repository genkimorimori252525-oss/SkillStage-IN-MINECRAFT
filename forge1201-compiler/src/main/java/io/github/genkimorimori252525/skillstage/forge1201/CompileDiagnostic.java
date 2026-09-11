package io.github.genkimorimori252525.skillstage.forge1201;

public record CompileDiagnostic(
        DiagnosticSeverity severity,
        String code,
        String path,
        String message
) implements Comparable<CompileDiagnostic> {
    public CompileDiagnostic {
        if (severity == null) throw new IllegalArgumentException("severity must not be null");
        code = requireText(code, "code");
        path = requireText(path, "path");
        message = requireText(message, "message");
    }

    public boolean isError() {
        return severity == DiagnosticSeverity.ERROR;
    }

    @Override
    public int compareTo(CompileDiagnostic other) {
        int severityCompare = severity.compareTo(other.severity);
        if (severityCompare != 0) return severityCompare;
        int codeCompare = code.compareTo(other.code);
        if (codeCompare != 0) return codeCompare;
        int pathCompare = path.compareTo(other.path);
        if (pathCompare != 0) return pathCompare;
        return message.compareTo(other.message);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }
}