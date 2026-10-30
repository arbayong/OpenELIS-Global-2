package org.openelisglobal.license.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Service;

@Service
public class FingerprintService {

    private static final String CONFIG_DIR_PROPERTY = "aegis.config.dir";
    private static final String DEFAULT_CONFIG_DIR = "config";
    private static final String MACHINE_ID_FILE = "machine-id.txt";

    public String computeFingerprint() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(collectRawFingerprint().getBytes(StandardCharsets.UTF_8));
            return hex(hash);
        } catch (NoSuchAlgorithmException e) {
            LogEvent.logError(e);
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    protected String collectRawFingerprint() {
        StringBuilder parts = new StringBuilder();
        String os = System.getProperty("os.name", "");
        if (os.toLowerCase().contains("windows")) {
            append(parts, runValue("wmic", "diskdrive", "get", "SerialNumber", "/format:value"));
            append(parts, runValue("wmic", "csproduct", "get", "UUID", "/format:value"));
            append(parts, runValue("wmic", "bios", "get", "SerialNumber", "/format:value"));
            append(parts, parseGetmac());
        }
        String raw = parts.toString();
        if (raw.isEmpty()) {
            return persistedMachineId();
        }
        return raw;
    }

    private void append(StringBuilder parts, String value) {
        if (value != null && !value.isEmpty()) {
            if (parts.length() > 0) {
                parts.append('|');
            }
            parts.append(value);
        }
    }

    private String runValue(String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = readFully(process.getInputStream());
            process.waitFor();
            for (String line : output.split("\\r?\\n")) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq >= 0) {
                    String value = line.substring(eq + 1).trim();
                    if (!value.isEmpty()) {
                        return value;
                    }
                }
            }
        } catch (Exception e) {
            LogEvent.logWarn(e);
        }
        return "";
    }

    private String parseGetmac() {
        try {
            ProcessBuilder pb = new ProcessBuilder("getmac", "/fo", "csv", "/nh");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = readFully(process.getInputStream());
            process.waitFor();
            for (String line : output.split("\\r?\\n")) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] fields = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                if (fields.length > 0) {
                    String mac = fields[0].trim();
                    if (mac.startsWith("\"") && mac.endsWith("\"") && mac.length() > 1) {
                        mac = mac.substring(1, mac.length() - 1);
                    }
                    if (!mac.isEmpty()) {
                        return mac;
                    }
                }
            }
        } catch (Exception e) {
            LogEvent.logWarn(e);
        }
        return "";
    }

    private String persistedMachineId() {
        File dir = new File(configDir());
        File file = new File(dir, MACHINE_ID_FILE);
        if (file.exists()) {
            try {
                String id = Files.readString(file.toPath(), StandardCharsets.UTF_8).trim();
                if (!id.isEmpty()) {
                    return id;
                }
            } catch (IOException e) {
                LogEvent.logWarn(e);
            }
        }
        String id = UUID.randomUUID().toString();
        try {
            Files.createDirectories(dir.toPath());
            Files.writeString(file.toPath(), id, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LogEvent.logWarn(e);
        }
        return id;
    }

    private String configDir() {
        return System.getProperty(CONFIG_DIR_PROPERTY, DEFAULT_CONFIG_DIR);
    }

    private String readFully(java.io.InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
