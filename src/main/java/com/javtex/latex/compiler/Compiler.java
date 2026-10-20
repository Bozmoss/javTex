package com.javtex.latex.compiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Compiler {
    public record CompilationResult(
        int exitCode,
        String output
    ) {
        public boolean isSuccess() {
            return exitCode == 0;
        }
    }

    public CompilationResult compile(Path texFile) throws IOException, InterruptedException {
        Path file = texFile.toAbsolutePath().normalize();

        if (!Files.isRegularFile(file)) {
            throw new IOException(
                "LaTeX file does not exist: " + file
            );
        }

        Path workingDirectory = file.getParent();

        ProcessBuilder builder = new ProcessBuilder(
            "pdflatex",
            "-interaction=nonstopmode",
            "-file-line-error",
            "-halt-on-error",
            file.getFileName().toString()
        );

        builder.directory(workingDirectory.toFile());
        builder.redirectErrorStream(true);

        Process process = builder.start();

        String output;

        try (var reader = process.inputReader()) {
            output = reader.lines().collect(java.util.stream.Collectors.joining(System.lineSeparator()));
        }

        int exitCode = process.waitFor();

        return new CompilationResult(exitCode, output);
    }
}