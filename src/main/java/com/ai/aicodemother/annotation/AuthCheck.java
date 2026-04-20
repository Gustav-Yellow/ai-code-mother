package com.ai.aicodemother.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthCheck {

    /**
     * 必须要角色
     * @return 角色
     */
    String mustRole() default "";
}
