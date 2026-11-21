package cl.rinconsur.usuarios.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class AvatarStorageService {

    private final Path directorioBase;

    public AvatarStorageService(
            @Value("${app.uploads.avatares:uploads/avatares}") String rutaCarpeta
    ) {
        this.directorioBase = Paths.get(rutaCarpeta).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.directorioBase);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear la carpeta de avatares", e);
        }
    }

    public String guardar(Long idUsuario, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen del avatar es obligatoria");
        }

        String extension = resolverExtension(archivo.getContentType());
        String nombreArchivo = "usuario_" + idUsuario + "_" + System.currentTimeMillis() + extension;

        try {
            Files.createDirectories(directorioBase);
            Path destino = directorioBase.resolve(nombreArchivo).normalize();
            if (!destino.startsWith(directorioBase)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre de archivo inválido");
            }
            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
            return nombreArchivo;
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo guardar la imagen en el servidor"
            );
        }
    }

    public Resource cargar(String nombreArchivo) {
        try {
            Path ruta = directorioBase.resolve(nombreArchivo).normalize();
            if (!ruta.startsWith(directorioBase)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ruta de archivo inválida");
            }
            Resource recurso = new UrlResource(ruta.toUri());
            if (!recurso.exists() || !recurso.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Avatar no encontrado");
            }
            return recurso;
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Avatar no encontrado");
        }
    }

    public MediaType detectarMediaType(String nombreArchivo) {
        try {
            Path ruta = directorioBase.resolve(nombreArchivo).normalize();
            String tipo = Files.probeContentType(ruta);
            if (tipo != null) {
                return MediaType.parseMediaType(tipo);
            }
        } catch (Exception ignored) {
        }
        String lower = nombreArchivo.toLowerCase();
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.IMAGE_JPEG;
    }

    public void eliminarSiExiste(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) return;
        String nombreArchivo = avatarUrl.substring(avatarUrl.lastIndexOf('/') + 1);
        if (nombreArchivo.isBlank()) return;
        try {
            Path ruta = directorioBase.resolve(nombreArchivo).normalize();
            if (ruta.startsWith(directorioBase)) {
                Files.deleteIfExists(ruta);
            }
        } catch (IOException ignored) {
        }
    }

    private String resolverExtension(String contentType) {
        if (contentType == null) return ".jpg";
        return switch (contentType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Formato de imagen no permitido. Usa JPG, PNG o WEBP."
            );
        };
    }
}