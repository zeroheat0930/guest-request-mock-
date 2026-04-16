package com.daol.concierge.core.parameter;

import com.daol.concierge.core.api.ApiException;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

public class RequestParams {

    private Map<String, Object> map;
    private Map<String, MultipartFile> fileMap;

    public RequestParams() {
        this.map = new HashMap<>();
    }

    public Map<String, Object> getParams() {
        Map<String, Object> param = new HashMap<>(map);
        if (fileMap != null) param.putAll(fileMap);
        return param;
    }

    public void put(String key, Object value) {
        this.map.put(key, value);
    }

    public void putAll(Map<String, Object> m) {
        this.map.putAll(m);
    }

    public void setParameterMap(Map<String, String[]> paramMap) {
        for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
            String[] values = entry.getValue();
            if (values.length == 1) {
                String v = values[0];
                put(entry.getKey(), (v == null || v.isEmpty()) ? null : v);
            } else {
                put(entry.getKey(), String.join(",", values));
            }
        }
    }

    public String getString(String key, String defaultValue) {
        if (map.containsKey(key)) {
            Object v = map.get(key);
            if (v == null) return defaultValue;
            String s = v.toString();
            return s.isEmpty() ? defaultValue : s;
        }
        return defaultValue;
    }

    public String getString(String key) { return getString(key, null); }

    public int getInt(String key, int defaultValue) {
        String v = getString(key);
        return (v == null || v.isEmpty()) ? defaultValue : Integer.parseInt(v);
    }

    public int getInt(String key) { return getInt(key, 0); }

    public long getLong(String key, long defaultValue) {
        String v = getString(key);
        return (v == null || v.isEmpty()) ? defaultValue : Long.parseLong(v);
    }

    public long getLong(String key) { return getLong(key, 0); }

    public boolean getBoolean(String key, boolean defaultValue) {
        String v = getString(key);
        return (v == null || v.isEmpty()) ? defaultValue : Boolean.parseBoolean(v);
    }

    public boolean getBoolean(String key) { return getBoolean(key, false); }

    public Object getObject(String key) {
        return map.getOrDefault(key, null);
    }

    public String getNotEmptyString(String key) {
        Object obj = map.get(key);
        if (obj == null || obj.toString().isEmpty()) {
            throw new ApiException(key + " is not present");
        }
        return obj.toString();
    }

    public Integer getNotEmptyInt(String key) {
        return Integer.parseInt(getNotEmptyString(key));
    }

    public Long getNotEmptyLong(String key) {
        return Long.parseLong(getNotEmptyString(key));
    }

    public void addFile(String fileName, MultipartFile file) {
        if (fileMap == null) fileMap = new HashMap<>();
        fileMap.put(fileName, file);
    }

    public Map<String, MultipartFile> getFileMap() { return fileMap; }

    @Override
    public String toString() {
        return "\nmap : " + map + "\nfileMap : " + fileMap;
    }
}
