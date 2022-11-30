package com.example.finalproject.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.example.finalproject.controller.response.MediaResponseDto;
import com.example.finalproject.domain.Media;
import com.example.finalproject.domain.Post;
import com.example.finalproject.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
// 미디어 파일들을 S3 , DB 에 저장하는 작업을 interface 로 따로 생성
public class ImageUpload implements ImageUploadInter {

    // S3 버킷
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    private final AmazonS3 amazonS3; // 아마존 S3 사용
    //    private final JPAQueryFactory jpaQueryFactory; // QueryDSL 사용
    private final MediaRepository mediaRepository; // 저장을 위한 Media jpa 사용

    // 미디어 파일들을 받아서 저장
    @Override
    public List<Media> filesUpload(List<MultipartFile> multipartFiles, Post post) {

        List<Media> mediaList = new ArrayList<>();

        for (MultipartFile multipartFile : multipartFiles) {

            String fileName = multipartFile.getOriginalFilename(); // 각 파일의 이름을 저장
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(multipartFile.getSize());
            objectMetadata.setContentType(multipartFile.getContentType());

            System.out.println("for each 진입 : " + fileName);

            try (InputStream inputStream = multipartFile.getInputStream()) {
                // S3에 업로드 및 저장
                amazonS3.putObject(new PutObjectRequest(bucket, fileName, inputStream, objectMetadata)
                        .withCannedAcl(CannedAccessControlList.PublicRead));
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다.");
            }

            // 접근가능한 URL 가져오기
            String mediaPath = amazonS3.getUrl(bucket, fileName).toString();

            // 동시에 해당 미디어 파일들을 미디어 DB에 이름과 Url 정보 저장.
            // 게시글마다 어떤 미디어 파일들을 포함하고 있는지 파악하기 위함 또는 활용하기 위함.
            Media media =
                    Media.builder()
                            .mediaName(fileName)
                            .mediaUrl(mediaPath)
                            .post(post)
                            .build();

            mediaRepository.save(media);

            mediaList.add(media);

        };

        return mediaList;
    }


    // S3에 저장되어있는 미디어 파일 삭제
    @Override
    public void deleteFile(String fileName) {
        amazonS3.deleteObject(new DeleteObjectRequest(bucket, fileName));
    }


    // 파일 업로드 시, 파일명을 난수화하기 위해 random으로 돌린다. (현재는 굳이 난수화할 필요가 없어보여 사용하지 않음)
    @Override
    public String createFileName(String fileName) {
        return UUID.randomUUID().toString().concat(getFileExtension(fileName));
    }


    // file 형식이 잘못된 경우를 확인하기 위해 만들어진 로직이며, 파일 타입과 상관없이 업로드할 수 있게 하기 위해 .의 존재 유무만 판단하였습니다.
    @Override
    public String getFileExtension(String fileName) {
        try {
            return fileName.substring(fileName.lastIndexOf("."));
        } catch (StringIndexOutOfBoundsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 형식의 파일(" + fileName + ") 입니다.");
        }
    }
}
