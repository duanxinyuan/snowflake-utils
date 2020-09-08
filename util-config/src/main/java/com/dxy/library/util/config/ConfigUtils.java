package com.dxy.library.util.config;


import com.dxy.library.util.config.dto.Config;
import com.dxy.library.util.config.exception.ConfigException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.dxy.library.json.jackson.JacksonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.XMLPropertiesConfiguration;
import org.apache.commons.configuration2.YAMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.tree.DefaultExpressionEngineSymbols;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 读取配置文件的工具类
 * @author duanxinyuan
 * 2018/8/6 12:44
 */
@Slf4j
public class ConfigUtils {

    private static final Map<String, Object> PROPERTIES = Maps.newLinkedHashMap();

    static {
        load("application.properties");
        load("application.yml");
        load("application.yaml");

        //需要额外加载的配置文件，多个以逗号隔开
        String configFiles = getAsString("config.files");
        if (StringUtils.isNotEmpty(configFiles)) {
            String[] strings = configFiles.split(",");
            for (String string : strings) {
                load(string);
            }
        }
    }

    public static int getAsInt(String key) {
        return NumberUtils.toInt(getAsString(key));
    }

    public static int getAsInt(String key, int defaultValue) {
        return NumberUtils.toInt(getAsString(key), defaultValue);
    }

    public static long getAsLong(String key) {
        return NumberUtils.toLong(getAsString(key));
    }

    public static long getAsLong(String key, long defaultValue) {
        return NumberUtils.toLong(getAsString(key), defaultValue);
    }

    public static double getAsDouble(String key) {
        return NumberUtils.toDouble(getAsString(key));
    }

    public static double getAsDouble(String key, double defaultValue) {
        return NumberUtils.toDouble(getAsString(key), defaultValue);
    }

    public static boolean getAsBoolean(String key) {
        return BooleanUtils.toBoolean(getAsString(key));
    }

    public static boolean getAsBoolean(String key, boolean defaultValue) {
        String s = getAsString(key);
        return StringUtils.isEmpty(s) ? defaultValue : BooleanUtils.toBoolean(s);
    }

    /**
     * 获取配置
     * @param key 配置名称
     */
    public static String getAsString(String key) {
        return getAsString(key, null);
    }

    /**
     * 获取配置
     * @param key 配置名称
     */
    public static String getAsString(String key, String defaultValue) {
        Object object = PROPERTIES.get(Objects.requireNonNull(key));
        if (ObjectUtils.isEmpty(object)) {
            object = System.getProperty(key);
        }
        if (object == null) {
            return defaultValue;
        } else {
            if (object instanceof String) {
                return (String) object;
            } else {
                return String.valueOf(object);
            }
        }
    }

    /**
     * 获取配置
     * @param key 配置名称
     */
    public static <T> T get(String key, Class<T> type) {
        return get(key, type, null);
    }

    /**
     * 获取配置
     * @param key 配置名称
     */
    public static <T> T get(String key, Class<T> type, T defaultValue) {
        String value = getAsString(key);
        if (StringUtils.isNotEmpty(value)) {
            return JacksonUtil.from(value, type);
        } else {
            return defaultValue;
        }
    }

    /**
     * 获取配置（以Key为前缀，获取所有符合规则的config）
     * @param key 配置名称前缀
     */
    public static Config<String> getConfig(String key, String name) {
        return getConfig(key, name, String.class);
    }

    /**
     * 获取配置（以Key为前缀，获取所有符合规则的config）
     * @param key 配置名称前缀
     */
    public static <T> Config<T> getConfig(String key, String name, Class<T> type) {
        List<Config<T>> configs = getConfigs(key, type);
        Optional<Config<T>> configOptional = configs.stream().filter(config -> config.getName().equals(name)).findFirst();
        return configOptional.orElse(null);
    }

    /**
     * 获取配置（以Key为前缀，获取所有符合规则的config）
     * @param key 配置名称前缀
     */
    public static List<Config<String>> getConfigs(String key) {
        return getConfigs(key, String.class);
    }

    /**
     * 获取配置（以Key为前缀，获取所有符合规则的config）
     * @param key 配置名称前缀
     * @return 所有符合以Key为前缀规则的config
     */
    public static <T> List<Config<T>> getConfigs(String key, Class<T> type) {
        key = Objects.requireNonNull(key);
        List<Config<T>> configs = Lists.newArrayList();
        for (Map.Entry<String, Object> entry : PROPERTIES.entrySet()) {
            String entryKey = entry.getKey();
            Object entryValue = entry.getValue();

            String name = null;
            if (entryKey.equals(key)) {
                name = Config.DEFAULT_NAME;
            } else if (entryKey.startsWith(key + ".")) {
                name = entry.getKey().replace(key + ".", "");
            }
            if (StringUtils.isNotEmpty(name)) {
                T value;
                if (entryValue == null) {
                    value = null;
                } else {
                    if (type == String.class) {
                        if (entryValue instanceof String) {
                            value = (T) entryValue;
                        } else {
                            value = (T) String.valueOf(entryValue);
                        }
                    } else {
                        value = JacksonUtil.from(String.valueOf(entryValue), type);
                    }
                }
                configs.add(new Config<>(entryKey, name, value));
            }
        }
        return configs;
    }

