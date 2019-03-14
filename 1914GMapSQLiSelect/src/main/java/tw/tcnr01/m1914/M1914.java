package tw.tcnr01.m1914;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import tw.tcnr01.m1914.providers.FriendsContentProvider;

public class M1914 extends AppCompatActivity implements OnMapReadyCallback ,
                                                                                                                            GoogleMap.OnMarkerClickListener,
                                                                                                                            GoogleMap.OnMyLocationButtonClickListener,
                                                                                                                            LocationListener{

    private static final String ACCESS_FINE_LOCATION = "101";
    private GoogleMap mMap;
    static LatLng VGPS = new LatLng(24.172127, 120.610313);
    int mapzoom = 12;
//    private static String[][] locations = {
//            {"MyHome", "24.1664196,120.6071264"},
//            {"中區職訓", "24.172127,120.610313"},
//            {"東海大學路思義教堂", "24.179051,120.600610"},
//            {"台中公園湖心亭", "24.144671,120.683981"},
//            {"秋紅谷", "24.1674900,120.6398902"},
//            {"台中火車站", "24.136829,120.685011"},
//            {"國立科學博物館", "24.1579361,120.6659828"}};

    private String[] MYCOLUMN = new String[]{"id", "name", "grp", "address"};

    private static String[] mapType = {
            "街道圖",
            "衛星圖",
            "地形圖",
            "混合圖",
            "開啟路況",
            "關閉路況"};

    private Spinner mSpnLocation, mSpnMapType;
    double dLat, dLon;
    private BitmapDescriptor image_des;// 圖標顯示
    private int icosel = 1; //圖示旗標
    //----------------
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 101;
    private LocationManager locationManager;
    String TAG = "TCNR09=>";
    private String provider; // 提供資料
    long minTime = 5000;// ms
    float minDist = 5.0f;// meter

    //---記錄軌跡-----------------------
    private ArrayList<LatLng> mytrace; // 追蹤我的位置


    //----------------
    //-----------------所需要申請的權限數組---------------
    private static final String[] permissionsArray = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private List<String> permissionsList = new ArrayList<String>();
    //申請權限後的返回碼
    private static final int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private TextView txtOutput,tmsg;
    private Marker markerMe;
    private int currentZoom=12;

    //---------------
    private ScrollView controlScroll;
    private CheckBox checkBox;
    private UiSettings mUiSettings;
    //--------------

    //-----自訂義-------
    int infowindow_ico;
    int showMarkerMeON = 0;

    private int resID = 0;
    private int resID1 = 0;

    // ========= Thread Hander =============
    private Handler mHandler = new Handler();
    private long timer = 20; // thread每幾秒run 多久更新一次資料
    private long timerang = 20; // 設定幾秒刷新Mysql
    private Long startTime = System.currentTimeMillis(); // 上回執行thread time
    private Long spentTime;
    //=============SQL Database================================
    private static ContentResolver mContRes;
    //----------------------------
    int DBConnectorError = 0;
    int MyspinnerNo = 0;
    int Spinnersel = 0;

    private String Myid = "0";
    private String Myname = "09號Castle";
    private String Myaddress = "24.172127,120.610313";
    private String Mygroup = "3"; //群組

    /***********************************************/
//    private String Selname = "我的位置";
//    private String Seladdress = "24.172127,120.610313";
    private String Selname;
    private String Seladdress;






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.m1914);

        //-----------------------------------------------
        StrictMode.setThreadPolicy(
                new StrictMode
                        .ThreadPolicy
                        .Builder()
                        .detectDiskReads()
                        .detectDiskWrites()
                        .detectNetwork()
                        .penaltyLog().build());
        StrictMode.setVmPolicy(
                new StrictMode
                        .VmPolicy
                        .Builder()
                        .detectLeakedSqlLiteObjects()
                        .penaltyLog()
                        .penaltyDeath()
                        .build());


