package com.ai.aicodemother.core.saver;

import cn.hutool.core.util.StrUtil;
import com.ai.aicodemother.ai.model.HtmlCodeResult;
import com.ai.aicodemother.exception.BusinessException;
import com.ai.aicodemother.exception.ErrorCode;
import com.ai.aicodemother.model.enums.CodeGenTypeEnum;

/**
 * HTML 代码文件保存器 - 实现类
 */
public class HtmlCodeFileSaverTemplate extends CodeFileSaverTemplate<HtmlCodeResult> {


    @Override
    protected void validateInput(HtmlCodeResult result) {
        // 首先对 result 非空校验
        super.validateInput(result);
        // 接下来校验 HTML 代码是否为空
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML 代码不能为空");
        }

    }

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void saveFiles(HtmlCodeResult result, String baseDirPath) {
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
    }
}
