package com.qingfengmy.howold;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

import net.youmi.android.banner.AdSize;
import net.youmi.android.banner.AdView;
import net.youmi.android.spot.SpotManager;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facepp.error.FaceppParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class ViewPagerMultiFragment extends ActionBarActivity {
	private static int TOTAL_COUNT = 9;

	private RelativeLayout viewPagerContainer;
	private ViewPager viewPager;
	private TextView indexText;
	private int from;
	private Button rightButton;
	private String[] lists;
	private int currentPosition;

	@InjectView(R.id.iv_photo)
	ImageView photo;

	@InjectView(R.id.loading)
	FrameLayout loading;

	@InjectView(R.id.tv_tip)
	TextView tip;

	@InjectView(R.id.tv_age)
	TextView ageText;

	@InjectView(R.id.showPhoto)
	RelativeLayout showPhoto;

	Bitmap mPhotoImg;
	private Paint mPaint;

	public static final String LOCATION = Environment
			.getExternalStorageDirectory().getAbsolutePath()
			+ "/how-old";
	@InjectView(R.id.ok)
	Button ok;
	private static final String IMAGE_FILE_LOCATION = "file:///sdcard/temp.jpg";

	private static final int MSG_SUCCESS = 6;
	private static final int MSG_ERROR = 7;
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
					SpotManager.getInstance(ViewPagerMultiFragment.this).showSpotAds(ViewPagerMultiFragment.this);
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
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_viewpagermultifragment);
		ButterKnife.inject(this);
		Intent intent = getIntent();
		from = intent.getIntExtra(MainActivity.TO, MainActivity.OTHER);
		if (from == MainActivity.MY) {
			File parent = new File(MainActivity.LOCATION);
			lists = parent.list();
			TOTAL_COUNT = lists.length;
		} else {
			TOTAL_COUNT = 9;
		}

		viewPager = (ViewPager) findViewById(R.id.view_pager);
		indexText = (TextView) findViewById(R.id.view_pager_index);
		viewPagerContainer = (RelativeLayout) findViewById(R.id.pager_layout);
		rightButton = (Button) findViewById(R.id.rightButton);

		if (TOTAL_COUNT != 0) {
			viewPager.setAdapter(new MyPagerAdapter());
			// to cache all page, or we will see the right item delayed
			viewPager.setOffscreenPageLimit(TOTAL_COUNT);
			viewPager.setPageMargin(getResources().getDimensionPixelSize(
					R.dimen.page_margin));
			MyOnPageChangeListener myOnPageChangeListener = new MyOnPageChangeListener();
			viewPager.setOnPageChangeListener(myOnPageChangeListener);

			viewPagerContainer.setOnTouchListener(new OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					// dispatch the events to the ViewPager, to solve the
					// problem
					// that we can swipe only the middle view.
					return viewPager.dispatchTouchEvent(event);
				}
			});

			if (from == MainActivity.MY) {
				rightButton.setText("删除");
			} else {
				rightButton.setText("分析");
			}

			rightButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					ProgressDialog dialog = new ProgressDialog(
							ViewPagerMultiFragment.this);
					dialog.show();
					if (from == MainActivity.MY) {
						// 删除
						File file = new File(MainActivity.LOCATION
								+ File.separator + lists[currentPosition]);
						if (file.delete()) {
							File parent = new File(MainActivity.LOCATION);
							lists = parent.list();
							TOTAL_COUNT = lists.length;
							viewPager.setAdapter(new MyPagerAdapter());
							// to cache all page, or we will see the right item
							// delayed
							viewPager.setOffscreenPageLimit(TOTAL_COUNT);
							viewPager
									.setPageMargin(getResources()
											.getDimensionPixelSize(
													R.dimen.page_margin));
							MyOnPageChangeListener myOnPageChangeListener = new MyOnPageChangeListener();
							viewPager
									.setOnPageChangeListener(myOnPageChangeListener);

							Toast.makeText(ViewPagerMultiFragment.this, "删除成功",
									Toast.LENGTH_SHORT).show();
						}
					} else {
						// 分析
						detect();
						ok.setEnabled(false);
						Bitmap tempPhoto = BitmapFactory.decodeResource(
								getResources(), R.mipmap.image1
										+ currentPosition);
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
					dialog.hide();
				}
			});
			indexText.setText(new StringBuilder().append("1/").append(
					TOTAL_COUNT));
			rightButton.setVisibility(View.VISIBLE);
		} else {
			indexText.setText("空空如也");
			rightButton.setVisibility(View.GONE);
		}


		mPaint = new Paint();
		mPaint.setColor(0xffffffff);
		mPaint.setStrokeWidth(2);

		// 实例化广告条
		AdView adView = new AdView(this, AdSize.FIT_SCREEN);
		// 获取要嵌入广告条的布局
		LinearLayout adLayout=(LinearLayout)findViewById(R.id.adLayout);
		// 将广告条加入到布局中
		adLayout.addView(adView);
	}

	/**
	 * this is a example fragment, just a imageview, u can replace it with your
	 * needs
	 * 
	 * @author Trinea 2013-04-03
	 */
	class MyPagerAdapter extends PagerAdapter {

		@Override
		public int getCount() {
			return TOTAL_COUNT;
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return (view == object);
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			ImageView imageView = new ImageView(ViewPagerMultiFragment.this);
			if (from == MainActivity.MY) {
				Bitmap bm = BitmapFactory.decodeFile(MainActivity.LOCATION
						+ File.separator + lists[position]);
				imageView.setImageBitmap(bm);
			} else {
				imageView.setImageResource(R.mipmap.image1 + position);
			}
			((ViewPager) container).addView(imageView, position);
			return imageView;

		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			((ViewPager) container).removeView((ImageView) object);
		}
	}

	public class MyOnPageChangeListener implements OnPageChangeListener {

		@Override
		public void onPageSelected(int position) {
			indexText.setText(new StringBuilder().append(position + 1)
					.append("/").append(TOTAL_COUNT));
			currentPosition = position;
		}

		@Override
		public void onPageScrolled(int position, float positionOffset,
				int positionOffsetPixels) {
			if (viewPagerContainer != null) {
				viewPagerContainer.invalidate();
			}
		}

		@Override
		public void onPageScrollStateChanged(int arg0) {
		}
	}


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


	public void detect() {
		loading.setVisibility(View.VISIBLE);
		mPhotoImg = BitmapFactory.decodeResource(
				getResources(), R.mipmap.image1
						+ currentPosition);
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
	@OnClick(R.id.ok)
	public void download() {
		ImageTools.savePhotoToSDCard(
				ViewPagerMultiFragment.this, mPhotoImg,
				MainActivity.LOCATION,
				System.currentTimeMillis() + "");
		showPhoto.setVisibility(View.INVISIBLE);
	}

	@OnClick(R.id.cancle)
	public void cancle() {
		// 返回
		showPhoto.setVisibility(View.INVISIBLE);
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
}
