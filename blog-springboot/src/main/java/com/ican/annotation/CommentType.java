package com.ican.annotation;

import com.ican.validator.CommentTypeValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * 评论类型注解
 *
 * @author xcs
 **/
@Documented
@Constraint(validatedBy = {CommentTypeValidator.class})
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommentType {

    String message() default "{jakarta.validation.constraints.NotBlank.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * @return 评论类型
     */
    int[] values() default {};

}