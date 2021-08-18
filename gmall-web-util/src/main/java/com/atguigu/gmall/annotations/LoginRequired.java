package com.atguigu.gmall.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)  // 生效范围  方法或者类中
@Retention(RetentionPolicy.RUNTIME)  // 使用范围    jvm中还是coding中
public @interface LoginRequired {

    boolean loginSuccess() default  true; // 如果是false的话,不管是否登录成功都是可以使用该方法的，只是不同的情况而已。 // 需要登录的方法分支成必须登录还有可以不登录的情况，这样就有三类的方法了。


}
