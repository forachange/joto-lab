package com.joto.lab.es.core.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.*;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.symmetric.SM4;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.util.List;


/**
 * 加解密工具类
 *
 * @author YYJ
 * @date 2023/7/6 下午2:52
 */
public final class EncryptUtil {

    private static final String SM_4_SECRET_CAN_NOT_EMPTY = "sm4 secret 不能为空";

    private EncryptUtil(){}

    public static final String SPLIT_STR = "@";

    /**
     * 国密二加密
     *
     * @param plain     明文
     * @param publicKey 公钥
     * @return 密文
     * @throws InvalidKeyException 公钥异常
     */
    public static String encrypt2(String plain, String publicKey) throws InvalidKeyException {
        if (StrUtil.isEmpty(plain)) {
            return plain;
        }
        if (StrUtil.isEmpty(publicKey)) {
            throw new InvalidKeyException("sm2 public key 不能为空");
        }
        return SmUtil.sm2(null, publicKey).encryptBase64(plain, KeyType.PublicKey);
    }

    /**
     * Sm2解密
     *
     * @param ciphertext  密文
     * @param privateKey 私钥
     * @return 明文
     */
    public static String decrypt2(String ciphertext, String privateKey) throws InvalidKeyException {
        if (StrUtil.isEmpty(ciphertext)) {
            return ciphertext;
        }
        if (StrUtil.isEmpty(privateKey)) {
            throw new InvalidKeyException("sm2 private key 不能为空");
        }
        // 公钥加密，私钥解密
        return SmUtil.sm2(privateKey, null).decryptStr(ciphertext, KeyType.PrivateKey);
    }


    /**
     * 国密三摘要/签名
     *
     * @param source 摘要源数据
     * @return 摘要结果值
     */
    public static String encrypt3(String source) {
        return SmUtil.sm3(source);
    }

    /**
     * 国密三摘要/签名
     *
     * @param source 摘要源数据
     * @param salt   盐
     * @return 摘要结果值
     */
    public static String encrypt3(String source, String salt) {
        if (StrUtil.isEmpty(salt)) {
            return encrypt3(source);
        }
        return SmUtil.sm3WithSalt(salt.getBytes(StandardCharsets.UTF_8)).digestHex(source);
    }

    /**
     * 国密四解密
     *
     * @param ciphertext 密文
     * @param secret ECB模式秘钥，CBC模式秘钥对（CBC秘钥和CBC向量用@分割）
     * @return 明文
     * @throws InvalidKeyException 秘钥异常
     */
    public static byte[] decrypt4ToBytes(String ciphertext, String secret) throws InvalidKeyException {
        if (StrUtil.isEmpty(ciphertext)) {
            return new byte[0];
        }
        if (StrUtil.isEmpty(secret)) {
            throw new InvalidKeyException(SM_4_SECRET_CAN_NOT_EMPTY);
        }
        if (StrUtil.contains(secret, SPLIT_STR)) {
            List<String> list = StrUtil.splitTrim(secret, SPLIT_STR);
            return decrypt4ToBytes(ciphertext, CollUtil.get(list, 0), CollUtil.get(list, 1));
        }
        return SmUtil.sm4(getKeyBytes(secret)).decrypt(ciphertext);
    }

    public static byte[] decrypt4ToBytes(String ciphertext, String key, String iv) {
        // 4.解得原文
        byte[] keyBytes = getKeyBytes(key);
        byte[] ivBytes = getKeyBytes(iv);
        SM4 sm4 = new SM4(Mode.CBC, Padding.PKCS5Padding, keyBytes, ivBytes);

        return sm4.decrypt(ciphertext);
    }

    /**
     * 国密四解密
     *
     * @param ciphertext 密文
     * @param secret ECB模式秘钥，CBC模式秘钥对（CBC秘钥和CBC向量用@分割）
     * @return 明文
     * @throws InvalidKeyException 秘钥异常
     */
    public static String decrypt4(String ciphertext, String secret) throws InvalidKeyException {
        if (StrUtil.isEmpty(ciphertext)) {
            return ciphertext;
        }
        if (StrUtil.isEmpty(secret)) {
            throw new InvalidKeyException(SM_4_SECRET_CAN_NOT_EMPTY);
        }
        if (StrUtil.contains(secret, SPLIT_STR)) {
            List<String> list = StrUtil.splitTrim(secret, SPLIT_STR);
            return decrypt4(ciphertext, CollUtil.get(list, 0), CollUtil.get(list, 1));
        }
        return SmUtil.sm4(getKeyBytes(secret)).decryptStr(ciphertext);
    }

