package com.joto.lab.es.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author joey
 * @date 2024/8/26 15:19
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EsIndex {
    /**
     * @return 索引名称
     */
    String name();
}
