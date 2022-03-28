package com.honzel.core.util.web;

import java.io.*;

/**
 *  FileItem.java
 * @author honzel
 * @date 2021/1/26
 */
public class FileItem {

    private String fileName;
    private String mimeType;
    private byte[] content;
    private File file;

    /**
     * 基于本地文件的构造器。
     *
     * @param file 本地文件
     */
    public FileItem(File file) {
        this.file = file;
    }

    /**
     * 基于文件绝对路径的构造器。
     *
     * @param filePath 文件绝对路径
     */
    public FileItem(String filePath) {
        this(new File(filePath));
    }

    /**
     * 基于文件名和字节流的构造器。
     *
     * @param fileName 文件名
     * @param content 文件字节流
     */
    public FileItem(String fileName, byte[] content) {
        this.fileName = fileName;
        this.content = content;
    }

    /**
     * 基于文件名、字节流和媒体类型的构造器。
     *
     * @param fileName 文件名
     * @param content 文件字节流
     * @param mimeType 媒体类型
     */
    public FileItem(String fileName, byte[] content, String mimeType) {
        this(fileName, content);
        this.mimeType = mimeType;
    }

    public String getFileName() {
        if (this.fileName == null && this.file != null && this.file.exists()) {
            this.fileName = file.getName();
        }
        return this.fileName;
    }

    public String getMimeType() throws IOException {
        if (this.mimeType == null) {
            this.mimeType = FileTypeEnum.getFileType(getContent()).getMineType();
        }
        return this.mimeType;
    }

    public byte[] getContent() throws IOException {
        if (this.content == null && this.file != null && this.file.exists()) {
            this.content = WebUtils.readAsOutputStream(new FileInputStream(this.file)).toByteArray();
        }
        return this.content;
    }

}
