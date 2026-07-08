package com.ashutosh.cyberhelper.service;

import com.ashutosh.cyberhelper.dto.document.DocumentResponse;
import com.ashutosh.cyberhelper.dto.document.DocumentScanResponse;
import com.ashutosh.cyberhelper.entity.Document;
import com.ashutosh.cyberhelper.entity.DocumentStatus;
import com.ashutosh.cyberhelper.entity.DocumentType;
import com.ashutosh.cyberhelper.exception.DocumentNotFoundException;
import com.ashutosh.cyberhelper.repository.DocumentRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for document management.
 *
 * <p>
 * <b>Why this exists:</b> This service is the bridge between the physical
 * filesystem
 * (where PDFs live) and the database (where metadata lives). It scans the
 * configured
 * upload directory, detects new PDF files, and registers them in PostgreSQL.
 * </p>
 *
 * <p>
 * <b>How it fits into the RAG architecture:</b>
 * </p>
 * <ul>
 * <li>Week 3 (now): Scans, registers, and manages document metadata</li>
 * <li>Week 4: {@code DocumentProcessingService} will call
 * {@code getDocumentsByStatus(READY_FOR_PROCESSING)} to find documents
 * needing text extraction, then read the file at
 * {@code document.getFilePath()}</li>
 * <li>Week 5+: Chunking and embedding services reference documents via their
 * ID</li>
 * </ul>
 *
 * <p>
 * <b>Key design decisions:</b>
 * </p>
 * <ul>
 * <li>Duplicate detection uses {@code fileName} (unique constraint in DB)</li>
 * <li>Default document type is {@code OTHER} — admin can update via PATCH
 * endpoint</li>
 * <li>Delete removes metadata only by default; physical file deletion is
 * opt-in</li>
 * <li>Upload directory is auto-created at startup if it doesn't exist</li>
 * </ul>
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final Path uploadDirectory;

    public DocumentService(
            DocumentRepository documentRepository,
            @Value("${app.storage.upload-dir}") String uploadDir) {
        this.documentRepository = documentRepository;
        this.uploadDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * Ensures the upload directory exists at application startup.
     * Creates the directory tree if it doesn't exist.
     */
    @PostConstruct
    public void initStorageDirectory() {
        try {
            if (!Files.exists(uploadDirectory)) {
                Files.createDirectories(uploadDirectory);
                log.info("Created upload directory: {}", uploadDirectory);
            } else {
                log.info("Upload directory already exists: {}", uploadDirectory);
            }
        } catch (IOException e) {
            log.error("Failed to create upload directory: {}", uploadDirectory, e);
            throw new RuntimeException("Could not initialize storage directory", e);
        }
    }

    /**
     * Scans the upload directory for PDF files and registers any new ones in the
     * database.
     *
     * <p>
     * <b>Scan logic:</b>
     * </p>
     * <ol>
     * <li>List all {@code .pdf} and {@code .txt} files in the upload directory</li>
     * <li>For each file, check if {@code fileName} already exists in DB</li>
     * <li>If new, create a {@link Document} entity with status
     * {@code REGISTERED}</li>
     * <li>Return a summary of what was found and registered</li>
     * </ol>
     *
     * @return scan result summary
     */
    @Transactional
    public DocumentScanResponse scanAndRegisterDocuments() {
        log.info("Starting document scan in directory: {}", uploadDirectory);

        List<Path> documentFiles = listDocumentFiles();
        int totalFound = documentFiles.size();
        int alreadyRegistered = 0;
        List<DocumentResponse> newlyRegistered = new ArrayList<>();

        for (Path file : documentFiles) {
            String fileName = file.getFileName().toString();

            if (documentRepository.existsByFileName(fileName)) {
                log.debug("Document already registered, skipping: {}", fileName);
                alreadyRegistered++;
                continue;
            }

            Document document = registerDocument(file, fileName);
            newlyRegistered.add(DocumentResponse.from(document));
            log.info("Registered new document: {}", fileName);
        }

        log.info("Scan complete. Found: {}, New: {}, Already registered: {}",
                totalFound, newlyRegistered.size(), alreadyRegistered);

        return new DocumentScanResponse(
                totalFound,
                newlyRegistered.size(),
                alreadyRegistered,
                newlyRegistered);
    }

    /**
     * Returns all documents in the database.
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getAllDocuments() {
        return documentRepository.findAll()
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    /**
     * Returns a single document by ID.
     *
     * @throws DocumentNotFoundException if no document with the given ID exists
     */
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(
                        "Document not found with id: " + id));
        return DocumentResponse.from(document);
    }

    /**
     * Returns all documents with the given processing status.
     * Week 4 will use this with {@code READY_FOR_PROCESSING}.
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByStatus(DocumentStatus status) {
        return documentRepository.findByStatus(status)
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    /**
     * Returns all documents of the given type.
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByType(DocumentType type) {
        return documentRepository.findByDocumentType(type)
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    /**
     * Updates a document's processing status.
     *
     * <p>
     * Example: admin marks a document as {@code READY_FOR_PROCESSING} so that
     * Week 4's {@code DocumentProcessingService} will pick it up for text
     * extraction.
     * </p>
     *
     * @throws DocumentNotFoundException if no document with the given ID exists
     */
    @Transactional
    public DocumentResponse updateDocumentStatus(Long id, DocumentStatus status) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(
                        "Document not found with id: " + id));

        document.setStatus(status);
        Document updated = documentRepository.save(document);
        log.info("Updated document {} status to {}", id, status);
        return DocumentResponse.from(updated);
    }

    /**
     * Bulk updates all documents with status REGISTERED to READY_FOR_PROCESSING.
     */
    @Transactional
    public List<DocumentResponse> updateAllRegisteredToReady() {
        List<Document> documents = documentRepository.findByStatus(DocumentStatus.REGISTERED);
        log.info("Bulk updating {} registered documents to READY_FOR_PROCESSING", documents.size());
        
        for (Document document : documents) {
            document.setStatus(DocumentStatus.READY_FOR_PROCESSING);
        }
        
        return documentRepository.saveAll(documents)
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    /**
     * Deletes a document's metadata from the database.
     *
     * <p>
     * By default, only the database record is removed. If {@code deleteFile} is
     * {@code true}, the physical file on disk is also deleted.
     * </p>
     *
     * @param id         document ID
     * @param deleteFile whether to also delete the physical file
     * @throws DocumentNotFoundException if no document with the given ID exists
     */
    @Transactional
    public void deleteDocument(Long id, boolean deleteFile) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(
                        "Document not found with id: " + id));

        if (deleteFile) {
            deletePhysicalFile(document.getFilePath());
        }

        documentRepository.delete(document);
        log.info("Deleted document metadata for id: {} (file deleted: {})", id, deleteFile);
    }

    // ─────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────

    /**
     * Lists all PDF and TXT files in the upload directory.
     */
    private List<Path> listDocumentFiles() {
        List<Path> documentFiles = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(uploadDirectory, "*.{pdf,txt}")) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    documentFiles.add(entry);
                }
            }
        } catch (IOException e) {
            log.error("Failed to scan upload directory: {}", uploadDirectory, e);
            throw new RuntimeException("Failed to scan upload directory", e);
        }

        return documentFiles;
    }

    /**
     * Creates and persists a new Document entity from a file on disk.
     */
    private Document registerDocument(Path file, String fileName) {
        long fileSize;
        LocalDateTime lastModified;

        try {
            fileSize = Files.size(file);
            lastModified = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(file.toFile().lastModified()),
                    ZoneId.systemDefault());
        } catch (IOException e) {
            log.error("Failed to read file attributes for: {}", fileName, e);
            throw new RuntimeException("Failed to read file: " + fileName, e);
        }

        Document document = Document.builder()
                .fileName(fileName)
                .originalFileName(fileName)
                .documentType(DocumentType.OTHER)
                .filePath(uploadDirectory.resolve(fileName).toString())
                .fileSize(fileSize)
                .status(DocumentStatus.READY_FOR_PROCESSING)   // auto-ready for Week 4 pipeline
                .uploadDate(lastModified)
                .build();

        return documentRepository.save(document);
    }

    /**
     * Deletes a physical file from disk. Logs a warning if deletion fails
     * (e.g., file already removed manually).
     */
    private void deletePhysicalFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                log.info("Deleted physical file: {}", filePath);
            } else {
                log.warn("Physical file not found (may have been removed manually): {}", filePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete physical file: {}", filePath, e);
            throw new RuntimeException("Failed to delete file: " + filePath, e);
        }
    }
}
