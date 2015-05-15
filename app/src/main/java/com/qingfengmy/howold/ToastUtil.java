

package com.qingfengmy.howold;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

public class ToastUtil {
	public static Toast mToast;

	@SuppressLint("ShowToast")
	private static void initToast(Context context) {
		if (mToast != null) {
			return;
		}
		if (context == null) {
			return;
		}
		mToast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
	}

	public static void showToast(Context context,int resId) {
		initToast(context);
		if (resId < 0 || mToast == null) {
			return;
		}

		mToast.setText(resId);
		mToast.show();
	}

	public static void showToast(Context context,String message) {
		initToast(context);
		if (TextUtils.isEmpty(message) || mToast == null) {
			return;
		}

		mToast.setText(message);
		mToast.show();
	}

	public static void dismissToast() {
		if (mToast == null) {
			return;
		}
		mToast.cancel();
	}
}
