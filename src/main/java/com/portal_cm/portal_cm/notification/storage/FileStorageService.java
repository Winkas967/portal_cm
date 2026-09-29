package com.portal_cm.portal_cm.notification.storage;

import com.portal_cm.portal_cm.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Guarda os anexos numa pasta do servidor (app.storage.dir).
 * O banco guarda apenas o caminho relativo, ex.: "2026/09/3f2a...c1.jpg".
 */
@Service
public class FileStorageService {

    public static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final Path baseDir;

    public FileStorageService(@Value("${app.storage.dir}") String storageDir) throws IOException {
        this.baseDir = Paths.get(storageDir).toAbsolutePath().normalize();
        Files.createDirectories(baseDir);
        log.info("Anexos serão gravados em {}", baseDir);
    }

    /** Confere tamanho e tipo real (pelo conteúdo, não pela extensão). Não grava nada. */
    public void validate(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("O arquivo '" + originalName(file) + "' é maior que 10 MB.");
        }
        detectType(file);
    }

    public StoredFile store(MultipartFile file) {
        validate(file);
        FileType type = detectType(file);

        LocalDate today = LocalDate.now();
        String relativePath = String.format("%d/%02d/%s%s",
                today.getYear(), today.getMonthValue(), UUID.randomUUID(), type.extension);
        Path target = resolve(relativePath);

        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar o arquivo no servidor.", e);
        }

        return new StoredFile(relativePath, originalName(file), type.contentType, file.getSize());
    }

    public Resource load(String relativePath) {
        Path path = resolve(relativePath);
        if (!Files.isReadable(path)) {
            log.error("Anexo registrado no banco mas ausente no disco: {}", path);
            throw new NotFoundException("Arquivo não encontrado no servidor.");
        }
        return new FileSystemResource(path);
    }

    /** Usado para desfazer gravações quando a ficha não é salva. Nunca lança exceção. */
    public void deleteQuietly(String relativePath) {
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException | RuntimeException e) {
            log.warn("Não foi possível apagar o arquivo {}: {}", relativePath, e.getMessage());
        }
    }

    /** Impede caminhos que saiam da pasta base (ex.: "../../etc/passwd"). */
    private Path resolve(String relativePath) {
        Path path = baseDir.resolve(relativePath).normalize();
        if (!path.startsWith(baseDir)) {
            throw new IllegalArgumentException("Caminho de arquivo inválido.");
        }
        return path;
    }

    private FileType detectType(MultipartFile file) {
        byte[] h;
        try (InputStream in = file.getInputStream()) {
            h = in.readNBytes(8);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o arquivo enviado.", e);
        }

        if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return FileType.JPEG;
        }
        if (h.length >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G'
                && h[4] == 0x0D && h[5] == 0x0A && h[6] == 0x1A && h[7] == 0x0A) {
            return FileType.PNG;
        }
        if (h.length >= 5 && h[0] == '%' && h[1] == 'P' && h[2] == 'D' && h[3] == 'F' && h[4] == '-') {
            return FileType.PDF;
        }
        throw new IllegalArgumentException("O arquivo '" + originalName(file) + "' não é um JPG, PNG ou PDF válido.");
    }

    private String originalName(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            return "arquivo";
        }
        name = name.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private enum FileType {
        JPEG("image/jpeg", ".jpg"),
        PNG("image/png", ".png"),
        PDF("application/pdf", ".pdf");

        private final String contentType;
        private final String extension;

        FileType(String contentType, String extension) {
            this.contentType = contentType;
            this.extension = extension;
        }
    }
}