//        checkRequiredPermission(this);     //  檢查SDK版本, 確認是否獲得權限.

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        //------------設定MapFragment-----------------------------------
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //-------------------------------------------------------
        u_checkgps();  //檢查GPS是否開啟
        setupViewComponent();

    }

    //--------------------
    private void setupViewComponent() {
        mSpnLocation = (Spinner) this.findViewById(R.id.spnLocation);
        mSpnMapType = (Spinner) this.findViewById(R.id.spnMapType);
        // ---------------

        txtOutput = (TextView) findViewById(R.id.txtOutput);
        tmsg = (TextView) findViewById(R.id.msg);
        // ---------------

        // ----設定control 控制鈕----------
        checkBox = (CheckBox) this.findViewById(R.id.checkcontrol);
        controlScroll = (ScrollView) this.findViewById(R.id.Scroll01);
        checkBox.setOnCheckedChangeListener(chklistener);
        controlScroll.setVisibility(View.INVISIBLE);
        // Parameters: 對應的三個常量值：VISIBLE=0 INVISIBLE=4 GONE=8


        icosel = 0;  //設定圖示初始值

        //-------檢查使用者是否存在--------------
        SelectMysql(Myname);
        //-------------------------------------
        // 設定Delay的時間
        mHandler.postDelayed(updateTimer, timer * 1000);
        // -------------------------

        Showspinner(); // 刷新spinner

//        // ---------------右邊
//        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
//                this, android.R.layout.simple_spinner_item);
//        for (int i = 0; i < locations.length; i++)
//            adapter.add(locations[i][0]);
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        mSpnLocation.setAdapter(adapter);
//        // 指定事件處理物件
//        mSpnLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                mMap.clear();
//                mytrace = null; //清除軌跡圖
//                showloc();
//
//                setMapLocation();
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//            }
//        });
//
//        // ---------------左邊
//        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
//        for (int i = 0; i < mapType.length; i++)
//            adapter.add(mapType[i]);
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        mSpnMapType.setAdapter(adapter);
//        //-----------設定ARGB 透明度----
////        mSpnMapType.setPopupBackgroundDrawable(new ColorDrawable(0x00FFFFFF)); //全透明
//        mSpnMapType.setPopupBackgroundDrawable(new ColorDrawable(0x99FFFFFF)); //60%透明
//        mSpnLocation.setPopupBackgroundDrawable(new ColorDrawable(0x99FFFFFF)); //60%透明
////        # ARGB依次代表透明度（alpha）、紅色(red)、綠色(green)、藍色(blue)
////        100% — FF       95% — F2        90% — E6        85% — D9
////        80% — CC        75% — BF        70% — B3        65% — A6
////        60% — 99        55% — 8C        50% — 80        45% — 73
////        40% — 66        35% — 59        30% — 4D        25% — 40
////        20% — 33        15% — 26        10% — 1A         5% — 0D         0% — 00
//        //---------------
//        mSpnMapType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                switch (position) {
//                    case 0:
//                        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL); // 道路地圖。
//                        break;
//                    case 1:
//                        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE); // 衛星空照圖
//                        break;
//                    case 2:
//                        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN); // 地形圖
//                        break;
//                    case 3:
//                        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID); // 道路地圖混合空照圖
//                        break;
//                    case 4:
//                        mMap.setTrafficEnabled(true); //開啟路況
//                        break;
//                    case 5:
//                        mMap.setTrafficEnabled(false); //關閉路況
//                        break;
//                }
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//            }
//        });


    }

    private void Showspinner() {
                // ---------------右邊
        /***************************************
         * 讀取SQLite => Spinner
         *****************************************/
        mContRes = getContentResolver();
        Cursor cur_Spinner =
                mContRes.query(FriendsContentProvider.CONTENT_URI, MYCOLUMN, null, null, null);
        cur_Spinner.moveToFirst();//一定要寫，不然會出錯

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        for (int i = 0; i < cur_Spinner.getCount(); i++) {
            cur_Spinner.moveToPosition(i);
            adapter.add(cur_Spinner.getString(1));
        }
        cur_Spinner.close();

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpnLocation.setAdapter(adapter);
        // 指定事件處理物件
        mSpnLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mMap.clear();
                mytrace = null; //清除軌跡圖
                showloc();

                setMapLocation();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // ---------------左邊
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        for (int i = 0; i < mapType.length; i++)
            adapter.add(mapType[i]);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpnMapType.setAdapter(adapter);
        //-----------設定ARGB 透明度----
//        mSpnMapType.setPopupBackgroundDrawable(new ColorDrawable(0x00FFFFFF)); //全透明
        mSpnMapType.setPopupBackgroundDrawable(new ColorDrawable(0x99FFFFFF)); //60%透明
        mSpnLocation.setPopupBackgroundDrawable(new ColorDrawable(0x99FFFFFF)); //60%透明
//        # ARGB依次代表透明度（alpha）、紅色(red)、綠色(green)、藍色(blue)
//        100% — FF       95% — F2        90% — E6        85% — D9
//        80% — CC        75% — BF        70% — B3        65% — A6
//        60% — 99        55% — 8C        50% — 80        45% — 73
//        40% — 66        35% — 59        30% — 4D        25% — 40
//        20% — 33        15% — 26        10% — 1A         5% — 0D         0% — 00
        //---------------
        mSpnMapType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL); // 道路地圖。
                        break;
                    case 1:
                        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE); // 衛星空照圖
                        break;
                    case 2:
                        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN); // 地形圖
                        break;
                    case 3:
                        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID); // 道路地圖混合空照圖
                        break;
                    case 4:
                        mMap.setTrafficEnabled(true); //開啟路況
                        break;
                    case 5:
                        mMap.setTrafficEnabled(false); //關閉路況
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

    }

    /************************************************
     * SQL Database
     ***********************************************/
    private void SelectMysql(String myname) {
        String selectMYSQL = "";
        String result = "";
        try {
            selectMYSQL = "SELECT * FROM member WHERE name = '" + myname + "' ORDER BY id";
            result = DBConnector.executeQuery(selectMYSQL);
            JSONArray jsonArray = new JSONArray(result);
            JSONObject jsonData = jsonArray.getJSONObject(0);
            Myid = jsonData.getString("id").toString();
            Myname = jsonData.getString("name").toString();
            Mygroup = jsonData.getString("grp").toString();
            Myaddress = jsonData.getString("address").toString();
//            }
        } catch (Exception e) {
            // Log.e("log_tag", e.toString());
        }
    }

    /************************************************
     * Thread Hander 固定要執行的方法
     ***********************************************/
    private final Runnable updateTimer = new Runnable() {
        @Override
        public void run() {
            spentTime = System.currentTimeMillis() - startTime;
            // startTime = System.currentTimeMillis();
            Long second = (spentTime / 1000);// 將運行時間後，轉換成秒數
            if (second >= timerang) {
                startTime = System.currentTimeMillis();
                dbmysql(); // 匯入database
                Showspinner(); // 刷新spinner
            }
            mHandler.postDelayed(this, timer * 1000);// time轉換成毫秒 updateTime
        }
    };

    private void dbmysql() {
        mContRes = getContentResolver();
        // --------------------------- 先刪除 SQLite 資料------------

        Cursor cur_dbmysql = mContRes.query(FriendsContentProvider.CONTENT_URI, MYCOLUMN, null, null, null);
        cur_dbmysql.moveToFirst(); // 一定要寫，不然會出錯

        // ------
        try {
            String result = DBConnector.executeQuery("SELECT * FROM member");
            if (result.length() <= 15) {//php找不到資料會回傳資料長度15,所以以15來判斷有沒有找到資料
                DBConnectorError++;//連線失敗次數
                if (DBConnectorError > 3)//連線失敗大於3次
                    Toast.makeText(M1914.this, "伺服器狀態異常,請檢查您的網路狀態!", Toast.LENGTH_LONG).show();
                else//連線失敗小於等於3次
                    Toast.makeText(M1914.this, "伺服器嘗試連線中,請稍候!", Toast.LENGTH_LONG).show();
            } else {//php找到資料,刪除SQLite資料
                DBConnectorError = 0;
                Uri uri = FriendsContentProvider.CONTENT_URI;
                mContRes.delete(uri, null, null); // 刪除所有資料
            }
            /** SQL 結果有多筆資料時使用JSONArray 只有一筆資料時直接建立JSONObject物件 JSONObject
             * jsonData = new JSONObject(result);  */
            JSONArray jsonArray = new JSONArray(result);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonData = jsonArray.getJSONObject(i);

                ContentValues newRow = new ContentValues();
                newRow.put("id", jsonData.getString("id").toString());
                newRow.put("name", jsonData.getString("name").toString());
                newRow.put("grp", jsonData.getString("grp").toString());
                newRow.put("address", jsonData.getString("address").toString());
                // ---------
//                    MyspinnerNo = i; // 儲存會員在spinner 的位置
                mContRes.insert(FriendsContentProvider.CONTENT_URI, newRow);
            }
        } catch (Exception e) {

        }
        cur_dbmysql.close();

    }

    // -----顯示 我的位置座標圖示
    public void setMyLocationLayerEnabled(View v) {
        if (!checkReady()) {
            return;
        }
        //----------取得定位許可-----------------------
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            //----顯示我的位置ICO-------
            mMap.setMyLocationEnabled(((CheckBox) v).isChecked());
        } else {
            Toast.makeText(getApplicationContext(), "GPS定位權限未允許", Toast.LENGTH_LONG).show();
        }
    }

    // ---檢查 Google Map 是否正確開啟
    private boolean checkReady() {
        if (mMap == null) {
            Toast.makeText(this, R.string.map_not_ready, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }


    //-------------監聽改變控制鈕 ------------
    private CheckBox.OnCheckedChangeListener chklistener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (checkBox.isChecked()) {
                controlScroll.setVisibility(View.VISIBLE);
                // Parameters: 對應的三個常量值：VISIBLE=0 INVISIBLE=4 GONE=8
            } else {
                controlScroll.setVisibility(View.INVISIBLE);
            }
        }
    };

    // ---Control 控制項設定--------------------------------
    private boolean isChecked(int id) {
        return ((CheckBox) findViewById(id)).isChecked();
    }

    // -------- 地圖縮放 -------------------------------------------
    public void setZoomButtonsEnabled(View v) {
        if (!checkReady()) {
            return;
        }
        // Enables/disables zoom controls (+/- buttons in the bottom right of
        // the map).
        mMap.getUiSettings().setZoomControlsEnabled(((CheckBox) v).isChecked());
    }

    // ---------------設定指北針----------------------------------------------
    public void setCompassEnabled(View v) {
        if (!checkReady()) {
            return;
        }
        // Enables/disables the compass (icon in the top left that indicates the
        // orientation of the
        // map).
        mMap.getUiSettings().setCompassEnabled(((CheckBox) v).isChecked());
    }

    //----可用捲動手勢操控,用手指平移或捲動來拖曳地圖
    public void setScrollGesturesEnabled(View v) {
        if (!checkReady()) {
            return;
        }
        // Enables/disables scroll gestures (i.e. panning the map).
        mMap.getUiSettings().setScrollGesturesEnabled(((CheckBox) v).isChecked());
    }

    //----縮放手勢按兩下按一下或兩指拉大拉小----
    public void setZoomGesturesEnabled(View v) {
        if (!checkReady()) {
            return;
        }
        // Enables/disables zoom gestures (i.e., double tap, pinch & stretch).
        mMap.getUiSettings().setZoomGesturesEnabled(((CheckBox) v).isChecked());
    }
    //----傾斜手勢改變地圖的傾斜角度兩指上下拖曳來增加/減少傾斜角度----
    public void setTiltGesturesEnabled(View v) {
        if (!checkReady()) {
            return;
        }
        // Enables/disables tilt gestures.
        mMap.getUiSettings().setTiltGesturesEnabled(((CheckBox) v).isChecked());

    }
    //----旋轉手勢兩指旋轉地圖----
    public void setRotateGesturesEnabled(View v) {
        if (!checkReady()) {
            return;
        }
        // Enables/disables rotate gestures.
        mMap.getUiSettings().setRotateGesturesEnabled(((CheckBox) v).isChecked());


    }





    private void setMapLocation() {
        showloc(); //刷新所有景點
        int iSelect = mSpnLocation.getSelectedItemPosition();
//        String[] sLocation = locations[iSelect][1].split(",");
        mContRes = getContentResolver();
        Cursor cur_setmap = mContRes.query(FriendsContentProvider.CONTENT_URI, MYCOLUMN, null, null,
                null);

        cur_setmap.moveToPosition(iSelect);
/**************************************
 * id: cur_setmap.getString(0) name: cur_setmap.getString(1) grp:
 * cur_setmap.getString(2) address:cur_setmap.getString(3)
 **************************************/
        Selname = cur_setmap.getString(1);// 地名
        Seladdress = cur_setmap.getString(3);// 緯經
        cur_setmap.close();
        String[] sLocation = Seladdress.split(",");

        double dLat = Double.parseDouble(sLocation[0]);    // 南北緯
        double dLon = Double.parseDouble(sLocation[1]);    // 東西經
//        String vtitle = locations[iSelect][0];//選擇的名字
        String vtitle = Selname;//選擇的名字

        //--- 設定所選位置之當地圖示 ---//
        image_des = BitmapDescriptorFactory
                .defaultMarker(BitmapDescriptorFactory.HUE_GREEN); //使用系統水滴Green
        VGPS = new LatLng(dLat, dLon);
        // --- 設定自訂義infowindow ---//
        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());
        mMap.setOnMarkerClickListener(this);
        // map.setOnInfoWindowClickListener(this);
        // map.setOnMarkerDragListener(this);

        // --- 根據所選位置項目顯示地圖/標示文字與圖片 ---//
        mMap.addMarker(new MarkerOptions()
                .position(VGPS)
                .title(vtitle)
                .snippet("座標:" + dLat + "," + dLon)
                .icon(image_des));// 顯示圖標文字
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(VGPS, mapzoom));
        onCameraChange(mMap.getCameraPosition());
        mMap.setOnMyLocationButtonClickListener(this);

    }
    //-----------onCameraChange--------------------------
    private void onCameraChange(CameraPosition cameraPosition) {
        tmsg.setText("目前Zoom:" + mMap.getCameraPosition().zoom);
    }


    //-------提示使用者開啟GPS定位------------------------------------
    private void u_checkgps() {
        // 取得系統服務的LocationManager物件
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        // 檢查是否有啟用GPS
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // 顯示對話方塊啟用GPS
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("定位管理")
                    .setMessage("GPS目前狀態是尚未啟用.\n"
                            + "請問你是否現在就設定啟用GPS?")
                    .setPositiveButton("啟用", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // 使用Intent物件啟動設定程式來更改GPS設定
                            Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(i);
                        }
                    })
                    .setNegativeButton("不啟用", null).create().show();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

    }

    private void checkRequiredPermission(Activity activity) {
        for (String permission : permissionsArray) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsList.add(permission);
            }
        }
        if (permissionsList.size() != 0) {
            ActivityCompat.requestPermissions(activity, permissionsList.toArray(new
                    String[permissionsList.size()]), REQUEST_CODE_ASK_PERMISSIONS);
        }
    }


        private void showloc() {
            Cursor cursholoc = mContRes.query(FriendsContentProvider.CONTENT_URI, MYCOLUMN, null, null, null);
        // 將所有景點位置顯示
//        for (int i = 0; i < locations.length; i++) {
//            String[] sLocation = locations[i][1].split(",");
            for (int i = 0; i < cursholoc.getCount(); i++) {
                cursholoc.moveToPosition(i);
                String[] sLocation = cursholoc.getString(3).split(",");
                dLat = Double.parseDouble(sLocation[0]); // 南北緯
                dLon = Double.parseDouble(sLocation[1]); // 東西經
                String vtitle = cursholoc.getString(1);
//            String vtitle = locations[i][0];
                resID = 0;
                resID1 = 0;

                // --- 設定所選位置之當地圖片 ---//
                // raw 目錄下 存放 q01.png ~ q06.png  t01.png ~t07.png 超出範圍 用 t99.png & q99.png
                if (i >= 0 && i < 8) {
                    String idName = "t" + String.format("%02d", i);
                    String imgName = "q" + String.format("%02d", i);
                    resID = getResources().getIdentifier(idName, "raw", getPackageName());
                    resID1 = getResources().getIdentifier(imgName, "raw", getPackageName());
                    image_des = BitmapDescriptorFactory.fromResource(resID);// 使用照片
                } else {
                    resID = getResources().getIdentifier("t99", "raw", getPackageName());
                    resID1 = getResources().getIdentifier("q99", "raw", getPackageName());
                }

                switch (icosel) {
                    case 0:
                        resID = getResources().getIdentifier("bbc64", "raw", getPackageName());
                        image_des = BitmapDescriptorFactory.fromResource(resID);// 使用照片
    //                    image_des = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED); // 使用色系統水滴
                        break;
                    case 1:
                        // 運用巨集
                        // --- 設定所選位置之當地圖片 ---
                        int resID = 0; //R 的機碼
                        // raw 目錄下 存放 t01.png ~t07.png 超出範圍 用 t99.png
                        if (i >= 0 && i < 8) {
                            String idName = "t" + String.format("%02d", i);
                            resID = getResources().getIdentifier(idName, "raw", getPackageName());
                        } else {
                            resID = getResources().getIdentifier("t99", "raw", getPackageName());
                        }

                        image_des = BitmapDescriptorFactory.fromResource(resID);// 使用照片
                        break;
                }
                //-----------------------------------------
                vtitle = vtitle + "#" + resID1; //存放圖片號碼
                //-----------------------------------------

                VGPS = new LatLng(dLat, dLon);// 更新成欲顯示的地圖座標
                mMap.addMarker(new MarkerOptions()
                        .position(VGPS)
                        .alpha(0.9f)
                        .title(i + "." +vtitle)
                        .snippet("緯度:" + String.valueOf(dLat) + "\n經度:" + String.valueOf(dLon))
                        .infoWindowAnchor(0.62f*2, 0.5f*2)
                        .icon(image_des));// 顯示圖標文字
                // .draggable(true) //設定maker 可移動
                //.infoWindowAnchor(0.5f, 0.9f)
                //--------------------使用自定義式窗-------------------------------------------------------
                mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());//外圓內方
            }

            cursholoc.close();

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //        mUiSettings = map.getUiSettings();//
//        開啟 Google Map 拖曳功能
//        mMap.getUiSettings().setScrollGesturesEnabled(true);
//        右下角的導覽及開啟 Google Map功能
//        mMap.getUiSettings().setMapToolbarEnabled(true);
//        左上角顯示指北針，要兩指旋轉才會出現
//        mMap.getUiSettings().setCompassEnabled(true);
//        右下角顯示縮放按鈕的放大縮小功能
//        mMap.getUiSettings().setZoomControlsEnabled(true);
        // --------------------------------
        mMap.addMarker(new MarkerOptions().position(VGPS).title("中區職訓"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(VGPS, mapzoom));
        //----------取得定位許可------------API 20-----------
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            //----顯示我的位置ICO-------
            Toast.makeText(getApplicationContext(), "GPS定位權限未允許", Toast.LENGTH_LONG).show();
        } else {
            //----顯示我的位置ICO-------
            mMap.setMyLocationEnabled(true);
            return;
        }
