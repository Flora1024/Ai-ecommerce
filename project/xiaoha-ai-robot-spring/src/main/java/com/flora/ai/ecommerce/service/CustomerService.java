package com.flora.ai.ecommerce.service;

import com.flora.ai.ecommerce.model.vo.customerService.DeleteMarkdownFileReqVO;
import com.flora.ai.ecommerce.model.vo.customerService.FindMarkdownFilePageListReqVO;
import com.flora.ai.ecommerce.model.vo.customerService.FindMarkdownFilePageListRspVO;
import com.flora.ai.ecommerce.model.vo.customerService.UpdateMarkdownFileReqVO;
import com.flora.ai.ecommerce.utils.PageResponse;
import com.flora.ai.ecommerce.utils.Response;
import org.springframework.web.multipart.MultipartFile;

public interface CustomerService {
    /**
     * 上传文件
     * @param file 文件
     * @return 结果
     */
    Response<?> uploadMarkDownFile(MultipartFile file);

    /**
     * 删除文件
     * @param deleteMarkdownFileReqVO
     * @return 删除结果
     */
    Response<?> deleteMarkdownFile(DeleteMarkdownFileReqVO deleteMarkdownFileReqVO);

    /**
     * 查询文件列表
     * @param findMarkdownFilePageListReqVO
     * @return 文件列表
     */
    PageResponse<FindMarkdownFilePageListRspVO> findMarkdownFilePageList(FindMarkdownFilePageListReqVO findMarkdownFilePageListReqVO);

    /**
     * 更新文件
     * @param updateMarkdownFileReqVO
     * @return 更新结果
     */
    Response<?> UpdateMarkdownFile(UpdateMarkdownFileReqVO updateMarkdownFileReqVO);
}
