package com.pacsapp.pacsapp.util;

import org.apache.commons.io.FilenameUtils;

import java.io.File;

public class FileUtilities {

    public static File getPACSAppUserFolder(){
        File pacsAppFolder = new File(FilenameUtils.concat(System.getProperty("user.home"), "PACSApp"));
        if(!pacsAppFolder.exists()){
            pacsAppFolder.mkdirs();
        }
        return  pacsAppFolder;
    }

    public static String getPACSServerDataPath(){
        return FilenameUtils.concat(getPACSAppUserFolder().getAbsolutePath(), "PacsServerData.json");
    }

}
