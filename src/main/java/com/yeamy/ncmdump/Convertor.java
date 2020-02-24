package com.yeamy.ncmdump;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Collection;

public class Convertor {
    public static void main(String[] args) {
        File baseDir = new File(args[0]);

        if (!baseDir.exists() || !baseDir.isDirectory()) {
            System.out.println("指定的路径错误: " + args[0]);
        }

        String[] extName = new String[1];
        extName[0] = "ncm";
        Collection<File> files = FileUtils.listFiles(baseDir, extName, true);
        System.out.println(files);

        for(File file : files) {
          boolean result =  NcmDump.dump(file, file.getParentFile());
          if(result) {
              File target = new File("D:/Temp/output_music/" + file.getName());
              file.renameTo(target);
          }
        }
    }
}
