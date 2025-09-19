package cn.chengzhiya.mhdfhttpframework.server.util;

import lombok.SneakyThrows;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class ClassUtil {
    /**
     * 获取当前ClassLoader的类列表
     *
     * @return 类列表
     */
    @SneakyThrows
    public static List<Class<?>> getClasses() {
        List<Class<?>> classList = new ArrayList<>();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources("");
        if (!resources.hasMoreElements()) {
            URL resource = ClassUtil.class.getProtectionDomain().getCodeSource().getLocation();
            String filePath = URLDecoder.decode(resource.getFile(), StandardCharsets.UTF_8);
            ClassUtil.getClassListByJar(new JarFile(filePath), classList);
        } else {
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                String protocol = resource.getProtocol();

                if ("file".equals(protocol)) {
                    String filePath = URLDecoder.decode(resource.getFile(), StandardCharsets.UTF_8);
                    ClassUtil.getClassListByFolder(new File(filePath), "", classList);
                } else if ("jar".equals(protocol)) {
                    JarFile jar = ((JarURLConnection) resource.openConnection()).getJarFile();
                    ClassUtil.getClassListByJar(jar, classList);
                }
            }
        }
        return classList;
    }

    private static void getClassListByFolder(File folder, String packageName, List<Class<?>> classList) {
        if (!folder.exists() || !folder.isDirectory()) return;

        File[] files = folder.listFiles(file -> file.isDirectory() || file.getName().endsWith(".class"));
        if (files == null) return;

        for (File file : files) {
            StringBuilder sb = new StringBuilder(packageName);
            if (file.isDirectory()) {
                if (!sb.isEmpty() && !sb.substring(sb.length() - 2).equals(".")) sb.append(".");
                ClassUtil.getClassListByFolder(file, sb.append(file.getName()).toString(), classList);
            } else {
                String className = sb.append(".").append(file.getName(), 0, file.getName().length() - 6).toString();
                try {
                    classList.add(Class.forName(className));
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static void getClassListByJar(JarFile jar, List<Class<?>> classList) {
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName().replace("/", ".");
            if (name.charAt(0) == '.') name = name.substring(1);

            if (name.endsWith(".class") && !entry.isDirectory()) {
                String className = name.substring(0, name.length() - 6);
                try {
                    classList.add(Class.forName(className));
                } catch (Throwable ignored) {
                }
            }
        }
    }
}
