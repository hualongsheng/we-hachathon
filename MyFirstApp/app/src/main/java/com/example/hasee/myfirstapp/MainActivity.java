package com.example.hasee.myfirstapp;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothProfile;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import com.baidu.aip.ocr.AipOcr;
import android.app.Dialog;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.baidu.ocr.sdk.OCR;
import com.baidu.ocr.sdk.OnResultListener;
import com.baidu.ocr.sdk.exception.OCRError;
import com.baidu.ocr.sdk.model.AccessToken;
import com.baidu.ocr.sdk.model.OcrRequestParams;
import com.baidu.ocr.sdk.model.OcrResponseResult;
import com.example.hasee.myfirstapp.gui.OpenFileDialog;
import com.example.hasee.myfirstapp.gui.CallbackBundle;
import com.example.hasee.myfirstapp.httptools.HttpClientUtils;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

public class MainActivity extends AppCompatActivity {
    String imagePath = null;
    static private int openfileDialogId = 0;

    public static final String API_KEY = "vDZO3GocQcB5BI9QYGhVIBuQ";
    public static final String SECRET_KEY = "ckir1R8epRXlj7uyzCGmVgIoevw1VMpE";
    private List<String> data_list;
    private ArrayAdapter<String> arr_adapter;
    private AlertDialog.Builder alertDialog;
    private ImageView imageView;

//    private TextView tvCategory;
//    private TextView tvDate;
//    private TextView tvAmount;

    private Spinner spinner;
    private EditText etDate;
    private EditText etAmount;

    private void infoPopText(final String result) {
        alertText("", result);
    }
    private void alertText(final String title, final String message) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alertDialog.setTitle(title)
                        .setMessage(message)
                        .setPositiveButton("确定", null)
                        .show();
            }
        });
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.e("", "onCreate");

        setContentView(R.layout.activity_main);
        alertDialog = new AlertDialog.Builder(this);
//        imagePath = "/storage/emulated/0/DCIM/Camera/test.jpg";

        data_list = new ArrayList<>();

        data_list.add("出差交通费");
        data_list.add("差旅杂费");
        data_list.add("出差住宿费");
        data_list.add("差旅补助补贴");

        imageView = (ImageView) findViewById(R.id.imageView);

        spinner = (Spinner) findViewById(R.id.spinner);

        //适配器
        arr_adapter= new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, data_list);
        //设置样式
        arr_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //加载适配器
        spinner.setAdapter(arr_adapter);


//        etCategory = (EditText) findViewById(R.id.editText);
        etDate = (EditText) findViewById(R.id.editText2);
        etAmount = (EditText) findViewById(R.id.editText3);

        // 设置单击按钮时打开文件对话框
        findViewById(R.id.button_openfile).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                image();
