package com.tehike.client.mst.app.project.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;

/**
 * Created by Root on 2018/9/4.
 */

public class PermissionUtils {

    public PermissionUtils() {
        throw new UnsupportedOperationException("Can't support Construct !!!");
    }

    public static boolean checkPermissions(Context mContexts, String[] permissions) {
        for (String permission : permissions) {
            if (checkPermission(mContexts, permission)) {
                return true;
            }
        }
        return false;
    }


    /**
     * 判断权限是否已申请
     * true success
     * false fail
     * @param mContexts
     * @param permission
     * @return
     */
    public static boolean checkPermission(Context mContexts, String permission) {
        return ContextCompat.checkSelfPermission(mContexts, permission) ==
                PackageManager.PERMISSION_GRANTED ? true : false;
    }
}
