package com.joto.lab.es.test;

import cn.hutool.core.util.RandomUtil;
import com.joto.lab.es.core.utils.EncryptUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.security.InvalidKeyException;

/**
 * @authro joey
 * @date 2024/8/28 14:31
 */
@Slf4j
public class CryptoTest {

    @Test
    public void encryptEs() throws InvalidKeyException {
        String user = "elastic";
        String pwd = "Elastic#123";
        String secret = RandomUtil.randomString(16);
        String iv = RandomUtil.randomString(16);

        log.info("user: {}", EncryptUtil.encrypt4(user, secret, iv));
        log.info("pwd: {}", EncryptUtil.encrypt4(pwd, secret, iv));
        log.info("secret: {}", secret);
        log.info("iv: {}", iv);
    }
}
