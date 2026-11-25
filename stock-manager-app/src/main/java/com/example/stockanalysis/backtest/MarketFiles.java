package com.example.stockanalysis.backtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

final class MarketFiles {
    private MarketFiles() {}

    static void write(ObjectMapper json, Path path, Object value) throws IOException {
        Files.createDirectories(path.getParent());
        Path tmp = Files.createTempFile(path.getParent(), ".writing-", ".tmp");
        try {
            try (OutputStream stream = Files.newOutputStream(tmp);
                 OutputStream output = compressed(path) ? new GZIPOutputStream(stream) : stream) {
                json.writeValue(output, value);
            }
            Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(tmp); }
    }

    static <T> T read(ObjectMapper json, Path path, Class<T> type) throws IOException {
        try (InputStream stream = Files.newInputStream(path);
             InputStream input = compressed(path) ? new GZIPInputStream(stream) : stream) {
            return json.readValue(input, type);
        }
    }

    private static boolean compressed(Path path) { return path.getFileName().toString().endsWith(".gz"); }
}
