package com.microfinance.base.service;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Service
@Slf4j
public class MfaService {
    
    @Value("${app.mfa.issuer:Microfinance System}")
    private String issuer;
    
    private static final String BACKUP_CODE_CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BACKUP_CODE_LENGTH = 8;
    private static final int BACKUP_CODE_COUNT = 10;
    private final SecureRandom random = new SecureRandom();
    
    public String generateSecret() {
        return new DefaultSecretGenerator().generate();
    }
    
    public String generateQrCodeImageUri(String secret, String username) {
        QrData data = new QrData.Builder()
                .label(username)
                .secret(secret)
                .issuer(issuer)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        
        try {
            QrGenerator generator = new ZxingPngQrGenerator();
            byte[] imageData = generator.generate(data);
            return getDataUriForImage(imageData, generator.getImageMimeType());
        } catch (QrGenerationException e) {
            log.error("Error generating QR code", e);
            throw new RuntimeException("Error generating QR code", e);
        }
    }
    
    public boolean verifyCode(String code, String secret) {
        if (code == null || secret == null) {
            return false;
        }
        
        CodeVerifier verifier = new DefaultCodeVerifier(
            new DefaultCodeGenerator(), 
            new SystemTimeProvider()
        );
        return verifier.isValidCode(secret, code);
    }
    
    public boolean isCodeValid(String code, String secret) {
        return code != null && secret != null && verifyCode(code, secret);
    }
    
    public List<String> generateBackupCodes() {
        return IntStream.range(0, BACKUP_CODE_COUNT)
                .mapToObj(i -> generateSingleBackupCode())
                .collect(Collectors.toList());
    }
    
    private String generateSingleBackupCode() {
        StringBuilder code = new StringBuilder(BACKUP_CODE_LENGTH);
        for (int i = 0; i < BACKUP_CODE_LENGTH; i++) {
            int index = random.nextInt(BACKUP_CODE_CHARACTERS.length());
            code.append(BACKUP_CODE_CHARACTERS.charAt(index));
        }
        return code.toString();
    }
    
    public boolean validateBackupCode(String inputCode, List<String> validBackupCodes) {
        return validBackupCodes.stream()
                .anyMatch(backupCode -> backupCode.equalsIgnoreCase(inputCode));
    }
}