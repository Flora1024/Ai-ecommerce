package com.flora.ai.ecommerce.domain.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flora.ai.ecommerce.domain.dos.AiCustomerServiceMdStorageDO;

public interface MdStorageMapper extends BaseMapper<AiCustomerServiceMdStorageDO> {

    /**
     * 分页查询 Markdown 文件列表
     * @param current
     * @param size
     * @return
     */
    default Page<AiCustomerServiceMdStorageDO> selectPageList(Long current, Long size) {
        // 分页对象
        Page<AiCustomerServiceMdStorageDO> page = new Page<>(current, size);
        // 构建查询条件
        LambdaQueryWrapper<AiCustomerServiceMdStorageDO> wrapper = Wrappers.<AiCustomerServiceMdStorageDO>lambdaQuery()
                .orderByDesc(AiCustomerServiceMdStorageDO::getCreateTime);

        return selectPage(page, wrapper);
    }
}
