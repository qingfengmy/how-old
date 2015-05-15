package com.qingfengmy.howold;

import android.os.Environment;

/**
 * Created by Administrator on 2015/5/14.
 */
public class Constant {
    public static final String APIKEY = "3a4b33974f9b28fbf779176c41d7dda9";
    public static final String APISECRET = "cGypoX7yxhs9G4YcwlL7Vk0Rs-nNxj96";
    public static final String PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "how-old";
    public static final String HOWOLDJPG = "/howold.jpg";
    public static final int GET_PIC_FROM_PHOTO_REQUEST = 1;
    public static final int GET_PIC_FROM_CAMERA_REQUEST = 2;
    public static final int CROP_PIC_REQUEST = 3;
}
