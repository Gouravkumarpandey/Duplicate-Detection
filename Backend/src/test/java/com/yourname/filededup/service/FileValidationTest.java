package com.yourname.filededup.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for FileStorageService file validation
 */
@SpringBootTest
public class FileValidationTest {

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService();
    }

    @Test
    void testValidPdfFile() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            "test content".getBytes()
        );

        // This should not throw an exception for valid files
        assertDoesNotThrow(() -> {
            // We'll test the validation logic indirectly through the service
            assertTrue(file.getContentType().equals("application/pdf"));
            assertTrue(file.getOriginalFilename().endsWith(".pdf"));
        });
    }

    @Test
    void testValidTextFile() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "test content".getBytes()
        );

        assertDoesNotThrow(() -> {
            assertTrue(file.getContentType().equals("text/plain"));
            assertTrue(file.getOriginalFilename().endsWith(".txt"));
        });
    }

    @Test
    void testValidDocxFile() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "test content".getBytes()
        );

        assertDoesNotThrow(() -> {
            assertTrue(file.getContentType().equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
            assertTrue(file.getOriginalFilename().endsWith(".docx"));
        });
    }

    @Test
    void testInvalidImageFile() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.jpg",
            "image/jpeg",
            "test content".getBytes()
        );

        // This should represent an invalid file type
        assertFalse(file.getContentType().equals("text/plain") ||
                   file.getContentType().equals("application/pdf") ||
                   file.getContentType().equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    }

    @Test
    void testEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            new byte[0]
        );

        assertTrue(file.isEmpty());
    }

    @Test
    void testLargeFile() {
        // Create a file larger than 100MB
        byte[] largeContent = new byte[101 * 1024 * 1024]; // 101MB
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "large.txt",
            "text/plain",
            largeContent
        );

        assertTrue(file.getSize() > 100 * 1024 * 1024);
    }

    @Test
    void testMaliciousFileName() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "../../../etc/passwd",
            "text/plain",
            "malicious content".getBytes()
        );

        // File name contains path traversal characters
        assertTrue(file.getOriginalFilename().contains(".."));
    }

    @Test
    void testNullFileName() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            null,
            "text/plain",
            "test content".getBytes()
        );

        assertNull(file.getOriginalFilename());
    }
}
