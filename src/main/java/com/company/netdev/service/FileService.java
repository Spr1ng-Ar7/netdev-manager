package com.company.netdev.service;

import com.company.netdev.common.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    /** 保存上传文件，返回存储文件名（UUID.扩展名） */
    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件为空");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0 && dot < original.length() - 1) {
            ext = original.substring(dot + 1).toLowerCase();
        }
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BizException("仅支持图片格式：jpg / png / gif / webp / bmp");
        }
        String stored = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath();
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), dir.resolve(stored), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BizException(500, "文件保存失败：" + e.getMessage());
        }
        return stored;
    }

    public void delete(String storedName) {
        try {
            Files.deleteIfExists(Paths.get(uploadDir).toAbsolutePath().resolve(storedName));
        } catch (IOException ignored) {
            // 磁盘文件删除失败不影响业务
        }
    }
}
