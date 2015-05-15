package com.qingfengmy.howold;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facepp.error.FaceppParseException;

import net.youmi.android.banner.AdSize;
import net.youmi.android.banner.AdView;
import net.youmi.android.diy.DiyManager;
import net.youmi.android.spot.SpotManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;


public class MainActivity extends ActionBarActivity {

    public static final String TO = "to";
    public static final int MY = 1;
    public static final int OTHER = 2;
    // 弹出对话框的按钮值
    private static final int TAKE_PICTURE = 0;
    private static final int CHOOSE_PICTURE = 1;

    private static final int MSG_SUCCESS = 6;
    private static final int MSG_ERROR = 7;

    private static final String IMAGE_FILE_LOCATION = "file:///sdcard/temp.jpg";
    Uri imageUri = Uri.parse(IMAGE_FILE_LOCATION);

    private Bitmap bitmap;

    public static final String LOCATION = Environment
            .getExternalStorageDirectory().getAbsolutePath()
            + "/how-old";

    @InjectView(R.id.tv_tip)
    TextView tip;

    @InjectView(R.id.iv_photo)
    ImageView photo;
    @InjectView(R.id.icLauncher)
    ImageView icLauncher;
    @InjectView(R.id.loading)
    FrameLayout loading;

    @InjectView(R.id.tv_age)
    TextView ageText;

    @InjectView(R.id.showPhoto)
    RelativeLayout showPhoto;

    Bitmap mPhotoImg;
    private Paint mPaint;

    @InjectView(R.id.ok)
    Button ok;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case MSG_SUCCESS:
                    loading.setVisibility(View.GONE);
                    JSONObject rs = (JSONObject) msg.obj;
                    prepareRsBitmap(rs);
                    photo.setImageBitmap(mPhotoImg);
                    ok.setEnabled(true);

