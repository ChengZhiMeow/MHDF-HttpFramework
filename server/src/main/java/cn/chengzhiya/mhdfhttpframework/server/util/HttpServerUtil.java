package cn.chengzhiya.mhdfhttpframework.server.util;

import com.alibaba.fastjson2.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public final class HttpServerUtil {
    /**
     * 返回字符串数据
     *
     * @param response 请求实例
     * @param string   字符串
     */
    public static void returnStringData(HttpServletResponse response, String string) {
        try {
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write(string);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 返回文件数据
     *
     * @param response 请求实例
     * @param file     文件实例
     */
    public static void returnFileData(HttpServletResponse response, File file) {
        if (file == null || !file.exists()) {
            return;
        }

        response.setContentType(getFilContentType(file));
        try {
            try (InputStream in = Files.newInputStream(file.toPath())) {
                try (OutputStream out = response.getOutputStream()) {
                    int len;
                    byte[] byteData = new byte[1042];
                    while ((len = in.read(byteData)) != -1) {
                        out.write(byteData, 0, len);
                    }
                    out.flush();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 转换文本为指定类型
     *
     * @param type 类型类实例
     * @param data 文本
     * @return 转换后的实例
     */
    public static <T> T converter(Class<T> type, String data) {
        if (type == null || data == null) {
            return null;
        }

        if (type.equals(Integer.class)) {
            return type.cast(Integer.parseInt(data));
        } else if (type.equals(Double.class)) {
            return type.cast(Double.parseDouble(data));
        } else if (type.equals(Float.class)) {
            return type.cast(Float.parseFloat(data));
        }

        return type.cast(data);
    }

    /**
     * 获取请求数据实例
     *
     * @param request 请求实例
     * @return 请求数据实例
     */
    public static JSONObject getRequestBody(HttpServletRequest request) {
        try {
            InputStream in = request.getInputStream();
            byte[] bytes = in.readAllBytes();
            return JSONObject.parseObject(new String(bytes));
        } catch (IOException e) {
            return new JSONObject();
        }
    }

    /**
     * 获取指定文件实例的内容类型
     *
     * @param file 文件实例
     * @return 内容类型
     */
    private static String getFilContentType(File file) {
        int i = file.getPath().lastIndexOf(".");
        String suffix = file.getPath().substring(i + 1);
        return switch (suffix.toLowerCase()) {
            case "pl" -> "application/x-perl";
            case "ps" -> "application/postscript";
            case "rv" -> "video/vnd.rn-realvideo";
            case "fdf" -> "application/vnd.fdf";
            case "p10" -> "application/pkcs10";
            case "crl" -> "application/pkix-crl";
            case "ras" -> "image/x-cmu-raster";
            case "js" -> "application/javascript";
            case "gz" -> "application/gzip";
            case "bz2" -> "application/x-bzip2";
            case "xpi" -> "application/x-xpinstall";
            case "edi" -> "application/edi-x12";
            case "ipa" -> "application/vnd.iphone";
            case "azw" -> "application/vnd.amazon.ebook";
            case "xwd" -> "image/x-xwindowdump";
            case "323" -> "text/h323";
            case "vcf" -> "text/vcard";
            case "acp" -> "audio/x-mei-aac";
            case "wma" -> "audio/x-ms-wma";
            case "m3u" -> "audio/x-mpegurl";
            case "odc" -> "text/x-ms-odc";
            case "class" -> "application/java-vm";
            case "latex" -> "application/x-latex";
            case "epub" -> "application/epub+zip";
            case "p7s" -> "application/pkcs7-signature";
            case "wbmp" -> "image/vnd.wap.wbmp";
            case "psd" -> "image/vnd.adobe.photoshop";
            case "torrent" -> "application/x-bittorrent";
            case "p7r" -> "application/x-pkcs7-certreqresp";
            case "mobi" -> "application/x-mobipocket-ebook";
            case "7z" -> "application/x-7z-compressed";
            case "rar" -> "application/vnd.rar";
            case "xap" -> "application/x-silverlight-app";
            case "apk" -> "application/vnd.android.package-archive";
            case "ppsx" -> "application/vnd.openxmlformats-officedocument.presentationml.slideshow";
            case "xltx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.template";
            case "dotx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.template";
            case "potx" -> "application/vnd.openxmlformats-officedocument.presentationml.template";
            case "ppam" -> "application/vnd.ms-powerpoint.addin.macroEnabled.12";
            case "pptm" -> "application/vnd.ms-powerpoint.presentation.macroEnabled.12";
            case "potm" -> "application/vnd.ms-powerpoint.template.macroEnabled.12";
            case "xlam" -> "application/vnd.ms-excel.addin.macroEnabled.12";
            case "xlsm" -> "application/vnd.ms-excel.sheet.macroEnabled.12";
            case "xltm" -> "application/vnd.ms-excel.template.macroEnabled.12";
            case "docm" -> "application/vnd.ms-word.document.macroEnabled.12";
            case "dotm" -> "application/vnd.ms-word.template.macroEnabled.12";
            case "xlsb" -> "application/vnd.ms-excel.sheet.binary.macroEnabled.12";
            case "ppsm" -> "application/vnd.ms-powerpoint.slideshow.macroEnabled.12";
            case "au", "snd" -> "audio/basic";
            case "pls", "xpl" -> "audio/scpls";
            case "dtd", "ent" -> "text/xml";
            case "p12", "pfx" -> "application/x-pkcs12";
            case "asf", "asx" -> "video/x-ms-asf";
            case "mp4", "m4e" -> "video/mpeg4";
            case "p7c", "p7m" -> "application/pkcs7-mime";
            case "ra", "ram" -> "audio/x-pn-realaudio";
            case "mfp", "swf" -> "application/x-shockwave-flash";
            case "mid", "midi", "rmi" -> "audio/midi";
            case "aif", "aifc", "aiff" -> "audio/x-aiff";
            case "eml", "mht", "mhtml" -> "message/rfc822";
            case "jar", "war", "ear" -> "application/java-archive";
            case "accdb", "mdb" -> "application/msaccess";
            case "sis", "sisx" -> "application/vnd.symbian.install";
            case "dll", "exe", "msi" -> "application/x-msdownload";
            case "p7b", "spc" -> "application/x-pkcs7-certificates";
            case "g4", "cg4", "ig4" -> "application/x-g4";
            case "ical", "ics", "ifb" -> "text/calendar";
            case "xls", "xlw", "xlsx" -> "application/vnd.ms-excel";
            case "mpeg", "m1v", "m2v", "mpe", "mpg" -> "video/mpeg";
            case "asp", "asa", "htx", "stm" -> "text/html";
            case "cer", "crt", "der", "pem" -> "application/x-x509-ca-cert";
            case "pot", "pps", "ppt", "pptx" -> "application/vnd.ms-powerpoint";
            case "doc", "docx", "dot", "rtf", "wiz" -> "application/msword";
            case "ws", "ws2" -> "application/x-ws";
            case "dwg", "dxf" -> "image/vnd." + suffix;
            case "tar", "xz", "anv", "c90", "cdr", "cel", "cmp", "cot", "csi", "cut", "dbf", "dbm", "dbx", "dcx", "dgn",
                 "dib", "drw", "dxb", "emf", "epi", "frm", "gbr", "gl2", "gp4", "hgl", "hmr", "hpg", "hpl", "hrf",
                 "icb", "iff", "igs", "img", "lbm", "ltr", "mac", "mil", "out", "pci", "pcl", "pic", "pr", "prn", "prt",
                 "ptn", "sam", "sat", "sdw", "slb", "sld", "smk", "sty", "tdf", "tg4", "uin", "vda", "vpg", "wb1",
                 "wb2", "wb3", "wk3", "wk4", "wkq", "wks", "wmd", "wmf", "wp6", "wpd", "wpg", "wq1", "wr1", "wri",
                 "wrk", "x_b", "x_t" -> "application/x-" + suffix;
            case "pdf", "zip", "json" -> "application/" + suffix;
            case "cmx", "pcx", "rgb", "tga" -> "image/x-" + suffix;
            case "c", "h", "cs", "cpp", "py", "css", "csv", "htm", "java", "php", "txt", "xml", "html" ->
                    "text/" + suffix;
            case "aac", "flac", "m4a", "mp3", "ogg", "wav" -> "audio/" + suffix;
            case "avi", "flv", "ivf", "mkv", "mov", "webm", "wmv", "wmx", "wvx" -> "video/" + suffix;
            case "bmp", "fax", "gif", "ico", "jpe", "jpg", "png", "tif", "webp", "jpeg", "jfif", "svg", "tiff" ->
                    "image/" + suffix;
            default -> "application/octet-stream";
        } + ";charset=utf-8";
    }
}
