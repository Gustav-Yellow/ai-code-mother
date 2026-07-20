package com.ai.aicodemother.core;

import cn.hutool.json.JSONUtil;
import com.ai.aicodemother.ai.AiCodeGeneratorService;
import com.ai.aicodemother.ai.AiCodeGeneratorServiceFactory;
import com.ai.aicodemother.ai.model.HtmlCodeResult;
import com.ai.aicodemother.ai.model.MultiFileCodeResult;
import com.ai.aicodemother.ai.model.message.AiResponseMessage;
import com.ai.aicodemother.ai.model.message.ToolExecutedMessage;
import com.ai.aicodemother.ai.model.message.ToolRequestMessage;
import com.ai.aicodemother.core.parser.CodeParserExecutor;
import com.ai.aicodemother.core.saver.CodeFileSaverExecutor;
import com.ai.aicodemother.exception.BusinessException;
import com.ai.aicodemother.exception.ErrorCode;
import com.ai.aicodemother.model.enums.CodeGenTypeEnum;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.View;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成外观类，组合生成和保存功能
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    // 需要根据不同的应用id，获取不同的 AiCodeGeneratorService 实例，不再直接引用 AiCodeGeneratorService 实例
    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    /**
     * 统一入口：根据类型生成并保存代码
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId 应用id
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        // 从 AiService 工厂中根据 appId 和 CodeGenType，获取对应的 AiCodeGeneratorService 实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 统一入口：根据类型生成并保存代码（流式）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId 应用id
     * @return 流式响应
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        // 从 AiService 工厂中根据 appId 和 codeGenType，获取对应的 AiCodeGeneratorService 实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            case VUE_PROJECT -> {
                TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                yield processTokenStream(tokenStream, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 充当适配器，将 TokenStream 转换成 Flux
     *
     * @param tokenStream token流
     * @param codeGenTypeEnum 代码生成类型
     * @param appId 应用id
     * @return 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        return Flux.create(sink -> {
            tokenStream.onPartialResponse((String partialResponse) -> {
                        // 这里获取到的是 AI 生成的回复，需要将 partialResponse 封装成 AiResponseMessage 对象，再转换成 JSON 字符串
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        // 这里获取到的是工具请求，需要将 toolExecutionRequest 封装成 ToolRequestMessage 对象，再转换成 JSON 字符串
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        // 这里获取到的是工具执行完成，需要将 toolExecution 封装成 ToolExecutedMessage 对象，再转换成 JSON 字符串
                        ToolExecutedMessage toolExecutionMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutionMessage));
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        // 这里是最终响应
                        sink.complete();
                    })
                    .onError((Throwable error) -> {
                        error.printStackTrace();
                        sink.error(error);
                    })
                    .start();
        });
    }

    /**
     * 生成多文件模式的代码并保存（流式）
     * @param result 代码流
     * @param codeGenTypeEnum 代码生成类型
     * @param appId 应用id
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> result, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        StringBuilder codeBuilder = new StringBuilder();
        return result
                .doOnNext(chunk -> {
                    codeBuilder.append(chunk);
                }).doOnComplete(() -> {
                    try {
                        String completeCode = codeBuilder.toString();
                        Object codeResult = CodeParserExecutor.executeParser(completeCode, codeGenTypeEnum);
                        File savedDir = CodeFileSaverExecutor.executeSaver(codeResult, codeGenTypeEnum, appId);
                        log.info("保存成功，路径为：" + savedDir.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("保存失败: {}", e.getMessage());
                    }
                });
    }

//    /**
//     * 生成 HTML 模式的代码并保存（流式）
//     *
//     * @param userMessage 用户提示词
//     * @return 保存的目录
//     */
//    private Flux<String> generateAndSaveHtmlCodeStream(String userMessage) {
//        Flux<String> result = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
//        // 当流式返回生成代码完成后，再保存代码
//        StringBuilder codeBuilder = new StringBuilder();
//        return result
//                .doOnNext(chunk -> {
//                    // 实时收集代码片段
//                    codeBuilder.append(chunk);
//                })
//                .doOnComplete(() -> {
//                    // 流式返回完成后保存代码
//                    try {
//                        String completeHtmlCode = codeBuilder.toString();
//                        HtmlCodeResult htmlCodeResult = CodeParser.parseHtmlCode(completeHtmlCode);
//                        // 保存代码到文件
//                        File savedDir = CodeFileSaver.saveHtmlCodeResult(htmlCodeResult);
//                        log.info("保存成功，路径为：" + savedDir.getAbsolutePath());
//                    } catch (Exception e) {
//                        log.error("保存失败: {}", e.getMessage());
//                    }
//                });
//    }
//
//
//    /**
//     * 生成多文件模式的代码并保存（流式）
//     *
//     * @param userMessage 用户提示词
//     * @return 保存的目录
//     */
//    private Flux<String> generateAndSaveMultiFileCodeStream(String userMessage) {
//        Flux<String> result = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
//        // 当流式返回生成代码完成后，再保存代码
//        StringBuilder codeBuilder = new StringBuilder();
//        return result
//                .doOnNext(chunk -> {
//                    // 实时收集代码片段
//                    codeBuilder.append(chunk);
//                })
//                .doOnComplete(() -> {
//                    // 流式返回完成后保存代码
//                    try {
//                        String completeMultiFileCode = codeBuilder.toString();
//                        MultiFileCodeResult multiFileResult = CodeParser.parseMultiFileCode(completeMultiFileCode);
//                        // 保存代码到文件
//                        File savedDir = CodeFileSaver.saveMultiFileCodeResult(multiFileResult);
//                        log.info("保存成功，路径为：" + savedDir.getAbsolutePath());
//                    } catch (Exception e) {
//                        log.error("保存失败: {}", e.getMessage());
//                    }
//                });
//    }

//    /**
//     * 生成 HTML 模式的代码并保存
//     *
//     * @param userMessage 用户提示词
//     * @return 保存的目录
//     */
//    private File generateAndSaveHtmlCode(String userMessage) {
//        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
//        return CodeFileSaver.saveHtmlCodeResult(result);
//    }
//
//    /**
//     * 生成多文件模式的代码并保存
//     *
//     * @param userMessage 用户提示词
//     * @return 保存的目录
//     */
//    private File generateAndSaveMultiFileCode(String userMessage) {
//        MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
//        return CodeFileSaver.saveMultiFileCodeResult(result);
//    }
}
