package com.yupi.usercenter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

/**
 * @author: shayu
 * @date: 2022/11/20
 * @ClassName: yupao-backend01
 * @Description: 自定义 Swagger 接口文档的配置
 */
@Configuration
@EnableSwagger2WebMvc
@Profile({"dev", "test"})   //版本控制访问
public class SwaggerConfig {

    @Bean(value = "defaultApi2")
    public Docket defaultApi2() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                // 这里一定要标注你控制器的位置
                .apis(RequestHandlerSelectors.basePackage("com.yupi.usercenter.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    /**
     * api 信息
     * @return
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("项目接口中心")
                .description("zcnovice用户中心接口文档")
                .termsOfServiceUrl("https://github.com/zcnovice")
                .contact(new Contact("shayu","https://shayuyu.cn/","shayu-yusha@qq.com"))
                .version("1.0")
                .build();
    }
}