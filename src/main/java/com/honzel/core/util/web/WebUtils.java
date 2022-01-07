package com.honzel.core.util.web;

import com.honzel.core.util.bean.BeanHelper;
import com.honzel.core.util.resolver.ResolverUtils;
import com.honzel.core.util.text.TextUtils;
import com.honzel.core.util.resolver.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * WEB Utils for request
 * @author honzel
 *
 */
public class WebUtils {
    private static final Logger LOG = LoggerFactory.getLogger(WebUtils.class);
    public static final Charset     DEFAULT_CHARSET = StandardCharsets.UTF_8;
    public static final String     METHOD_POST     = "POST";
    public static final String     METHOD_GET      = "GET";

    private static HostnameVerifier verifier        = null;

    private static SSLSocketFactory socketFactory   = null;
    private static CookieHandler cookieManager   	= null;

    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 20000;

    private static class DefaultTrustManager implements X509TrustManager {
        private static final X509Certificate[] EMPTY_CERTIFICATES = {};
        public X509Certificate[] getAcceptedIssuers() {
            return EMPTY_CERTIFICATES;
        }
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            if ((chain != null) && (chain.length > 0) && !"UNKNOWN".equals(authType)) {
                LOG.debug("不检验客户端证书: {}", authType);
            }
        }
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            if ((chain != null) && (chain.length > 0) && !"UNKNOWN".equals(authType)) {
                LOG.debug("不检验服务端证书: {}", authType);
            }
        }
    }
    static {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(new KeyManager[0], new TrustManager[] { new DefaultTrustManager() }, new SecureRandom());
            ctx.getClientSessionContext().setSessionTimeout(15);
            ctx.getClientSessionContext().setSessionCacheSize(1000);
            socketFactory = ctx.getSocketFactory();
        } catch (Exception e) {
            LOG.info("initialize SocketFactory fail!");
        }
        verifier = (hostname, session) -> {
            return false;//默认认证不通过，进行证书校验。
        };
    }

    protected WebUtils() {
    }

    /**
     * 是否启用Cookie管理
     * @param enabled 是否启用cookie
     */
    public static void setCookieEnabled(boolean enabled) {
        CookieHandler defaultManager = CookieManager.getDefault();
        if (enabled) {
            if (defaultManager == null) {
                if (cookieManager == null)
                    cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
                CookieManager.setDefault(cookieManager);
            }
        } else {
            if (defaultManager != null && cookieManager == null)
                cookieManager = defaultManager;
            CookieManager.setDefault(null);
        }
    }

    /**
     * 获取Cookie管理器
     * @return
     */
    public static CookieHandler getCookieHandler() {
        return CookieManager.getDefault();
    }



    /**
     * 执行HTTP POST请求。
     *
     * @param url 请求地址
     * @param textParams 请求参数
     * @return 响应字符串
     * @throws IOException 异常
     */
    public static String doPost(String url, Map<String, Object> textParams) throws IOException {
        return doPost(url, textParams, DEFAULT_CHARSET, null);
    }
    /**
     * 执行HTTP POST请求。
     *
     * @param url 请求地址
     * @param textParams 请求参数
     * @param charset 字符集，如UTF-8, GBK, GB2312
     * @param headerMap 请求头属性设置
     * @return 响应字符串
     * @throws IOException 异常
     */
    public static String doPost(String url, Map<String, Object> textParams, Charset charset, Map<String, Object> headerMap) throws IOException {
        return doPost(url, textParams, charset, CONNECT_TIMEOUT, READ_TIMEOUT, headerMap);
    }


    /**
     * 执行HTTP POST请求。
     *
     * @param url 请求地址
     * @param textParams 请求参数对
     * @param charset 字符集，如UTF-8, GBK, GB2312
     * @param connectTimeout 连接超时时间
     * @param readTimeout 读取超时时间
     * @param headerMap 请求头属性设置
     * @return 响应字符串
     * @throws IOException 异常
     */
    public static String doPost(String url, Map<String, Object> textParams, Charset charset, int connectTimeout, int readTimeout, Map<String, Object> headerMap) throws IOException {
        return doPost(url, buildQuery(textParams, charset), charset, connectTimeout, readTimeout, headerMap);
    }

    /**
     * 执行HTTP POST请求。
     *
     * @param url 请求地址
     * @param content 请求内容
     * @param charset 字符集，如UTF-8, GBK, GB2312
     * @param connectTimeout 连接超时时间
     * @param readTimeout 读取超时时间
     * @param headerMap 请求头属性设置
     * @return 响应字符串
     * @throws IOException 异常
     */
    public static String doPost(String url, String content, Charset charset, int connectTimeout, int readTimeout, Map<String, Object> headerMap) throws IOException {
        if (charset == null) {
            charset = DEFAULT_CHARSET;
        }
        String contentType = null;
        if (headerMap == null) {
            contentType = "application/x-www-form-urlencoded;charset=" + charset;
        }
        return doRequest(getConnection(url, METHOD_POST, contentType, connectTimeout, readTimeout, headerMap), content, charset);
    }
    /**
     * 执行HTTP POST请求。
     *
     * @param url 请求地址
     * @param content 请求内容
     * @param headerMap 请求头属性设置
     * @return 响应字符串
     * @throws IOException 异常
     */
    public static String doPost(String url, String content, Map<String, Object> headerMap) throws IOException {
        return doPost(url, content, DEFAULT_CHARSET, CONNECT_TIMEOUT, READ_TIMEOUT, headerMap);
    }
    /**
     * 执行HTTP POST请求。
     *
     * @param url 请求地址
     * @param content 请求内容
     * @return 响应字符串
     * @throws IOException 异常
     */
    public static String doPost(String url, String content) throws IOException {
        return doPost(url, content, null);
    }

    /**
     * 执行HTTP请求。
     * @param conn 请求连接
     * @param content 请求内容
     * @param charset 请求字符集
     * @return 响应字符串
     * @throws IOException 异常
     */
    public static String doRequest(HttpURLConnection conn, String content, Charset charset) throws IOException {
        if (conn == null) {
            throw new IOException("connection is null");
        }
        try {
            return getResponseAsString(getConnectionInputStream(conn, content, charset), getResponseCharset(conn.getContentType(), charset));
        } finally {
            conn.disconnect();
        }
    }

    /**
     * 执行HTTP请求并获取输入流。
     * @param conn 请求连接
     * @param content 请求内容
     * @param charset 请求字符集
     * @return 响应输入流
     * @throws IOException 异常
     */
    private  static InputStream getConnectionInputStream(HttpURLConnection conn, String content, Charset charset) throws IOException {
        if (content != null && content.length() > 0) {
            if (TextUtils.isEmpty(conn.getRequestMethod())) {
                conn.setRequestMethod(METHOD_POST);
            }
            OutputStream out = null;
            try {
                out = conn.getOutputStream();
                out.write(content.getBytes(charset != null ? charset : DEFAULT_CHARSET));
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (Exception e) {
                    LOG.warn("关闭输出流失败");
                }
            }
        } else {
            if (TextUtils.isEmpty(conn.getRequestMethod())) {
                conn.setRequestMethod(METHOD_GET);
            }
        }
        if (conn.getResponseCode() < 400) {
            // 正常返回数据
            return convertInputStream(conn.getInputStream(), conn.getContentEncoding());
        } else {
            // 异常返回数据
            InputStream input = convertInputStream(conn.getErrorStream(), conn.getContentEncoding());
            String msg = getResponseAsString(input, getResponseCharset(conn.getContentType(), charset));
            if (TextUtils.isEmpty(msg)) {
                throw new IOException(conn.getResponseCode() + ":" + conn.getResponseMessage());
            } else {
                throw new IOException(msg);
            }
        }
    }

    private static InputStream convertInputStream(InputStream input, String contentEncoding) {
        try {
            if (contentEncoding != null && contentEncoding.toLowerCase().contains("gzip")) {
                input = new GZIPInputStream(input);
            }
        } catch (Throwable t) {
            LOG.warn("返回指定内容编码指定为[{}], 但进行编码转换时发生异常: {}", contentEncoding, t.getMessage(), t);
        }
        return input;
    }


    private static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception e) {
            LOG.warn("{}流关闭失败", closeable.getClass().getName());
        }
    }
    /**
     * 执行HTTP请求。
     * @param conn 请求连接
     * @param content 请求内容
     * @param charset 请求字符集
     * @return 响应字节数组流
     * @throws IOException 异常
     */
    public static ByteArrayOutputStream doRequestAsStream(HttpURLConnection conn, String content, Charset charset) throws IOException {
        if (conn == null) {
            throw new IOException("connection is null");
        }
        try {
            return getResponseAsStream(getConnectionInputStream(conn, content, charset));
        } finally {
            conn.disconnect();
        }
    }
    /**
     * 执行HTTP请求。
     * @param conn 请求连接
     * @param content 请求内容
     * @return 响应字节数组流
     * @throws IOException 异常
     */
    public static ByteArrayOutputStream doRequestAsStream(HttpURLConnection conn, String content) throws IOException {
        return doRequestAsStream(conn, content, null);
    }
    /**
     * 执行HTTP请求。
     * @param conn 请求连接
     * @param content 请求内容
     * @return 响应字符串
     * @throws IOException 异常
     */
    public static String doRequest(HttpURLConnection conn, String content) throws IOException {
        return doRequest(conn, content, null);
    }

    /**
     * 执行带文件上传的HTTP POST请求。
     *
     * @param url 请求地址
     * @param textParams 文本请求参数
     * @param fileParams 文件请求参数
     * @return 响应字符串
     * @throws IOException 异常
     */
    public static String doPost(String url, Map<String, Object> textParams,  Map<String, FileItem> fileParams) throws IOException {
        return doPost(url, textParams, fileParams, null);
    }
    /**
     * 执行带文件上传的HTTP POST请求。
     *
     * @param url 请求地址
     * @param textParams 文本请求参数
     * @param fileParams 文件请求参数
     * @param headerMap
     * @return 响应字符串
     * @throws IOException 异常
     */
    public static String doPost(String url, Map<String, Object> textParams, Map<String, FileItem> fileParams, Map<String, Object> headerMap) throws IOException {
        if (fileParams == null || fileParams.isEmpty()) {
            return doPost(url, textParams, DEFAULT_CHARSET, CONNECT_TIMEOUT, READ_TIMEOUT, headerMap);
        } else {
            return doPost(url, textParams, fileParams, DEFAULT_CHARSET, CONNECT_TIMEOUT, 0, headerMap);
        }
    }

    /**
     * 执行带文件上传的HTTP POST请求。
     *
     * @param url 请求地址
     * @param textParams 文本请求参数
     * @param fileParams 文件请求参数
     * @param charset 字符集，如UTF-8, GBK, GB2312
     * @param connectTimeout 连接超时时间
     * @param readTimeout 读取超时时间
     * @param headerMap 请求头
     * @return 响应字符串
     * @throws IOException 异常
     */
    public static String doPost(String url, Map<String, Object> textParams, Map<String, FileItem> fileParams, Charset charset, int connectTimeout, int readTimeout, Map<String, Object> headerMap)
            throws IOException {
        if (fileParams == null || fileParams.isEmpty()) {
            return doPost(url, textParams, charset, connectTimeout, readTimeout, headerMap);
        }
        if (charset == null) {
            charset = DEFAULT_CHARSET;
        }
        String boundary = System.currentTimeMillis() + ""; // 随机分隔线
        HttpURLConnection conn = null;
        OutputStream out = null;
        try {
            String cType = "multipart/form-data;boundary=" + boundary + ";charset=" + charset;
            conn = getConnection(url, METHOD_POST, cType, connectTimeout, readTimeout, headerMap);
            out = conn.getOutputStream();
            byte[] entryBoundaryBytes = ("\r\n--" + boundary + "\r\n").getBytes(charset);
            if (textParams != null) { // 组装文本请求参数
                Set<Entry<String, Object>> textEntrySet = textParams.entrySet();
                for (Entry<String, Object> textEntry : textEntrySet) {
                    String value = BeanHelper.convert(textEntry.getValue(), String.class);
                    byte[] textBytes = getTextEntry(textEntry.getKey(), value, charset);
                    out.write(entryBoundaryBytes);
                    out.write(textBytes);
                }
            }
            // 组装文件请求参数
            Set<Entry<String, FileItem>> fileEntrySet = fileParams.entrySet();
            for (Entry<String, FileItem> fileEntry : fileEntrySet) {
                FileItem fileItem = fileEntry.getValue();
                byte[] fileBytes = getFileEntry(fileEntry.getKey(), fileItem.getFileName(), fileItem.getMimeType(), charset);
                out.write(entryBoundaryBytes);
                out.write(fileBytes);
                out.write(fileItem.getContent());
            }
            // 添加请求结束标志
            byte[] endBoundaryBytes = ("\r\n--" + boundary + "--\r\n").getBytes(charset);
            out.write(endBoundaryBytes);
            return getResponseAsString(getConnectionInputStream(conn, null, charset), getResponseCharset(conn.getContentType(), charset));
        } finally {
            closeQuietly(out);
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static byte[] getTextEntry(String fieldName, String fieldValue, Charset charset) {
        String entry = "Content-Disposition:form-data;name=\"" +
                fieldName +
                "\"\r\nContent-Type:text/plain\r\n\r\n" +
                fieldValue;
        return entry.getBytes(charset);
    }

    private static byte[] getFileEntry(String fieldName, String fileName, String mimeType, Charset charset) {
        String entry = "Content-Disposition:form-data;name=\"" +
                fieldName +
                "\";filename=\"" +
                fileName +
                "\"\r\nContent-Type:" +
                mimeType +
                "\r\n\r\n";
        return entry.getBytes(charset);
    }

    /**
     * 执行HTTP GET请求。
     *
     * @param url 请求地址
     * @param textParams 请求参数
     * @return 响应字符串
     * @throws IOException 异常
     */
    public static String doGet(String url, Map<String, Object> textParams) throws IOException {
        return doGet(url, textParams, DEFAULT_CHARSET);
    }
    /**
     * 执行HTTP GET请求。
     *
     * @param url 请求地址
     * @return 响应字符串
     * @throws IOException 异常
     */
    public static String doGet(String url) throws IOException {
        return doGet(url, (String) null);
    }

    /**
     * 执行HTTP GET请求。
     *
     * @param url 请求地址
     * @param textParams 请求参数
     * @param charset 字符集，如UTF-8, GBK, GB2312
     * @return 响应字符串
     * @throws IOException 异常
     */
    public static String doGet(String url, Map<String, Object> textParams, Charset charset) throws IOException {
        if (charset == null) {
            charset = DEFAULT_CHARSET;
        }
        return doGet(url, buildQuery(textParams, charset), charset, CONNECT_TIMEOUT, READ_TIMEOUT, null);
    }

    /**
     * 执行HTTP GET请求。
     *
     * @param url 请求地址
     * @param params 请求参数
     * @param headerMap
     * @return 响应字符串
     * @throws IOException 异常
     */
    public static String doGet(String url, String params, Map<String, Object> headerMap) throws IOException {
        String contentType = "application/x-www-form-urlencoded;charset=" + DEFAULT_CHARSET;
        return doRequest(getConnection(buildGetUrl(url, params), METHOD_GET, contentType, CONNECT_TIMEOUT, READ_TIMEOUT, headerMap), null, DEFAULT_CHARSET);
    }

    /**
     * 执行HTTP GET请求。
     *
     * @param url 请求地址
     * @param params 请求参数
     * @param charset 字符集，如UTF-8, GBK, GB2312
     * @param connectTimeout 连接超时时间
     * @param readTimeout 读取超时时间
     * @param headerMap
     * @return 响应字符串
     * @throws IOException 异常
     */
    public static String doGet(String url, String params, Charset charset, int connectTimeout, int readTimeout, Map<String, Object> headerMap) throws IOException {
        if (charset == null) {
            charset = DEFAULT_CHARSET;
        }
        String contentType = null;
        if (headerMap == null) {
            contentType = "application/x-www-form-urlencoded;charset=" + charset;
        }
        return doRequest(getConnection(buildGetUrl(url, params), METHOD_GET, contentType, connectTimeout, readTimeout, headerMap), null, charset);
    }

    /**
     * 执行HTTP GET请求。
     *
     * @param url 请求地址
     * @param params 请求参数
     * @return 响应字符串
     * @throws IOException 异常
     */
    public static String doGet(String url, String params) throws IOException {
        return doGet(url, params, null);
    }

    /**
     * 获取连接
     * @param url 请求url
     * @param method 请求方法
     * @param contentType Content Type
     * @param connectTimeout 链接超时时间
     * @param readTimeout 读取超时时间
     * @param headerMap 请求头
     * @return 返回连接
     */
    public static HttpURLConnection getConnection(String url, String method, String contentType, int connectTimeout, int readTimeout, Map<String, Object> headerMap) throws IOException {
        HttpURLConnection conn = null;
        try {
            URL endPoint = new URL(url);
            if ("https".equals(endPoint.getProtocol())) {
                HttpsURLConnection connHttps = (HttpsURLConnection) endPoint.openConnection();
                conn = connHttps;
                connHttps.setSSLSocketFactory(socketFactory);
                connHttps.setHostnameVerifier(verifier);
            } else {
                conn = (HttpURLConnection) endPoint.openConnection();
            }
            if (method != null) {
                conn.setRequestMethod(method);
            }
            conn.setDoInput(true);
            conn.setDoOutput(true);
            if (contentType != null) {
                conn.setRequestProperty("Content-Type", contentType);
            }
            if (connectTimeout >= 0) {
                conn.setConnectTimeout(connectTimeout);
            }
            if (readTimeout >= 0) {
                conn.setReadTimeout(readTimeout);
            }
            if (headerMap != null && !headerMap.isEmpty()) {
                for (Entry<String, Object> entry : headerMap.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        conn.setRequestProperty(entry.getKey(), entry.getValue().toString());
                    }
                }
            }
        } catch (IOException e) {
            if (conn != null) {
                conn.disconnect();
            }
            LOG.error("打开http连接时发生错误: {}", e.getMessage());
            throw e;
        }
        return conn;
    }

    /**
     * 获取连接
     * @param url 请求url
     * @param method 请求方法
     * @param contentType Content Type
     * @return 返回连接
     */
    public static HttpURLConnection getConnection(String url, String method, String contentType) throws IOException {
        return getConnection(url, method, contentType, null);
    }

    /**
     * 获取连接
     * @param url 请求url
     * @param method 请求方法
     * @param contentType Content Type
     * @param headerMap 请求头
     * @return 返回连接
     */
    public static HttpURLConnection getConnection(String url, String method, String contentType, Map<String, Object> headerMap) throws IOException {
        return getConnection(url, method, contentType, CONNECT_TIMEOUT, READ_TIMEOUT, headerMap);
    }

    private static String buildGetUrl(String strUrl, String query) {
        if (TextUtils.isEmpty(query)) {
            return strUrl;
        }
        int index = strUrl.indexOf('?');
        if (index > 0 && index < strUrl.length() - 1) {
            if (strUrl.endsWith("&")) {
                strUrl = strUrl + query;
            } else {
                strUrl = strUrl + "&" + query;
            }
        } else {
            if (strUrl.endsWith("?")) {
                strUrl = strUrl + query;
            } else {
                strUrl = strUrl + "?" + query;
            }
        }
        return strUrl;
    }

    /**
     * 组装get参数
     * @param textParams 参数
     * @param charset 字符串集
     * @return 返回组装后字符串
     */
    public static String buildQuery(Map<String, Object> textParams, Charset charset) {
        return buildQuery(textParams, charset, null);
    }

    /**
     * 组装get参数
     * @param textParams 参数
     * @param charset 字符串集
     * @param ignoreKeys 忽略拼入的keys
     * @return 返回组装后字符串
     */
    public static String buildQuery(Map<String, Object> textParams, Charset charset, String ignoreKeys) {
        if (textParams == null || textParams.isEmpty()) {
            return null;
        }
        StringBuilder query = new StringBuilder();
        Set<Entry<String, Object>> entries = textParams.entrySet();
        boolean hasParam = false;
        for (Entry<String, Object> entry : entries) {
            String key = entry.getKey();
            if (ignoreKeys != null && TextUtils.containsValue(ignoreKeys, key)) {
                // 忽略的key
                continue;
            }
            String value = BeanHelper.convert(entry.getValue(), String.class);
            // 忽略参数名或参数值为空的参数
            if (value != null) {
                if (hasParam) {
                    query.append("&");
                } else {
                    hasParam = true;
                }
                query.append(key).append("=").append(encode(value, charset));
            }
        }
        return query.toString();
    }
    /**
     * 组装get参数
     * @param textParams 参数
     * @return 返回组装后字符串
     */
    public static String buildQuery(Map<String, Object> textParams) {
        return buildQuery(textParams, DEFAULT_CHARSET);
    }



    private static ByteArrayOutputStream getResponseAsStream(InputStream input) throws IOException {
        try {
            int available = input.available();
            //
            ByteArrayOutputStream output = available > 0 ? new ByteArrayOutputStream(available) : new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int count;
            while ((count = input.read(buf)) != -1) {
                output.write(buf, 0, count);
            }
            return output;
        } finally {
            closeQuietly(input);
        }
    }

    private static String getResponseAsString(InputStream input, Charset charset) throws IOException {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, charset));
            StringWriter writer = new StringWriter();
            char[] chars = new char[256];
            int count;
            while ((count = reader.read(chars)) != -1) {
                writer.write(chars, 0, count);
            }
            return writer.toString();
        } finally {
            closeQuietly(input);
        }
    }

    private static Charset getResponseCharset(String contentType, Charset defaultCharset) {
        Charset charset = defaultCharset != null ? defaultCharset : DEFAULT_CHARSET;
        if (TextUtils.isEmpty(contentType)) {
            return charset;
        }
        Resolver resolver = ResolverUtils.createResolver(";", "=", true);
        resolver.reset(contentType);
        while (resolver.hasNext()) {
            if (resolver.isInTokens() && resolver.nextEquals("charset") && resolver.hasNext()) {
                String charsetName = resolver.next();
                try {
                    charset = Charset.forName(charsetName);
                } catch (Exception e) {
                    LOG.error("编码" + charsetName + "不支持");
                }
                break;
            }
        }
        return charset;
    }

    /**
     * 使用默认的UTF-8字符集反编码请求参数值。
     *
     * @param value 参数值
     * @return 反编码后的参数值
     */
    public static String decode(String value) {
        return decode(value, DEFAULT_CHARSET);
    }

    /**
     * 使用默认的UTF-8字符集编码请求参数值。
     *
     * @param value 参数值
     * @return 编码后的参数值
     */
    public static String encode(String value) {
        return encode(value, DEFAULT_CHARSET);
    }

    /**
     * 使用指定的字符集反编码请求参数值。
     *
     * @param value 参数值
     * @param charset 字符集
     * @return 反编码后的参数值
     */
    public static String decode(String value, Charset charset) {
        if (charset != null && !TextUtils.isEmpty(value)) {
            try {
                value = URLDecoder.decode(value, charset.name());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return value;
    }

    /**
     * 使用指定的字符集编码请求参数值。
     *
     * @param value 参数值
     * @param charset 字符集
     * @return 编码后的参数值
     */
    public static String encode(String value, Charset charset) {
        if (charset != null && !TextUtils.isEmpty(value)) {
            try {
                value = URLEncoder.encode(value, charset.name());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return value;
    }

    public static String buildForm(String baseUrl, Map<String, Object> parameters) {
        StringBuilder sb = new StringBuilder();
        sb.append("<form name=\"submit_form\" method=\"post\" action=\"");
        sb.append(baseUrl);
        sb.append("\">\n");
        buildHiddenFields(sb, parameters);
        sb.append("<input type=\"submit\" value=\"submit\" style=\"display:none\" >\n");
        sb.append("</form>\n");
        sb.append("<script>document.forms[0].submit();</script>");
        return sb.toString();
    }

    private static void buildHiddenFields(StringBuilder sb, Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return;
        }
        Set<Entry<String, Object>> entries = parameters.entrySet();
        for (Entry<String, Object> entry : entries) {
            String value = BeanHelper.convert(entry.getValue(), String.class);
            // 除去参数中的空值
            if (entry.getKey() == null || value == null)
                continue;
            buildHiddenField(sb, entry.getKey(), value);
        }
    }

    private static void buildHiddenField(StringBuilder sb, String key, String value) {
        sb.append("<input type=\"hidden\" name=\"");
        sb.append(key);
        sb.append("\" value=\"");
        //转义特殊字符
        if (value.indexOf('&') >= 0) {
            value = value.replace("&", "&amp;");
        }
        if (value.indexOf('<') >= 0) {
            value = value.replace("<", "&lt;");
        }
        if (value.indexOf('\"') >= 0) {
            value = value.replace("\"", "&quot;");
        }
        sb.append(value).append("\">");
    }

    /**
     * 获取文件的真实后缀名。目前只支持JPG, GIF, PNG, BMP四种图片文件。
     *
     * @param fileBytes 文件字节流
     * @return JPG, GIF, PNG or null
     */
    public static String getFileSuffix(byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length < 10) {
            return null;
        }
        if (fileBytes[0] == 'G' && fileBytes[1] == 'I' && fileBytes[2] == 'F') {
            return "gif";
        } else if (fileBytes[1] == 'P' && fileBytes[2] == 'N' && fileBytes[3] == 'G') {
            return "png";
        } else if (fileBytes[6] == 'J' && fileBytes[7] == 'F' && fileBytes[8] == 'I' && fileBytes[9] == 'F') {
            return "jpg";
        } else if (fileBytes[0] == 'B' && fileBytes[1] == 'M') {
            return "bmp";
        } else {
            return null;
        }
    }

    /**
     * 获取文件的真实媒体类型。目前只支持JPG, GIF, PNG, BMP四种图片文件。
     *
     * @param fileBytes 文件字节流
     * @return 媒体类型(MEME-TYPE)
     */
    public static String getMimeType(byte[] fileBytes) {
        String suffix = getFileSuffix(fileBytes);
        String mimeType;
        if ("jpg".equals(suffix)) {
            mimeType = "image/jpeg";
        } else if ("gif".equals(suffix)) {
            mimeType = "image/gif";
        } else if ("png".equals(suffix)) {
            mimeType = "image/png";
        } else if ("bmp".equals(suffix)) {
            mimeType = "image/bmp";
        } else {
            mimeType = "application/octet-stream";
        }
        return mimeType;
    }

}