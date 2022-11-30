package com.example.finalproject.service;

import com.example.finalproject.domain.Media;
import com.example.finalproject.domain.Post;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ImageUploadInter {

    List<Media> filesUpload(List<MultipartFile> multipartFiles, Post post);
    void deleteFile(String fileName);
    String createFileName(String fileName);
    String getFileExtension(String fileName);

}