                    SpotManager.getInstance(MainActivity.this).showSpotAds(MainActivity.this);
                    break;
                case MSG_ERROR:
                    loading.setVisibility(View.GONE);
                    FaceppParseException exception = (FaceppParseException) msg.obj;
                    if (!TextUtils.isEmpty(exception.getErrorMessage())) {
                        tip.setText(exception.getErrorMessage());
                    } else if (!TextUtils.isEmpty(exception.getMessage())) {
                        tip.setText(exception.getErrorMessage());
                    }
            }
        }
    };

    private void prepareRsBitmap(JSONObject rs) {

        Bitmap bitmap = Bitmap.createBitmap(mPhotoImg.getWidth(), mPhotoImg.getHeight(), mPhotoImg.getConfig());
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(mPhotoImg, 0, 0, null);

        try {
            JSONArray faces = rs.getJSONArray("face");
            int faceCount = faces.length();
            tip.setText("find " + faceCount);

            for (int i = 0; i < faceCount; i++) {
                JSONObject face = faces.getJSONObject(i);
                JSONObject posObj = face.getJSONObject("position");
                // 取中心点，其值表示相对于图片的百分比
                float x = (float) posObj.getJSONObject("center").getDouble("x");
                float y = (float) posObj.getJSONObject("center").getDouble("y");
                // 取长宽
                float w = (float) posObj.getDouble("width");
                float h = (float) posObj.getDouble("height");
                // 百分比转换
                x = x / 100 * bitmap.getWidth();
                y = y / 100 * bitmap.getHeight();

                w = w / 100 * bitmap.getWidth();
                h = h / 100 * bitmap.getHeight();

                // 画线
                canvas.drawLine(x - w / 2, y - h / 2, x - w / 2, y + h / 2, mPaint);
                canvas.drawLine(x - w / 2, y - h / 2, x + w / 2, y - h / 2, mPaint);
                canvas.drawLine(x + w / 2, y - h / 2, x + w / 2, y + h / 2, mPaint);
                canvas.drawLine(x - w / 2, y + h / 2, x + w / 2, y + h / 2, mPaint);

                // get age and gender
                int age = face.getJSONObject("attribute").getJSONObject("age").getInt("value");
                String gender = face.getJSONObject("attribute").getJSONObject("gender").getString("value");

                Bitmap ageBitmap = buildAgeBitmap(age, "Male".equals(gender));

                int ageWidth = ageBitmap.getWidth();
                int ageHeight = ageBitmap.getHeight();

                if (w < ageBitmap.getWidth() && h < ageBitmap.getHeight()) {
                    float ratio = Math.max(w * 1.0f / ageBitmap.getWidth(), h * 1.0f / ageBitmap.getHeight());
                    if (w < ageWidth / 3) {
                        ratio = ratio * 1.5f;
                    } else if (w > ageWidth / 2) {
                        ratio = ratio / 3;
                    }
                    ageBitmap = Bitmap.createScaledBitmap(ageBitmap, (int) (ageWidth * ratio), (int) (ageHeight * ratio), false);
                }
                canvas.drawBitmap(ageBitmap, x - ageBitmap.getWidth() / 2, y - h / 2 - ageBitmap.getHeight(), null);
            }

            mPhotoImg = bitmap;
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private Bitmap buildAgeBitmap(int age, boolean isMale) {

        ageText.setText(age + "");
        if (isMale) {
            ageText.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.mipmap.male), null, null, null);
        } else {
            ageText.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.mipmap.female), null, null, null);
        }

        ageText.setDrawingCacheEnabled(true);
        Bitmap ageBitmap = Bitmap.createBitmap(ageText.getDrawingCache());
        ageText.destroyDrawingCache();
        return ageBitmap;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        mPaint = new Paint();
        mPaint.setColor(0xffffffff);
        mPaint.setStrokeWidth(2);

        RotateAnimation rotate = new RotateAnimation(0, 360, 0.5f, 0.5f);
        rotate.setDuration(1500);
        rotate.setInterpolator(new AnticipateOvershootInterpolator());
        rotate.setRepeatMode(Animation.REVERSE);
        rotate.setRepeatCount(-1);
        icLauncher.startAnimation(rotate);


        // 实例化广告条
        AdView adView = new AdView(this, AdSize.FIT_SCREEN);
        // 获取要嵌入广告条的布局
        LinearLayout adLayout=(LinearLayout)findViewById(R.id.adLayout);
        // 将广告条加入到布局中
        adLayout.addView(adView);
    }


    public void detect() {
        loading.setVisibility(View.VISIBLE);
        if (mPhotoImg == null) {
            return;
        }
        FaceppDetect.detect(mPhotoImg, new FaceppDetect.CallBack() {
            @Override
            public void success(JSONObject jsonObject) {
                Message message = Message.obtain();
                message.what = MSG_SUCCESS;
                message.obj = jsonObject;
                mHandler.sendMessage(message);
            }

            @Override
            public void error(FaceppParseException exception) {
                Message message = Message.obtain();
                message.what = MSG_ERROR;
                message.obj = exception;
                mHandler.sendMessage(message);
            }
        });
    }

    @OnClick(R.id.create)
    public void getImage() {
        showPicturePicker(this, true);

        ok.setEnabled(false);
    }

    @OnClick(R.id.lookAtMy)
    public void lookMy() {
        Intent intent = new Intent();
        intent.setClass(this, ViewPagerMultiFragment.class);
        intent.putExtra(TO, MY);
        startActivity(intent);
    }

    @OnClick(R.id.lookAtOther)
    public void lookOther() {
        Intent intent = new Intent();
        intent.setClass(this, ViewPagerMultiFragment.class);
        intent.putExtra(TO, OTHER);
        startActivity(intent);
    }

    @OnClick(R.id.ok)
    public void download() {
        long currentTimeMillis = System.currentTimeMillis();
        ImageTools.savePhotoToSDCard(this, bitmap, LOCATION,
                currentTimeMillis + "");
        showPhoto.setVisibility(View.INVISIBLE);
    }

    @OnClick(R.id.cancle)
    public void cancle() {
        // 返回
        showPhoto.setVisibility(View.INVISIBLE);
    }

    @OnClick(R.id.more)
    public void clickMore() {
        DiyManager.showRecommendWall(this); // 展示所有无积分推荐墙
    }

    @OnClick(R.id.github)
    public void clicGitHub() {
        Intent intent = new Intent();
        intent.setData(Uri.parse("https://github.com/qingfengmy"));
        intent.setAction(Intent.ACTION_VIEW);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constant.GET_PIC_FROM_PHOTO_REQUEST) {
            createCropImg(data);
        } else if (requestCode == Constant.GET_PIC_FROM_CAMERA_REQUEST) {
            cropImageUri(imageUri, 200, 200);
        } else if (requestCode == Constant.CROP_PIC_REQUEST) {
            createCropImg(data);
        }
    }

    public void showPicturePicker(Context context, boolean isCrop) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("图片来源");
        builder.setNegativeButton("取消", null);
        builder.setItems(new String[]{"拍照", "相册"},
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case TAKE_PICTURE:
                                Intent intent = new Intent(
                                        MediaStore.ACTION_IMAGE_CAPTURE);
                                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                                startActivityForResult(intent,
                                        Constant.GET_PIC_FROM_CAMERA_REQUEST);
                                break;
                            case CHOOSE_PICTURE:
                                intent = new Intent(Intent.ACTION_GET_CONTENT, null);
                                intent.addCategory(Intent.CATEGORY_OPENABLE);
                                intent.setType("image/*");
                                intent.putExtra("crop", "true");
                                intent.putExtra("aspectX", 1);
                                intent.putExtra("aspectY", 1);
                                intent.putExtra("outputX", 800);
                                intent.putExtra("outputY", 800);
                                intent.putExtra("scale", true);
                                intent.putExtra("return-data", false);
                                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                                intent.putExtra("outputFormat",
                                        Bitmap.CompressFormat.JPEG.toString());
                                intent.putExtra("noFaceDetection", false);
                                startActivityForResult(intent,
                                        Constant.GET_PIC_FROM_PHOTO_REQUEST);
                                break;
                            default:
                                break;
                        }
                    }
                });
        builder.create().show();
    }

    private void cropImageUri(Uri uri, int outputX, int outputY) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", outputX);
        intent.putExtra("outputY", outputY);
        intent.putExtra("scale", true);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        intent.putExtra("return-data", false);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", true); // no face detection
        startActivityForResult(intent, Constant.CROP_PIC_REQUEST);
    }

    private void createCropImg(Intent data) {
        Bitmap tempPhoto = decodeUriAsBitmap(imageUri);
        if (tempPhoto != null) {
            mPhotoImg = resizePhoto(tempPhoto);
            showPhoto.setVisibility(View.VISIBLE);
            photo.setImageBitmap(mPhotoImg);
            tip.setText("分析中...");

            detect();
        } else {
            tip.setText("error");
        }
    }

    private Bitmap decodeUriAsBitmap(Uri uri) {
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(getContentResolver()
                    .openInputStream(uri));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return bitmap;
    }


    private Bitmap resizePhoto(Bitmap image) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (image.getByteCount() / 1024 > 1024 * 3) {//判断如果图片大于3M,进行压缩避免在生成图片（BitmapFactory.decodeStream）时溢出
            // 压缩
            image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            InputStream temp = new ByteArrayInputStream(baos.toByteArray());

            BitmapFactory.Options options = new BitmapFactory.Options();
            // 这个参数代表，不为bitmap分配内存空间，只记录一些该图片的信息（例如图片大小），说白了就是为了内存优化
            options.inJustDecodeBounds = true;
            // 通过创建图片的方式，取得options的内容（这里就是利用了java的地址传递来赋值）
            // 这个参数表示 新生成的图片为原始图片的几分之一。
            options.inSampleSize = 2;
            // 这里之前设置为了true，所以要改为false，否则就创建不出图片
            options.inJustDecodeBounds = false;

            image = BitmapFactory.decodeStream(temp, null, options);
        }
        // 返回
        return image;
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (showPhoto.getVisibility() != View.VISIBLE) {
                // 退出程序
                if ((System.currentTimeMillis() - exitTime) > 2000) {
                    Toast.makeText(getApplicationContext(), "再按一次退出程序",
                            Toast.LENGTH_SHORT).show();
                    exitTime = System.currentTimeMillis();
                    SpotManager.getInstance(this).showSpotAds(this);
                } else {
                    finish();
                    System.exit(0);
                }
                return true;
            }
        }
        return true;
    }

    /**
     * 截取view图片
     *
     * @param context
     * @return
     */
    private Bitmap captureScreen(View cv, Activity context) {
        Bitmap bmp = Bitmap.createBitmap(cv.getWidth(), cv.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        cv.draw(canvas);
        return bmp;
    }

    @Override
    protected void onDestroy() {
        SpotManager.getInstance(this).unregisterSceenReceiver();
        super.onDestroy();
    }

    private long exitTime = 0;

    @Override
    public void onBackPressed() {
        // 如果有需要，可以点击后退关闭插屏广告（可选）。
        if (!SpotManager.getInstance(this).disMiss(true)) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onStop() {
        // 如果不调用此方法，则按home键的时候会出现图标无法显示的情况。
        SpotManager.getInstance(this).disMiss(false);

        super.onStop();
    }
}
