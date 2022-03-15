package com.honzel.core.util.web;

import com.honzel.core.constant.ArrayConstants;
import com.honzel.core.constant.NumberConstants;
import com.honzel.core.util.text.TextUtils;

/**
 * 文件类型
 * @author luhz@trendit.cn
 * @date 2022/3/15
 */
public enum FileTypeEnum {
    /**
     * 其他文件类型:
     */
    OTHER("application/octet-stream", TextUtils.EMPTY) {boolean matchHeader(byte[] fileBytes) {return false;}},
    /**
     * 文件类型: gif
     */
    IMAGE_GIF("image/gif", "gif") {boolean matchHeader(byte[] fileBytes) {return fileBytes[0] == 'G' && fileBytes[1] == 'I' && fileBytes[2] == 'F';}},
    /**
     * 文件类型: jpg
     */
    IMAGE_JPEG("image/jpeg", "jpg", "jpeg") {boolean matchHeader(byte[] fileBytes) {return fileBytes[6] == 'J' && fileBytes[7] == 'F' && fileBytes[8] == 'I' && fileBytes[9] == 'F';}},
    /**
     * 文件类型: png
     */
    IMAGE_PNG("image/png", "png") {boolean matchHeader(byte[] fileBytes) {return fileBytes[1] == 'P' && fileBytes[2] == 'N' && fileBytes[3] == 'G';}},
    /**
     * 文件类型: wav
     */
    AUDIO_WAV("audio/x-wav", "wav", 44) {boolean matchHeader(byte[] fileBytes) {return fileBytes[8] == 'W' && fileBytes[9] == 'A' && fileBytes[10] == 'V' && fileBytes[11] == 'E' && fileBytes[0] == 'R' && fileBytes[1] == 'I' && fileBytes[2] == 'F' && fileBytes[3] == 'F';}},
    /**
     * 文件类型: bmp
     */
    IMAGE_BMP("image/bmp", "bmp") {boolean matchHeader(byte[] fileBytes) {return fileBytes[0] == 'B' && fileBytes[1] == 'M';}},
//    /**
//     * 文件类型: 3gp
//     */
//    VIDEO_3GP("video/3gpp", "3gp") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: apk
//     */
//    APPLICATION_APK("application/vnd.android.package-archive", "apk") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: asf
//     */
//    VIDEO_ASF("video/x-ms-asf", "asf") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: avi
//     */
//    VIDEO_AVI("video/x-msvideo", "avi") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: bin
//     */
//    APPLICATION_BIN("application/octet-stream", "bin") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: c
//     */
//    TEXT_C("text/plain", "c") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: class
//     */
//    APPLICATION_CLASS("application/octet-stream", "class") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: conf
//     */
//    TEXT_CONF("text/plain", "conf") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: cpp
//     */
//    TEXT_CPP("text/plain", "cpp") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: doc
//     */
//    APPLICATION_DOC("application/msword", "doc") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: docx
//     */
//    APPLICATION_DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: xls
//     */
//    APPLICATION_XLS("application/vnd.ms-excel", "xls") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: xlsx
//     */
//    APPLICATION_XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: exe
//     */
//    APPLICATION_EXE("application/octet-stream", "exe") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: gtar
//     */
//    APPLICATION_GTAR("application/x-gtar", "gtar") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: gz
//     */
//    APPLICATION_GZ("application/x-gzip", "gz") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: h
//     */
//    TEXT_H("text/plain", "h") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: htm
//     */
//    TEXT_HTM("text/html", "htm") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: html
//     */
//    TEXT_HTML("text/html", "html") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: jar
//     */
//    APPLICATION_JAR("application/java-archive", "jar") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: java
//     */
//    TEXT_JAVA("text/plain", "java") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: js
//     */
//    APPLICATION_JS("application/x-javascript", "js") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: log
//     */
//    TEXT_LOG("text/plain", "log") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: m3u
//     */
//    AUDIO_M3U("audio/x-mpegurl", "m3u") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: m4a
//     */
//    AUDIO_M4A("audio/mp4a-latm", "m4a") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: m4b
//     */
//    AUDIO_M4B("audio/mp4a-latm", "m4b") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: m4p
//     */
//    AUDIO_M4P("audio/mp4a-latm", "m4p") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: m4u
//     */
//    VIDEO_M4U("video/vnd.mpegurl", "m4u") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: m4v
//     */
//    VIDEO_M4V("video/x-m4v", "m4v") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: mov
//     */
//    VIDEO_MOV("video/quicktime", "mov") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: mp2
//     */
//    AUDIO_MP2("audio/x-mpeg", "mp2") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: mp3
//     */
//    AUDIO_MP3("audio/x-mpeg", "mp3") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: mp4
//     */
//    VIDEO_MP4("video/mp4", "mp4") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: mpc
//     */
//    APPLICATION_MPC("application/vnd.mpohun.certificate", "mpc") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: mpe
//     */
//    VIDEO_MPE("video/mpeg", "mpe") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: mpeg
//     */
//    VIDEO_MPEG("video/mpeg", "mpeg") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: mpg
//     */
//    VIDEO_MPG("video/mpeg", "mpg") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: mpg4
//     */
//    VIDEO_MPG4("video/mp4", "mpg4") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: mpga
//     */
//    AUDIO_MPGA("audio/mpeg", "mpga") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: msg
//     */
//    APPLICATION_MSG("application/vnd.ms-outlook", "msg") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: ogg
//     */
//    AUDIO_OGG("audio/ogg", "ogg") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: pdf
//     */
//    APPLICATION_PDF("application/pdf", "pdf") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: pps
//     */
//    APPLICATION_PPS("application/vnd.ms-powerpoint", "pps") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: ppt
//     */
//    APPLICATION_PPT("application/vnd.ms-powerpoint", "ppt") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: pptx
//     */
//    APPLICATION_PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: prop
//     */
//    TEXT_PROP("text/plain", "prop") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: rc
//     */
//    TEXT_RC("text/plain", "rc") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: rmvb
//     */
//    AUDIO_RMVB("audio/x-pn-realaudio", "rmvb") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: rtf
//     */
//    APPLICATION_RTF("application/rtf", "rtf") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: sh
//     */
//    TEXT_SH("text/plain", "sh") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: tar
//     */
//    APPLICATION_TAR("application/x-tar", "tar") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: tgz
//     */
//    APPLICATION_TGZ("application/x-compressed", "tgz") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: txt
//     */
//    TEXT_TXT("text/plain", "txt") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: wma
//     */
//    AUDIO_WMA("audio/x-ms-wma", "wma") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: wmv
//     */
//    AUDIO_WMV("audio/x-ms-wmv", "wmv") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: wps
//     */
//    APPLICATION_WPS("application/vnd.ms-works", "wps") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: xml
//     */
//    TEXT_XML("text/plain", "xml") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: z
//     */
//    APPLICATION_Z("application/x-compress", "z") {boolean matchHeader(byte[] fileBytes) {return false;}},
//    /**
//     * 文件类型: zip
//     */
//    APPLICATION_ZIP("application/x-zip-compressed", "zip") {boolean matchHeader(byte[] fileBytes) {return false;}},
//
    //------------------------------- end --------------------------------------
    ;