    /**
     * 获取配置（以Key为前缀，获取所有符合规则的config，转化为对象）
     * @param key module
     * @param <T> 配置对象类型
     */
    public static <T> T getConfigAsObject(String key, Class<T> type) {
        Properties props = getConfigAsProperties(key);
        Set<String> propertyNames = props.keySet().stream().filter(Objects::nonNull).map(Object::toString).collect(Collectors.toSet());
        Map<String, Object> map = Maps.newHashMap();
        propertyNames.forEach(propertyName -> map.put(propertyName, props.get(propertyName)));
        return JacksonUtil.from(JacksonUtil.to(map), type);
    }

    /**
     * 获取某个前缀的 所有配置信息， key包含前缀
     */
    public static Properties getAsProperties(String key) {
        Properties result = new Properties();
        String prefix = key + ".";
        PROPERTIES.forEach((k, v) -> {
            if (k.startsWith(prefix)) {
                result.put(k, v);
            }
        });
        return result;
    }

    /**
     * 获取某个前缀的 所有配置信息， key不包含前缀
     */
    public static Properties getConfigAsProperties(String key) {
        Properties result = new Properties();
        String prefix = key + ".";
        PROPERTIES.forEach((k, v) -> {
            if (k.startsWith(prefix)) {
                result.put(k.replace(prefix, ""), v);
            }
        });
        return result;
    }

    /**
     * 获取所有配置
     */
    public static Properties getAllConfigAsProperties() {
        Properties result = new Properties();
        PROPERTIES.forEach(result::put);
        return result;
    }

    /**
     * 获取所有配置
     */
    public static Map<String, Object> getAllConfigAsMap() {
        return PROPERTIES;
    }

    /**
     * 加载配置文件
     * @param name 配置文件名称（支持properties、yaml、yml、xml四种格式的文件）
     */
    public static Properties loadAsProperties(String name) {
        Map<String, Object> hashMap = loadAsMap(name);
        Properties result = new Properties();
        hashMap.forEach(result::put);
        return result;
    }

    /**
     * 加载配置文件到内存中
     * @param name 配置文件名称（支持properties、yaml、yml、xml四种格式的文件）
     */
    public static void load(String name) {
        Map<String, Object> hashMap = loadAsMap(name);
        if (!hashMap.isEmpty()) {
            PROPERTIES.putAll(hashMap);
        }
    }

    /**
     * 加载配置文件
     * @param name 配置文件名称（支持properties、yaml、yml、xml四种格式的文件）
     */
    public static Map<String, Object> loadAsMap(String name) {
        name = name.trim();
        if (!name.endsWith("properties") && !name.endsWith("yaml") && !name.endsWith("yml") && !name.endsWith("xml")) {
            throw new ConfigException("unsupported file format");
        }

        Map<String, Object> hashMap = Maps.newLinkedHashMap();
        try {
            // 设置编码，默认采用的是`ISO-8859-1`，会出现中文乱码
            FileBasedConfigurationBuilder.setDefaultEncoding(PropertiesConfiguration.class, StandardCharsets.UTF_8.name());
            FileBasedConfigurationBuilder.setDefaultEncoding(XMLPropertiesConfiguration.class, StandardCharsets.UTF_8.name());
            FileBasedConfigurationBuilder.setDefaultEncoding(YAMLConfiguration.class, StandardCharsets.UTF_8.name());

            if (name.endsWith("properties")) {
                URL resource = getResource(name);
                if (resource != null) {
                    File file = new File(resource.getPath());
                    if (file.exists()) {
                        PropertiesConfiguration properties = new Configurations().properties(file);
                        Iterator<String> keys = properties.getKeys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            Object property = properties.getProperty(key);
                            hashMap.put(key, property);
                        }
                    }
                }
            } else if (name.endsWith("yaml") || name.endsWith("yml")) {
                InputStream resourceStream = getResourceStream(name);
                if (null != resourceStream) {
                    YAMLConfiguration config = new YAMLConfiguration();
                    config.read(resourceStream);
                    Iterator<String> keys = config.getKeys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        Object property = config.getProperty(key);
                        if (key.contains(DefaultExpressionEngineSymbols.DEFAULT_ESCAPED_DELIMITER)) {
                            //yml解析带小数点的Key，会将key值中的小数点替换成两个小数点，这里替换回去
                            key = key.replace(DefaultExpressionEngineSymbols.DEFAULT_ESCAPED_DELIMITER,
                                    DefaultExpressionEngineSymbols.DEFAULT_PROPERTY_DELIMITER);
                        }
                        hashMap.put(key, property);
                    }
                }
            } else if (name.endsWith("xml")) {
                URL resource = getResource(name);
                if (resource != null) {
                    File file = new File(resource.getPath());
                    if (file.exists()) {
                        XMLConfiguration configuration = new Configurations().xml(file);
                        Iterator<String> keys = configuration.getKeys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            Object property = configuration.getProperty(key);
                            hashMap.put(key, property);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("config load error, file: {}", name, e);
        }
        return hashMap;
    }

    private static URL getResource(String name) {
        return ConfigUtils.class.getClassLoader().getResource(name);
    }

    private static InputStream getResourceStream(String name) {
        return ConfigUtils.class.getClassLoader().getResourceAsStream(name);
    }

}

