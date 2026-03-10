package com.flora.ai.ecommerce.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.unit.DataSizeUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Maps;
import com.flora.ai.ecommerce.domain.dos.AiCustomerServiceMdStorageDO;
import com.flora.ai.ecommerce.domain.mapper.MdStorageMapper;
import com.flora.ai.ecommerce.enums.AiCustomerServiceMdStatusEnum;
import com.flora.ai.ecommerce.enums.ResponseCodeEnum;
import com.flora.ai.ecommerce.event.MdUploadedEvent;
import com.flora.ai.ecommerce.exception.BizException;
import com.flora.ai.ecommerce.model.vo.customerService.DeleteMarkdownFileReqVO;
import com.flora.ai.ecommerce.model.vo.customerService.FindMarkdownFilePageListReqVO;
import com.flora.ai.ecommerce.model.vo.customerService.FindMarkdownFilePageListRspVO;
import com.flora.ai.ecommerce.model.vo.customerService.UpdateMarkdownFileReqVO;
import com.flora.ai.ecommerce.service.CustomerService;
import com.flora.ai.ecommerce.utils.PageResponse;
import com.flora.ai.ecommerce.utils.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    @Resource
    private ApplicationEventPublisher eventpublisher;
    @Resource
    private MdStorageMapper mdStorageMapper;
    @Value("${customer-service.md-storage-path}")
    private String mdStoragePath;
    @Resource
    private VectorStore vectorStore;

    /**
     * 上传 Markdown 文件
     * @param file 文件
     * @return
     */
    @Override
    public Response<?> uploadMarkDownFile(MultipartFile file) {
        // 校验文件是否为空
        if (file == null || file.isEmpty()) {
            return Response.fail("文件不能为空");
        }
        // 获取原始文件名
        String originalFilename = file.getOriginalFilename();
        // 验证文件类型
        if (StringUtils.isBlank(originalFilename) || !isMarkDownFile(originalFilename)) {
            throw new BizException(ResponseCodeEnum.ONLY_SUPPORT_MARKDOWN);
        }

        try {
            // 生成新的文件名,防止上传覆盖
            String newFileName = UUID.randomUUID().toString() + "-" + originalFilename;
            // 构建存储路径
            Path storageDirectory = Paths.get(mdStoragePath);
            Path targetPath = storageDirectory.resolve(newFileName);

            // 确保目录存在
            if (!Files.exists(storageDirectory)) {
                Files.createDirectory(storageDirectory);
            }
            // 保存文件
            file.transferTo(targetPath.toFile());

            // 记录操作日志
            log.info("## Markdown 问答文件存储成功, 文件名：{} -> 存储路径：{}", originalFilename, targetPath);

            // 存储入库
            AiCustomerServiceMdStorageDO aiCustomerServiceMdStorageDO = AiCustomerServiceMdStorageDO.builder()
                    .filePath(targetPath.toString())
                    .fileSize(file.getSize())
                    .originalFileName(originalFilename)
                    .newFileName(newFileName)
                    .status(AiCustomerServiceMdStatusEnum.PENDING.getCode())
                    .updateTime(LocalDateTime.now())
                    .createTime(LocalDateTime.now())
                    .build();

            mdStorageMapper.insert(aiCustomerServiceMdStorageDO);

            // 获取主键 ID
            Long id = aiCustomerServiceMdStorageDO.getId();

            // 元数据
            Map<String, Object> metadatas = Maps.newHashMap();
            metadatas.put("mdStorageId", id);
            metadatas.put("originalFileName", originalFilename);

            // 发布事件
            eventpublisher.publishEvent(MdUploadedEvent.builder()
                            .id(id)
                            .filePath(targetPath.toString())
                            .metadatas(metadatas)
                            .build());

            return Response.success();

        } catch (Exception e) {
            log.error("上传文件失败", e);
            throw new BizException(ResponseCodeEnum.UPLOAD_FILE_FAILED);
        }
    }

    /**
     * 删除 Markdown 文件
     * @param deleteMarkdownFileReqVO
     * @return
     */
    @Override
    public Response<?> deleteMarkdownFile(DeleteMarkdownFileReqVO deleteMarkdownFileReqVO) {
        Long id = deleteMarkdownFileReqVO.getId();
        AiCustomerServiceMdStorageDO aiCustomerServiceMdStorageDO = mdStorageMapper.selectById(id);
        // 文件不存在
        if (Objects.isNull(aiCustomerServiceMdStorageDO)) {
            throw new BizException(ResponseCodeEnum.MARKDOWN_FILE_NOT_FOUND);
        }
        // 文件正在处理，无法删除
        AiCustomerServiceMdStatusEnum statusEnum = AiCustomerServiceMdStatusEnum.codeOf(aiCustomerServiceMdStorageDO.getStatus());
        if (Objects.equals(statusEnum, AiCustomerServiceMdStatusEnum.VECTORIZING) || Objects.equals(statusEnum, AiCustomerServiceMdStatusEnum.PENDING)) {
            throw new BizException(ResponseCodeEnum.MARKDOWN_FILE_CANT_DELETE);
        }
        // 删除文件表记录
        mdStorageMapper.deleteById(id);
        // 删除向量数据
        vectorStore.delete(String.format("mdStorageId == %s", id));
        // 删除本地文件
        String filePath = aiCustomerServiceMdStorageDO.getFilePath();
        try {
            FileUtils.forceDelete(new File(filePath));
        } catch (Exception e) {
            log.error("删除文件失败", e);
        }

        return Response.success();
    }

    /**
     * 查询 Markdown 文件列表
     * @param findMarkdownFilePageListReqVO
     * @return
     */
    @Override
    public PageResponse<FindMarkdownFilePageListRspVO> findMarkdownFilePageList(FindMarkdownFilePageListReqVO findMarkdownFilePageListReqVO) {
        Long current = findMarkdownFilePageListReqVO.getCurrent();
        Long size = findMarkdownFilePageListReqVO.getSize();

        // 执行分页查询
        Page<AiCustomerServiceMdStorageDO> mdStorageDOPage = mdStorageMapper.selectPageList(current, size);
        List<AiCustomerServiceMdStorageDO> mdStorageDOS = mdStorageDOPage.getRecords();

        // DO 转 VO
        List<FindMarkdownFilePageListRspVO> vos = null;

        if (CollUtil.isNotEmpty(mdStorageDOS)) {
            vos = mdStorageDOS.stream()
                    .map(mdStorageDO -> FindMarkdownFilePageListRspVO.builder()
                            .id(mdStorageDO.getId())
                            .originalFileName(mdStorageDO.getOriginalFileName())
                            .remark(mdStorageDO.getRemark())
                            .fileSize(DataSizeUtil.format(mdStorageDO.getFileSize()))
                            .status(mdStorageDO.getStatus())
                            .createTime(mdStorageDO.getCreateTime())
                            .updateTime(mdStorageDO.getUpdateTime())
                            .build())
                    .collect(Collectors.toList());
        }

        return PageResponse.success(mdStorageDOPage, vos);
    }
    public boolean isMarkDownFile(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return false;
        }
        String fileExtension = FilenameUtils.getExtension(fileName);
        return StringUtils.equalsIgnoreCase(fileExtension, "md");
    }

    /**
     * 更新Markdown文件
     * @param updateMarkdownFileReqVO
     * @return 更新结果
     */
    public Response<?> UpdateMarkdownFile(UpdateMarkdownFileReqVO updateMarkdownFileReqVO) {
        Long id = updateMarkdownFileReqVO.getId();
        String remark = updateMarkdownFileReqVO.getRemark();

        AiCustomerServiceMdStorageDO aiCustomerServiceMdStorageDO = mdStorageMapper.selectById(id);

        int count = mdStorageMapper.updateById(AiCustomerServiceMdStorageDO.builder()
                .id(id)
                .remark(remark)
                .updateTime(LocalDateTime.now()).build());

        if (count == 0) {
            throw new BizException(ResponseCodeEnum.MARKDOWN_FILE_NOT_FOUND);
        }

        return Response.success();
    }
}