    /**
     * 后缀名
     */
    private final String suffix;
    /**
     * 后缀名
     */
    private final String[] otherSuffixes;
    /**
     * 类型
     */
    private final String mineType;
    /**
     * 最小长度
     */
    private final int minLength;

    private static final FileTypeEnum[] VALUES = values();

    public String getSuffix() {
        return suffix;
    }

    public String getMineType() {
        return mineType;
    }

    /**
     * 后缀数量
     * @return 返回后缀总数量
     */
    public int getSuffixCount() {
        return otherSuffixes != null ? otherSuffixes.length + 1 : 1;
    }

    /**
     * 获取指定位置的后缀
     * @param index 后缀位置
     * @return
     */
    public String getSuffix(int index) {
        if (index != NumberConstants.INTEGER_ZERO) {
            return index > NumberConstants.INTEGER_ZERO && index <= otherSuffixes.length ? otherSuffixes[index - NumberConstants.INTEGER_ONE] : null;
        }
        return suffix;
    }



    FileTypeEnum(String mineType, String suffix, int minLength) {
        this(mineType, suffix, minLength, ArrayConstants.EMPTY_STRING_ARRAY);
    }

    FileTypeEnum(String mineType, String suffix) {
        this(mineType, suffix, NumberConstants.INTEGER_TEN, ArrayConstants.EMPTY_STRING_ARRAY);
    }

    FileTypeEnum(String mineType, String suffix, String... otherSuffixes) {
        this(mineType, suffix, NumberConstants.INTEGER_TEN, otherSuffixes);
    }

    FileTypeEnum(String mineType, String suffix, int minLength, String... otherSuffixes) {
        this.suffix = suffix;
        this.mineType = mineType;
        this.minLength = minLength;
        this.otherSuffixes = otherSuffixes;
    }

    /**
     * 获取文件类型
     * @param fileBytes 文件内容
     * @return
     */
    public static FileTypeEnum getFileType(byte[] fileBytes) {
        if (fileBytes != null) {
            for (FileTypeEnum value : VALUES) {
                if (value != OTHER && fileBytes.length >= value.minLength && value.matchHeader(fileBytes)) {
                    return value;
                }
            }
        }
        return OTHER;
    }

    abstract boolean matchHeader(byte[] fileBytes);
}
