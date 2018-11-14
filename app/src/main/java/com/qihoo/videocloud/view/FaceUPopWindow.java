package com.qihoo.videocloud.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar;

import com.qihoo.livecloud.livekit.api.QHVCFaceUCallBack;
import com.qihoo.livecloudrefactor.R;
import com.qihoo.videocloud.recorder.adapter.FaceURecylerViewAdapter;
import com.qihoo.videocloud.utils.AndroidUtil;

import java.io.File;
import java.util.ArrayList;


public class FaceUPopWindow extends PopupWindow {

    private Context context;
    private RecyclerView mfaceURecyclerView;
    private ArrayList<String> faceUfilePathArrayList = new ArrayList<String>();
    private FaceUPopWindow.MyItemClickListener mOnItemClickListener;

    public FaceUPopWindow(Context context) {
        super(context);
        this.context = context;
        View faceUView = LayoutInflater.from(context).inflate(R.layout.face_u_popwindow_layout, null);
        initFaceUPopWindowView(faceUView);

        //设置SelectPicPopupWindow的View
        this.setContentView(faceUView);
        //设置SelectPicPopupWindow弹出窗体的宽
        this.setWidth(WindowManager.LayoutParams.MATCH_PARENT);
        //设置SelectPicPopupWindow弹出窗体的高
        this.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        //设置SelectPicPopupWindow弹出窗体可点击
        this.setFocusable(true);
        this.setOutsideTouchable(true);
        this.setTouchable(true);
        this.setBackgroundDrawable(new BitmapDrawable());
        this.setAnimationStyle(R.style.popupWindowAnimation);
    }

    /**
     * 初始化faceUpopWindow
     *
     * @param faceUView
     */
    private void initFaceUPopWindowView(View faceUView) {
        mfaceURecyclerView = (RecyclerView) faceUView.findViewById(R.id.face_u_reclcyerview);
        GridLayoutManager mgr = new GridLayoutManager(context, 6);
        mgr.setOrientation(LinearLayoutManager.VERTICAL);
        mfaceURecyclerView.setLayoutManager(mgr);
        String appRootDirPath = AndroidUtil.getAppDir() + "eff";
        File rootFile = new File(appRootDirPath);
        File[] array = rootFile.listFiles();
        if (array == null) {
            return;
        }
        ArrayList<Bitmap> bitmapArrayList = new ArrayList<Bitmap>();
        faceUfilePathArrayList.clear();
        bitmapArrayList.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.recorder_face_u_close));
        faceUfilePathArrayList.add("");/*关闭按钮*/
        for (int i = 0; i < array.length; i++) {
            String fileName = array[i].getName();
            faceUfilePathArrayList.add(appRootDirPath + File.separator + fileName);
            Bitmap bitmap = BitmapFactory.decodeFile(appRootDirPath + File.separator + fileName + File.separator + fileName + ".png");
            if (bitmap != null) {
                bitmapArrayList.add(bitmap);
            }
        }
        FaceURecylerViewAdapter recylerViewAdapter = new FaceURecylerViewAdapter(bitmapArrayList);
        recylerViewAdapter.setOnItemClickListener(new FaceURecylerViewAdapter.MyItemClickListener() {
            @Override
            public void onItemClick(View view, int postion) {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(view, postion, faceUfilePathArrayList.get(postion));
                }
            }
        });
        mfaceURecyclerView.setAdapter(recylerViewAdapter);
    }

    //声明MyItemClickListener这个接口
    public interface MyItemClickListener {
        public void onItemClick(View view, int postion, String faceUPath);
    }

    public void setOnItemClickListener(FaceUPopWindow.MyItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }
}
