package com.ai.aicodemother.utils;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;

/**
 * 通用缓存 Key 生成工具类
 * 将请求对象先变成 JSON 格式的数据，然后再将 Json 转换成字符串，之后再用字符串求 MD5 作为 cacheKey
 */
public class CacheKeyUtils {

    /**
     * 根据对象生成缓存 key（JSON + MD5）
     *
     * @param object 要生成 key 的对象
     * @return 缓存 key
     */
    public static String getCacheKey(Object object) {
        if (object == null) {
            // 防止用户一直查询一个空字符串的缓存，避免缓存击穿
            return DigestUtil.md5Hex("null");
        }
        // 先转 Json，再转 MD5
        String jsonStr = JSONUtil.toJsonStr(object);
        return DigestUtil.md5Hex(jsonStr);
    }

}