    public static String decrypt4(String ciphertext, String key, String iv) {
        // 4.解得原文
        byte[] keyBytes = getKeyBytes(key);
        byte[] ivBytes = getKeyBytes(iv);
        SM4 sm4 = new SM4(Mode.CBC, Padding.PKCS5Padding, keyBytes, ivBytes);

        return sm4.decryptStr(ciphertext);
    }
    /**
     * 国密四加密
     *
     * @param plain  明文
     * @param secret ECB模式秘钥，CBC模式秘钥对（CBC秘钥和CBC向量用@分割）
     * @return 密文
     * @throws InvalidKeyException 秘钥异常
     */
    public static String encrypt4(String plain, String secret) throws InvalidKeyException {
        if (StrUtil.isEmpty(plain)) {
            return plain;
        }
        if (StrUtil.isEmpty(secret)) {
            throw new InvalidKeyException(SM_4_SECRET_CAN_NOT_EMPTY);
        }
        if (StrUtil.contains(secret, SPLIT_STR)) {
            List<String> list = StrUtil.splitTrim(secret, SPLIT_STR);
            return encrypt4(plain, CollUtil.get(list, 0), CollUtil.get(list, 1));
        }
        return SmUtil.sm4(getKeyBytes(secret)).encryptBase64(plain);
    }

    /**
     * 国密四加密
     *
     * @param plain  明文
     * @param secret CBC秘钥
     * @param iv     CBC向量
     * @return 密文
     * @throws InvalidKeyException 秘钥异常
     */
    public static String encrypt4(String plain, String secret, String iv) throws InvalidKeyException {
        if (StrUtil.isEmpty(plain)) {
            return plain;
        }
        if (StrUtil.isEmpty(secret)) {
            throw new InvalidKeyException(SM_4_SECRET_CAN_NOT_EMPTY);
        }
        if (StrUtil.isEmpty(iv)) {
            throw new InvalidKeyException("sm4 iv 不能为空");
        }
        final SM4 sm4 = new SM4(Mode.CBC, Padding.PKCS5Padding, getKeyBytes(secret), getKeyBytes(iv));
        return sm4.encryptBase64(plain);
    }

    /**
     * 将秘钥字符串转为字节数组
     *
     * @param key 秘钥字符串
     * @return 字节数组
     */
    public static byte[] getKeyBytes(String key) {
        return key.length() == 16 ? key.getBytes(StandardCharsets.UTF_8) : HexUtil.decodeHex(key);
    }


    /**
     * 国密二秘钥对生成
     *
     * @return 国密二秘钥对，格式：[公钥@私钥]
     */
    public static String generateKeyPair2() {
        KeyPair keyPair = KeyUtil.generateKeyPair("SM2", 1024);
        String publicKey = HexUtil.encodeHexStr(BCUtil.encodeECPublicKey(keyPair.getPublic(), false));
        String privateKey = HexUtil.encodeHexStr(BCUtil.encodeECPrivateKey(keyPair.getPrivate()));
        return publicKey + SPLIT_STR + privateKey;
    }

    public static String generateKey4() {
        String sm4Key = RandomUtil.randomString(16);
        String sm4Iv = RandomUtil.randomString(16);
        return sm4Key + SPLIT_STR + sm4Iv;
    }

    /**
     * 根据 SM2 私钥获取公钥
     * @param priKey
     * @return
     */
    public static String getSm2PublicKeyByPrivateKey(String priKey) {
        final byte[] decode = SecureUtil.decode(priKey);

        final ECPrivateKeyParameters ecPrivateKeyParameters = ECKeyUtil.decodePrivateKeyParams(decode);

        final ECPublicKeyParameters publicParams = ECKeyUtil.getPublicParams(ecPrivateKeyParameters);

        return HexUtil.encodeHexStr(publicParams.getQ().getEncoded(false));
    }
}