//-------------------------------------------------

    }



    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(getApplicationContext(), "返回GPS目前位置", Toast.LENGTH_LONG).show();
        return true;

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (initLocationProvider()) {
            nowaddress();
        } else {
            txtOutput.setText("GPS未開啟,請先開啟定位！");
        }

    }

    private void nowaddress() {
// 取得上次已知的位置
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Location location = locationManager.getLastKnownLocation(provider);
            updateWithNewLocation(location);
            return;
        }

// 監聽 GPS Listener----------------------------------
// long minTime = 5000;// ms
// float minDist = 5.0f;// meter
//---網路和GPS來取得定位，因為GPS精準度比網路來的更好，所以先使用網路定位、
// 後續再用GPS定位，如果兩者皆無開啟，則跳無法定位的錯誤訊息
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        Location location = null;
        if (!(isGPSEnabled || isNetworkEnabled))
            tmsg.setText("GPS 未開啟");
        else {
            if (isNetworkEnabled) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        minTime, minDist, locationListener);
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                tmsg.setText("使用網路GPS");
            }

            if (isGPSEnabled) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        minTime, minDist, locationListener);
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                tmsg.setText("使用精確GPS");
            }
        }

    }

    /**
     * 位置變更狀態監視
     */
    LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            updateWithNewLocation(location);
            Log.d(TAG, "locationListener->onLocationChanged:" + mMap.getCameraPosition().zoom + " currentZoom:"
                    + currentZoom);
            tmsg.setText("目前Zoom:" + mMap.getCameraPosition().zoom);
        }

        @Override
        public void onProviderDisabled(String provider) {
            updateWithNewLocation(null);
            Log.d(TAG, "onProviderDisabled");
        }

        @Override
        public void onProviderEnabled(String provider) {
            tmsg.setText("onProviderEnabled");
            Log.d(TAG, "onProviderEnabled");
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                case LocationProvider.OUT_OF_SERVICE:
                    Log.v(TAG, "Status Changed: Out of Service");
                    tmsg.setText("Out of Service");
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.v(TAG, "Status Changed: Temporarily Unavailable");
                    tmsg.setText("Temporarily Unavailable");
                    break;
                case LocationProvider.AVAILABLE:
                    Log.v(TAG, "Status Changed: Available");
                    tmsg.setText("Available");
                    break;
            }
        }

    };



    private void updateWithNewLocation(Location location) {
        String where = "";
        if (location != null) {

            double lng = location.getLongitude();// 經度
            double lat = location.getLatitude();// 緯度
            float speed = location.getSpeed();// 速度
            long time = location.getTime();// 時間
            String timeString = getTimeString(time);

            where = "經度: " + lng + "\n緯度: " + lat + "\n速度: " + speed + "\n時間: " + timeString + "\nProvider: "
                    + provider;

            // 標記"我的位置"
            showMarkerMe(lat, lng);
            cameraFocusOnMe(lat, lng);
            trackMe(lat, lng);// 軌跡圖

        } else {
            where = "*位置訊號消失*";
        }

        // 位置改變顯示
        txtOutput.setText(where);

    }

    //追蹤目前我的位置畫軌跡圖
    private void trackMe(double lat, double lng) {
        if (mytrace == null) {
            mytrace = new ArrayList<LatLng>();
        }
        mytrace.add(new LatLng(lat, lng));

        PolylineOptions polylineOpt = new PolylineOptions();
        for (LatLng latlng : mytrace) {
            polylineOpt.add(latlng);
        }
        polylineOpt.color(Color.YELLOW); // 軌跡顏色
        Polyline line = mMap.addPolyline(polylineOpt);
        line.setWidth(15); // 軌跡寬度
        line.setPoints(mytrace);
    }


    /***********************************************
     * timeInMilliseconds
     ***********************************************/
    private String getTimeString(long timeInMilliseconds) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(timeInMilliseconds);
    }


    private void cameraFocusOnMe(double lat, double lng) {
        CameraPosition camPosition = new CameraPosition.Builder()
                .target(new LatLng(lat, lng))
                .zoom(mMap.getCameraPosition().zoom)
                .build();
        /* 移動地圖鏡頭 */
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(camPosition));
        tmsg.setText("目前Zoom:" + mMap.getCameraPosition().zoom);

    }
    /*** 顯示目前位置 */
    private void showMarkerMe(double lat, double lng) {
        Log.d(TAG, "showMarkerMe");
        if (markerMe != null) {
            markerMe.remove();
        }
        //------------------
        int resID = getResources().getIdentifier("t00", "raw", getPackageName());
//------------------
        if (icosel != 0){
            image_des = BitmapDescriptorFactory.fromResource(resID);// 使用照片
        }else {
            image_des = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE); // 使用系統水滴
        }
