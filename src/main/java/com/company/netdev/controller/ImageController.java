package com.company.netdev.controller;

import com.company.netdev.common.BizException;
import com.company.netdev.common.Result;
import com.company.netdev.entity.UploadImage;
import com.company.netdev.repo.ImageRepository;
import com.company.netdev.service.FileService;
import com.company.netdev.service.LogService;
import com.company.netdev.config.AuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageRepository imageRepository;
    private final FileService fileService;
    private final LogService logService;

    @GetMapping
    public Result<List<UploadImage>> list(@RequestParam String bizType, @RequestParam Long bizId) {
        return Result.ok(imageRepository.findByBizTypeAndBizIdOrderByIdAsc(bizType, bizId));
    }

    @PostMapping("/upload")
    public Result<UploadImage> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam String bizType,
                                      @RequestParam Long bizId,
                                      HttpServletRequest request) {
        String stored = fileService.save(file);
        UploadImage img = new UploadImage();
        img.setBizType(bizType);
        img.setBizId(bizId);
        img.setStoredName(stored);
        img.setOriginalName(file.getOriginalFilename());
        img.setSize(file.getSize());
        imageRepository.save(img);
        logService.log(AuthInterceptor.currentUserId(request), AuthInterceptor.currentUsername(request),
                "图片", "上传", "上传图片(" + bizType + "#" + bizId + ") " + file.getOriginalFilename(),
                request.getRemoteAddr());
        return Result.ok(img);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        UploadImage img = imageRepository.findById(id).orElseThrow(() -> new BizException("图片不存在"));
        fileService.delete(img.getStoredName());
        imageRepository.delete(img);
        logService.log(AuthInterceptor.currentUserId(request), AuthInterceptor.currentUsername(request),
                "图片", "删除", "删除图片(" + img.getBizType() + "#" + img.getBizId() + ") " + img.getOriginalName(),
                request.getRemoteAddr());
        return Result.ok();
    }
}
