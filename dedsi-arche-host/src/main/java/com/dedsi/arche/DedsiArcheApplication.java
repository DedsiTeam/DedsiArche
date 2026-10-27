package com.dedsi.arche;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 单一启动入口；业务模块均位于当前根包之下。
 */
@SpringBootApplication
public class DedsiArcheApplication {

    /**
     * 启动应用并加载业务模块与基础设施配置。
     */
    public static void main(String[] args) {
        SpringApplication.run(DedsiArcheApplication.class, args);
    }
}