//-------------------------

        dLat = lat; // 南北緯
        dLon = lng; // 東西經
        String vtitle = "GPS位置:";
        String vsnippet = "座標:" + String.valueOf(dLat) + "," + String.valueOf(dLon);
        VGPS = new LatLng(lat, lng);// 更新成欲顯示的地圖座標
        MarkerOptions markerOpt = new MarkerOptions();
        markerOpt.position(new LatLng(lat, lng));
        markerOpt.title(vtitle);
        markerOpt.snippet(vsnippet);
        markerOpt.infoWindowAnchor(0.5f, 0.9f);
        markerOpt.draggable(true);

        markerOpt.icon(image_des);
//        markerOpt.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        markerMe = mMap.addMarker(markerOpt);
//----------------------------
//        locations[0][1] = lat + "," + lng;

    }

    /* 檢查GPS 是否開啟 */
    private boolean initLocationProvider() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
            return true;
        }
        return false;

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        locationManager.removeUpdates(locationListener);
    }
    //-----------------
    @Override
    public boolean onMarkerClick(final Marker marker_Animation) {
        Log.d(TAG, "onMarkerClick:" + marker_Animation.getTitle()); //
        if (!marker_Animation.getTitle().substring(0, 4).equals("Move")) {
            // 設定動畫
            final Handler handler = new Handler();
            final long start = SystemClock.uptimeMillis();
            final long duration = 1500;//連續時間

            final Interpolator interpolator = new BounceInterpolator();

            handler.post(new Runnable() {
                @Override
                public void run() {
                    long elapsed = SystemClock.uptimeMillis() - start;
                    float t = Math.max(1 - interpolator.getInterpolation((float) elapsed / duration), 0);
                    marker_Animation.setAnchor(0.5f, 1.0f + 2 * t); //設定標的位置

                    if (t > 0.0) {
                        // Post again 16ms later.
                        handler.postDelayed(this, 16);
                    }
                }
            });
        } else {
            M1914.this.markerMe.hideInfoWindow();
        }
        return false;

    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


    //========自訂義infowindow class============//
    private class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
        @Override
        public View getInfoWindow(Marker marker) {
            // 依指定layout檔，建立地標訊息視窗View物件
            // --------------------------------------------------------------------------------------
            // 單一框
            // View infoWindow=
            // getLayoutInflater().inflate(R.layout.custom_info_window,
            // null);
            // 有指示的外框
            View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_content, null);
            infoWindow.setAlpha(0.5f);
            // ----------------------------------------------
            // 顯示地標title
            TextView title = ((TextView) infoWindow.findViewById(R.id.title));
            String[] ss = marker.getTitle().split("#");
            title.setText(ss[0]);
            // 顯示地標snippet
            TextView snippet = ((TextView) infoWindow.findViewById(R.id.snippet));
            snippet.setText(marker.getSnippet());
            // 顯示圖片
            ImageView imageview = ((ImageView) infoWindow.findViewById(R.id.content_ico));
            imageview.setImageResource(Integer.parseInt(ss[1]));

            return infoWindow;

        }

        @Override
        public View getInfoContents(Marker marker) {
            Toast.makeText(getApplicationContext(), "getInfoContents", Toast.LENGTH_LONG).show();
            return null;
        }

    }
    //// -------------subclass end------------------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.m1914, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.item1:
                mMap.clear();  //歸零
                if (icosel < 1) {
                    icosel = 1; //用照片顯示
                    showloc();
                } else
                    icosel = 0; //用水滴顯示
                showloc();
                break;
            case R.id.item3:
                //----