//                showDialog(openfileDialogId);
            }
        });

        try
        {
            initAccessTokenWithAkSk();
        } catch (Exception e)
        {
            Log.e("", "Auth failed!");
        }


        // R.string.app_name

    }

    public static File getSaveFile(Context context) {
        File file = new File(context.getFilesDir(), "pic.jpg");
        return file;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2){
            String path = handleImageOnKitKat(data);
            Log.e("路径",">>" + path);

            try
            {
                // 通用票据识别参数设置
//                OcrRequestParams param = new OcrRequestParams();

                // 设置image参数
//                param.setImageFile(new File(path));


                // 设置额外参数
//                param.putParam("detect_direction", "true");


                Bitmap bm = BitmapFactory.decodeFile(path);
//                imageView.setImageBitmap(bm);

                RecognizeService.recReceipt(path,
                        new RecognizeService.ServiceListener() {
                            @Override
                            public void onResult(String result) {
                                infoPopText(result);

                                // 解析 result ==> category, date,  amount
//                                etCategory.setText("出差交通费");
                                etDate.setText("2017-12-01");
                                etAmount.setText("100.0");
//
                                setMoney(result);
                                setType(result);
                                Log.e("RES", result);
                            }
                        });

            }
            catch (Exception e)
            {
//                Log.d("result", res.toString());
                Log.d("", e.toString());
            }

        }
    }
    public void setMoney(String jsonResult) {
        try{

            JSONObject dataJson = new JSONObject(jsonResult);
            JSONArray words_result = dataJson.getJSONArray("words_result");
            for (int i = 0; i < words_result.length(); i++) {
                JSONObject result = words_result.getJSONObject(i);
                String words = result.getString("words");
                if (words.equals("金额") ){
                    if (i + 1 < words_result.length()) {
                        String money = "";
                        result = words_result.getJSONObject(i+1);
                        String tmpmoney = result.getString("words");
                        for (int j = 0; j < tmpmoney.length(); j++) {
                            if ((tmpmoney.charAt(j) >= '0' && tmpmoney.charAt(j) <= '9') ) {
                                money += tmpmoney.charAt(j);
                            }
                            else if(tmpmoney.charAt(j) == '.'|| tmpmoney.charAt(j) == '·'){
                                money += '.';
                            }
                        }
                        if(money.indexOf('.')==money.length()-2){
                            money+='0';
                        }
                        Log.e("",money);
                        etAmount.setText(money);
                    }
                } else if (words.contains("金额")) {
                    String money = "";
                    for (int j = 0; j < words.length(); j++) {
                        if ((words.charAt(j) >= '0' && words.charAt(j) <= '9')) {
                            money += words.charAt(j);
                        }
                        else if(words.charAt(j) == '.'|| words.charAt(j) == '·'){
                            money += '.';
                        }
                    }
                    if(money.indexOf('.')==money.length()-2){
                        money+='0';
                    }
                    Log.e("shl",money);
                    etAmount.setText(money);
                }
            }
        }
        catch(Exception e){
            Log.e("e","failed parse json");
        }

    }

    public void setType(String jsonResult) {
        if(jsonResult.contains("上车")||jsonResult.contains("下车")||jsonResult.contains("出租")){
            spinner.setSelection(0);
        }

    }
    private void initAccessTokenWithAkSk() {
        OCR.getInstance().initAccessTokenWithAkSk(new OnResultListener<AccessToken>() {
            @Override
            public void onResult(AccessToken result) {
                String token = result.getAccessToken();
            }

            @Override
            public void onError(OCRError error) {
                error.printStackTrace();
            }
        }, getApplicationContext(), API_KEY, SECRET_KEY);
    }

    private void image() {
        Intent intent = new Intent();
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        //根据版本号不同使用不同的Action
        if (Build.VERSION.SDK_INT < 19) {
            intent.setAction(Intent.ACTION_GET_CONTENT);
        } else {
            intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        }
        startActivityForResult(intent, 2);
    }


    /**
     * @描述 4.4及以上系统使用这个方法处理图片 相册图片返回的不再是真实的Uri,而是分装过的Uri
     * */
    @TargetApi(19)
    private String handleImageOnKitKat(Intent data) {
        imagePath = null;
        Uri uri = data.getData();
        Log.e("TAG", "handleImageOnKitKat: uri is " + uri);
        if (DocumentsContract.isDocumentUri(this, uri)) {
            // 如果是document类型的Uri，则通过document id处理
            String docId = DocumentsContract.getDocumentId(uri);
            if (Variable.android_providers_media_documents.equals(uri.getAuthority())) {
                String id = docId.split(":")[1]; // 解析出数字格式的id
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if (Variable.android_providers_downloads_documents.equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if (Variable.content.equalsIgnoreCase(uri.getScheme())) {//equalsIgnoreCase 比较的两个字符串 可以不区分大小写
            // 如果是content类型的Uri，则使用普通方式处理
            imagePath = getImagePath(uri, null);
        } else if (Variable.file.equalsIgnoreCase(uri.getScheme())) {
            // 如果是file类型的Uri，直接获取图片路径即可
            imagePath = uri.getPath();
        }
        Log.i("imagePath : ",""+imagePath);
        return imagePath;
    }

    /**
     * @描述 查询图片的真实路径
     * */
    private String getImagePath(Uri uri, String selection) {
        String path = null;
        // 通过Uri和selection来获取真实的图片路径
        Cursor cursor = MainActivity.this.getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }



}