//                LatLng VGPS_3D = new LatLng(34.687404, 135.525763);//大阪城
//                LatLng VGPS_3D = new LatLng(25.0339979,121.5633886);//101 taipei
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(VGPS)//目標
                        .zoom(17)       //縮放 1：世界 5：地塊/大陸 10：城市 15：街道 20：建築物
                        .bearing(80)   //0:北 45:西北 90:西 135:西南 180:南 225:東南 270:東 315:東北
                        .tilt(5)       //傾斜度（檢視角度）0-90 0:正上方(0~15 最佳)
                        .build();       // Creates a CameraPosition from the builder
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                mMap.setBuildingsEnabled(true);
//                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID); // 衛星圖
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL); // 街道圖
                //----
                break;

            case R.id.item4:
                //----
//                LatLng VGPS_3D = new LatLng(34.687404, 135.525763);//大阪城
                LatLng VGPS_3D = new LatLng(35.6272534,139.8817705);//Desney
                CameraPosition cameraPosition2 = new CameraPosition.Builder()
                        .target(VGPS_3D)//目標
                        .zoom(17)       //縮放 1：世界 5：地塊/大陸 10：城市 15：街道 20：建築物
                        .bearing(80)   //0:北 45:西北 90:西 135:西南 180:南 225:東南 270:東 315:東北
                        .tilt(10)       //傾斜度（檢視角度）0-90 0:正上方(0~15 最佳)
                        .build();       // Creates a CameraPosition from the builder
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition2));
                mMap.setBuildingsEnabled(true);
                /***
                 * Google Map提供了五种地图模式：
                 *
                 * Normal（普通）模式：普通的道路地图，可显示人造的景观以及自然景观，比如河流等，道路信息可见。
                 * Hybrid（混合）模式：普通的道路地图中添加了卫星图像，道路信息可见。
                 * Satellite（卫星）模式：卫星图像，道路信息不可见。
                 * Terrain（地形）模式：展示地形数据，地图中包含等高线和透视的阴影，部分道路可见。
                 * None（空）模式：不显示任何地图图像。
                 * 可以通过GoogleMap的setMapType()方法设置地图模式：
                 * */
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID); // 衛星圖
//                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL); // 街道圖
                mMap.getUiSettings().setMapToolbarEnabled(true);
                //----
                break;

            case R.id.item5:
//                Intent it = new Intent();
//                it.setClass(M1914.this, M1914select.class);
//                startActivity(it);
                break;

            case R.id.action_settings:
                this.finish();
                break;
        }
        return super.onOptionsItemSelected(item);

    }

// ===================main class end ===========================

}
