package tw.tcnr01.m1901;

import android.Manifest;
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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
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

import tw.tcnr01.m1901.providers.FriendsContentProvider;

public class M1901 extends AppCompatActivity implements OnMapReadyCallback
        , LocationListener
        , GoogleMap.OnMyLocationButtonClickListener
        , GoogleMap.OnMarkerClickListener {

    private GoogleMap map;

    static LatLng VGPS = new LatLng(24.172127, 120.610313);
    int mapzoom = 12;
    // ========= map html ============
//    private static String[][] locations = {
//            {"我的位置", "24.172127,120.610313"},
//            {"中區職訓", "24.172127,120.610313"},
//            {"東海大學路思義教堂", "24.179051,120.600610"},
//            {"台中公園湖心亭", "24.144671,120.683981"},
//            {"秋紅谷", "24.1674900,120.6398902"},
//            {"台中火車站", "24.136829,120.685011"},
//            {"國立科學博物館", "24.1579361,120.6659828"},
//            {"路人1", "24.1569461,120.6750828"},
//            {"路人2", "24.1589561,120.6851828"},
//            {"路人3", "24.1599661,120.6952828"}
//    };
    private String[] MYCOLUMN = new String[]{"id", "name", "grp", "address"};

    private static String[] mapType = {"街道圖", "衛星圖", "地形圖", "混合圖", "開啟路況", "關閉路況"};
    private static BitmapDescriptor image_des;//圖標顯示
    private Spinner mSpnLocation, mSpnMapType;
    private int icosel; //圖示旗標

    double dLat, dLon;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINAL_LOCATION = 101;
    private LocationManager locationManager;
    private Location currentLocation;
    //----GPS------------
    private TextView txtOutput;
    private TextView tmsg;
    private Marker markerMe;
    private String provider; // 提供資料
    long minTime = 5000;// ms
    float minDist = 5.0f;// meter

    private ArrayList<LatLng> mytrace;//追蹤我的位置
    //---------------
    private ScrollView controlScroll;
    private CheckBox checkBox;
    private UiSettings mUiSettings;
    //---自訂義---------
    int infowindow_ico;
    int showMarkerMeON = 0;

    private int resID = 0;
    private int resID1 = 0;

    float Anchor_x;
    float Anchor_y;

    float infoAnchor_x = 0.5f;//水滴水平錨點
    float infoAnchor_y = 1.0f;

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

    private String Myname = "1號林繨菖";
    private String Myaddress = "24.172127,120.610313";
    private String Mygroup = "3"; //群組
    /***********************************************/
//    private String Selname = "我的位置";
//    private String Seladdress = "24.172127,120.610313";
    private String Selname;
    private String Seladdress;
    private String TAG = "tcnr01=>";

    //--------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.m1901);
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
        //--------設定MapFragment--------------------------------------------------------------------
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //-------------------------------------------------------------------------------
        dbmysql();
        u_checkgps();//檢查GPS是否開啟
        setupViewComponent();
    }

    private void setupViewComponent() {
        mSpnLocation = (Spinner) this.findViewById(R.id.spnLocation);
        mSpnMapType = (Spinner) this.findViewById(R.id.spnMapType);
        txtOutput = (TextView) findViewById(R.id.txtOutput);
        tmsg = (TextView) findViewById(R.id.msg);
        //---設定control控制鈕----------
        checkBox = (CheckBox) this.findViewById(R.id.checkcontrol);
        controlScroll = (ScrollView) this.findViewById(R.id.Scroll01);
        checkBox.setOnCheckedChangeListener(chklistener);
        controlScroll.setVisibility(View.INVISIBLE);
        //Parameters:對應的三個常量值: VISIBLE=0 INVISIBLE=4 GONE=8
        // ---------------
        icosel = 0; //設定圖示初始值
        //-------檢查使用者是否存在--------------
        SelectMysql(Myname);
        //-------------------------------------
        // 設定Delay的時間
        mHandler.postDelayed(updateTimer, timer * 1000);
        // -------------------------
        Showspinner(); // 刷新spinner
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

    private void Showspinner() {
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
        //指定事件處理物件
        mSpnLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                map.clear();
                mytrace = null;//清除軌跡圖
                showloc();
                setMapLocation();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        //---------------
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        for (int i = 0; i < mapType.length; i++)
            adapter.add(mapType[i]);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpnMapType.setAdapter(adapter);
        //-----------設定ARGB透明度----
        mSpnMapType.setPopupBackgroundDrawable(new ColorDrawable(0xF2FFFFFF));
        mSpnMapType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);//道路地圖。
                        break;
                    case 1:
                        map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);//衛星空照圖
                        break;
                    case 2:
                        map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);//地形圖
                        break;
                    case 3:
                        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);//道路地圖混合空照圖
                        break;
                    case 4://開啟路況
                        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);//道路地圖。
                        map.setTrafficEnabled(true);//交通路況圖
                        break;
                    case 5://關閉路況
                        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);//道路地圖。
                        map.setTrafficEnabled(false);//交通路況圖
                        break;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
    //-------------監聽改變控制鈕------------
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

    //-----Control控制項設定---------------
    private boolean isChecked(int id) {
        return ((CheckBox) findViewById(id)).isChecked();
    }

    //----檢查GoogleMap是否正確開啟---------
    private boolean checkReady() {
        if (map == null) {
            Toast.makeText(this, R.string.map_not_ready, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    //-----地圖縮放----------------------
    public void setZoomButtonsEnabled(View v) {
        if (!checkReady()) {
            return;
        }
        //Enables/disables zoom controls (+/-buttons in the bottom right of the map).
        map.getUiSettings().setZoomControlsEnabled(((CheckBox) v).isChecked());
    }

    //-----設定指北針-----------------------
    public void setCompassEnabled(View v) {
        if (!checkReady()) {
            return;
        }
        //Enables/disables the compass (icon in the top left that indicates the orientation of the map).
        map.getUiSettings().setCompassEnabled(((CheckBox) v).isChecked());
    }

    //---顯示 我的位置座標圖示
    public void setMyLocationLayerEnabled(View v) {
        if (!checkReady()) {
            return;
        }
        //----------取得定位許可-----------------------
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            //----顯示我的位置ICO-------
            map.setMyLocationEnabled(((CheckBox) v).isChecked());
        } else {
            Toast.makeText(getApplicationContext(), "GPS定位權限未允許", Toast.LENGTH_LONG).show();
        }
    }

    // ----可用手勢操控
    public void setScrollGesturesEnabled(View v) {
        if (!checkReady()) {
            return;
        }
        // Enables/disables scroll gestures (i.e. panning the map).
        map.getUiSettings().setScrollGesturesEnabled(((CheckBox) v).isChecked());
    }

    // ----按兩下按一下或兩指拉大拉小----
    public void setZoomGesturesEnabled(View v) {
        if (!checkReady()) {
            return;
        }
        // Enables/disables zoom gestures (i.e., double tap, pinch & stretch).
        map.getUiSettings().setZoomGesturesEnabled(((CheckBox) v).isChecked());
    }

    public void setTiltGesturesEnabled(View v) {
        if (!checkReady()) {
            return;
        }
        // Enables/disables tilt gestures.
        map.getUiSettings().setTiltGesturesEnabled(((CheckBox) v).isChecked());
    }

    public void setRotateGesturesEnabled(View v) {
        if (!checkReady()) {
            return;
        }
        // Enables/disables rotate gestures.
        map.getUiSettings().setRotateGesturesEnabled(((CheckBox) v).isChecked());
    }

    //-------------------------------------
    private void setMapLocation() {
        showloc(); //刷新所有景點
        int iSelect = mSpnLocation.getSelectedItemPosition();

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

        double dLat = Double.parseDouble(sLocation[0]); // 南北緯
        double dLon = Double.parseDouble(sLocation[1]); // 東西經
        String vtitle = Selname;//選擇的名字
        //--- 設定所選位置之當地圖示---//
        image_des = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE); //使用系統水滴
        //水滴可選擇樣式顏色
        VGPS = new LatLng(dLat, dLon);
        //----設定自訂義infowindow----//
        map.setInfoWindowAdapter(new CustomInfoWindowAdapter());//產生一個副calss在本class裡面
        map.setOnMarkerClickListener(this);
        //map.setOnInfoWindowClickListener(this);
        //map.setOnMarkerDragListener(this);
        map.addMarker(new MarkerOptions()
                .position(VGPS)
                .title(vtitle)
                .snippet("座標:" + dLat + "," + dLon)
                .infoWindowAnchor(Anchor_x, Anchor_y)
                .icon(image_des));//顯示圖標文字
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(VGPS, mapzoom));
        onCameraChange(map.getCameraPosition());//始終把圖放在中間
    }
    /*** onCameraChange */
    private void onCameraChange(CameraPosition cameraPosition) {
    }
    private void showloc() {
        Cursor cursholoc = mContRes.query(FriendsContentProvider.CONTENT_URI, MYCOLUMN, null, null, null);
        // 將所有景點位置顯示
        for (int i = 0; i < cursholoc.getCount(); i++) {
            cursholoc.moveToPosition(i);
            String[] sLocation = cursholoc.getString(3).split(",");
            dLat = Double.parseDouble(sLocation[0]); // 南北緯
            dLon = Double.parseDouble(sLocation[1]); // 東西經
            String vtitle = cursholoc.getString(1);
            resID = 0;//從R裡面抓出來的機碼(配置碼)
            resID1 = 0;
            // ---設定所選位置之當地圖片---//
            //drawable目錄下存放q01.png ~ q06.png t01.png ~t07.png 超出範圍用t99.png & q99.png
            if (i >= 0 && i < 7) {
                String idName = "t" + String.format("%02d", i);
                String imgName = "q" + String.format("%02d", i);
                resID = getResources().getIdentifier(idName, "drawable",
                        getPackageName());
                resID1 = getResources().getIdentifier(imgName, "drawable",
                        getPackageName());
                image_des = BitmapDescriptorFactory.fromResource(resID);// 使用照片
            } else {
                resID = getResources().getIdentifier("t99", "drawable",
                        getPackageName());//超出範圍 用t99.png
                resID1 = getResources().getIdentifier("q99", "drawable",
                        getPackageName());//超出範圍 用q99.png
            }
            // ---設定所選位置之當地圖片---//
            switch (icosel) {
                case 0:
                    image_des = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE); // 使用橘色系統水滴
                    Anchor_x = -1.6f;
                    Anchor_y = 1.0f;
                    break;
                case 1:
                    // 運用巨集
                    image_des = BitmapDescriptorFactory.fromResource(resID);// 使用照片
                    Anchor_x = -0.3f;
                    Anchor_y = 1.0f;
                    break;
            }
            vtitle = vtitle + "#" + resID1;//存放圖片號碼
            VGPS = new LatLng(dLat, dLon);// 更新成欲顯示的地圖座標
            //---根據所選位置項目顯示地圖/標示文字與圖片---//
            map.addMarker(new MarkerOptions()
                            .position(VGPS)
                            .alpha(0.9f)
                            .title(i + "." + vtitle)
                            .snippet("緯度:" + String.valueOf(dLat) + "\n經度:" + String.valueOf(dLon))
                            .infoWindowAnchor(Anchor_x, Anchor_y)//設定圖標的基點位置
                            .icon(image_des)// 顯示圖標文字
//                    .draggable(true)//設定marker可移動
            );
            //--------使用自定義視窗-------------------------
            map.setInfoWindowAdapter(new CustomInfoWindowAdapter());//外圓內方
        }
        cursholoc.close();
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
//        mUiSettings = map.getUiSettings();//
//        開啟 Google Map 拖曳功能
        map.getUiSettings().setScrollGesturesEnabled(true);
//        右下角的導覽及開啟 Google Map功能
        map.getUiSettings().setMapToolbarEnabled(true);
//        左上角顯示指北針，要兩指旋轉才會出現
        map.getUiSettings().setCompassEnabled(true);
//        右下角顯示縮放按鈕的放大縮小功能
        map.getUiSettings().setZoomControlsEnabled(true);

        // --------------------------------
        map.addMarker(new MarkerOptions().position(VGPS).title("中區職訓"));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(VGPS, mapzoom));
        //------------------取得許可-----------------------------------------
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
            return;
        } else {
            Toast.makeText(getApplicationContext(), "GPS定位權限未允許", Toast.LENGTH_LONG).show();
        }
//------------------------------------------------------------
        //秀3D map
        map.setBuildingsEnabled(true);
    }
    private void changeCamera(CameraUpdate update) {
        changeCamera(update, null);
    }

    private void changeCamera(CameraUpdate update, GoogleMap.CancelableCallback callback) {
        map.animateCamera(update, callback);
    }
    private void u_checkgps() {
        //取得系統服務的LocationManager物件
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        //檢查是否有啟用GPS
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            //險是對話方塊啟用GPS
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("定位管理")
                    .setMessage("GPS目前狀態是尚未啟用.\n" + "請問你是否現在就設定啟用GPS?")
                    .setPositiveButton("啟用", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //使用Intent物件啟動設定程式來更改GPS設定
                            Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(i);
                        }
                    })
                    .setNegativeButton("不啟用", null).create().show();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINAL_LOCATION);
        }
    }
// ============ GPS =================
    /*** onMyLocationButtonClick */
    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(getApplicationContext(), "返回GPS目前位置", Toast.LENGTH_SHORT).show();
        return false;//使用false則用內定返回現在位置
    }

    private boolean initLocationProvider() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
            return true;
        }
        return false;
    }

    //更新現在的位置
    private void updatePosition() {
        if (currentLocation == null) {
            txtOutput.setText("取得定位資訊中...");
        }
    }

    /*** 位置變更狀態監視*/
    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            updateWithNewLocation(location);
            tmsg.setText("目前Zoom:" + map.getCameraPosition().zoom);
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                case LocationProvider.OUT_OF_SERVICE:
                    tmsg.setText("Out of Service");
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    tmsg.setText("Temporarily Unavailable");
                    break;
                case LocationProvider.AVAILABLE:
                    tmsg.setText("Available");
                    break;
            }
        }
        @Override
        public void onProviderEnabled(String provider) {
            tmsg.setText("onProviderEnabled");
        }
        @Override
        public void onProviderDisabled(String provider) {
            updateWithNewLocation(null);
        }
    };
    //--------------------------------------------
    @Override
    public void onLocationChanged(Location location) {
    }
    @Override
    public void onProviderDisabled(String provider) {
    }
    @Override
    public void onProviderEnabled(String provider) {
    }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }
    private void nowaddress() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            Location location = locationManager.getLastKnownLocation(provider);
            updateWithNewLocation(location);
            return;
        }

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
    private void updateWithNewLocation(Location location) {
        String where = "";
        if (location != null) {
            double lng = location.getLongitude();//經度
            double lat = location.getLatitude();//緯度
            float speed = location.getSpeed();//速度
            long time = location.getTime();//時間
            String timeString = getTimeString(time);

            where = "經度:" + lng + "\n緯度:" + lat + "\n速度:" + speed + "\n時間:" + timeString + "\nProvider:" + provider;
            Myaddress = lat + "," + lng;
            //標記"我的位置"
            showMarkerMe(lat, lng);
            cameraFocusOnMe(lat, lng);
            trackMe(lat, lng);//軌跡圖
        } else {
            where = "*位置訊號消失*";
        }
        //位置改變顯示
        txtOutput.setText(where);
    }

    /*追蹤目前我的位置畫軌跡圖*/
    private void trackMe(double lat, double lng) {
        if (mytrace == null) {
            mytrace = new ArrayList<LatLng>();
        }
        mytrace.add(new LatLng(lat, lng));

        PolylineOptions polylineOpt = new PolylineOptions();
        for (LatLng latlng : mytrace) {
            polylineOpt.add(latlng);
        }

        polylineOpt.color(Color.RED);//軌跡顏色

        Polyline line = map.addPolyline(polylineOpt);//畫線
        line.setWidth(15);//軌跡寬度
        line.setPoints(mytrace);
    }

    /*** cameraFocusOnMe */
    private void cameraFocusOnMe(double lat, double lng) {
        CameraPosition camPosition = new CameraPosition.Builder().target(new LatLng(lat, lng)).zoom(map.getCameraPosition().zoom).build();
        /*移動地圖鏡頭*/
        map.animateCamera(CameraUpdateFactory.newCameraPosition(camPosition));
        tmsg.setText("目前Zoom:" + map.getCameraPosition().zoom);
    }

    //*** 顯示目前位置*/
    private void showMarkerMe(double lat, double lng) {
        if (markerMe != null) {
            markerMe.remove();
        }
        int resID = getResources().getIdentifier("z00", "drawable", getPackageName());
//------------------
        if (icosel != 0) {
            image_des = BitmapDescriptorFactory.fromResource(resID);//使用照片
        } else {
            image_des = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET);//使用系統水滴
        }
//-------------------------
        dLat = lat; // 南北緯
        dLon = lng; // 東西經
        String vtitle = "GPS位置:" + "#" + resID;
        String vsnippet = "座標:" + String.valueOf(dLat) + "," + String.valueOf(dLon);
        VGPS = new LatLng(lat, lng);// 更新成欲顯示的地圖座標
        MarkerOptions markerOpt = new MarkerOptions();
        markerOpt.position(new LatLng(lat, lng));
        markerOpt.title(vtitle);
        markerOpt.snippet(vsnippet);
        markerOpt.infoWindowAnchor(Anchor_x, Anchor_y);
        markerOpt.draggable(true);
        markerOpt.icon(image_des);
        markerMe = map.addMarker(markerOpt);
    }

    //**增加Marker監聽 使用Animation動畫**/
    @Override
    public boolean onMarkerClick(final Marker marker_Animation) {
        if (!marker_Animation.getTitle().substring(0, 4).equals("Move")) {
            //非GPS移動位置;
            //設定動畫
            final Handler handler = new Handler();
            final long start = SystemClock.uptimeMillis();
            final long duration = 1500;//連續時間
            final Interpolator interpolator = new BounceInterpolator();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    long elapsed = SystemClock.uptimeMillis() - start;
                    float t = Math.max(1 - interpolator.getInterpolation((float) elapsed / duration), 0);
                    marker_Animation.setAnchor(infoAnchor_x, infoAnchor_y + 2 * t);//設定標的位置
                    if (t > 0.0) {
                        //Post again 16ms later.
                        handler.postDelayed(this, 16);
                    }
                }
            });
        } else {//GPS移動不做動畫
            M1901.this.markerMe.hideInfoWindow();//不秀InfoWindow
        }
        return false;
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

    /***********************************************
     * timeInMilliseconds
     ***********************************************/
    private String getTimeString(long timeInMilliseconds) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(timeInMilliseconds);
    }

    private void dbmysql() {
        mContRes = getContentResolver();
        // --------------------------- 先刪除 SQLite 資料------------

        Cursor cur_dbmysql = mContRes.query(FriendsContentProvider.CONTENT_URI, MYCOLUMN, null, null, null);
        cur_dbmysql.moveToFirst(); // 一定要寫，不然會出錯

        // ------
        try {
            String result = DBConnector.executeQuery("SELECT * FROM member");
            if (result.length() <= 7) {//php找不到資料會回傳7,所以以7來判斷有沒有找到資料
                DBConnectorError++;//連線失敗次數
                if (DBConnectorError > 3)//連線失敗大於3次
                    Toast.makeText(M1901.this, "伺服器狀態異常,請檢查您的網路狀態!", Toast.LENGTH_LONG).show();
                else//連線失敗小於等於3次
                    Toast.makeText(M1901.this, "伺服器嘗試連線中,請稍候!", Toast.LENGTH_LONG).show();
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

    @Override
    protected void onStart() {
        super.onStart();
        if (initLocationProvider()) {
            nowaddress();
        } else {
            txtOutput.setText("GPS未開啟，請先開啟定位!");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        locationManager.removeUpdates(locationListener);
    }


    //-----------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.m1901, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (item.getItemId()) {
            case R.id.item1:
                map.clear(); //歸零
                if (icosel < 1) {
                    icosel = 1; //用照片顯示
                    showloc();
                } else
                    icosel = 0; //用水滴顯示
                showloc();
                break;
            case R.id.item3:
                //----
                CameraPosition Taipei101 = new CameraPosition.Builder()
                        .target(new LatLng(25.0339640, 121.5644720))//目標 台北101
                        .zoom(16.0f)       //縮放 1：世界 5：地塊/大陸 10：城市 15：街道 20：建築物
                        .bearing(180)   //0:北  45:西北  90:西  135:西南  180:南 225:東南 270:東 315:東北
                        .tilt(5)       //傾斜度（檢視角度）0-90 0:正上方(0~15 最佳)
                        .build();       // Creates a CameraPosition from the builder
                map.animateCamera(CameraUpdateFactory.newCameraPosition(Taipei101));
                map.setBuildingsEnabled(true);
                map.setMapType(GoogleMap.MAP_TYPE_SATELLITE); // 衛星空照圖
                //----
                break;
            case R.id.item4:
                onMapReady(map);
                map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                break;
            case R.id.action_settings:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //====================================================================================
    private class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
        @Override
        public View getInfoWindow(Marker marker) {
            //依指定layout檔，建立地標訊息視窗View物件
            //-----------------------------------
            //單一框
//            View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_window,null);
            //有指示的外框
            View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_content, null);
            infoWindow.setAlpha(0.5f);
            //------------------------------------
            //顯示地標title
            TextView title = ((TextView) infoWindow.findViewById(R.id.title));
            String[] ss = marker.getTitle().split("#");
            title.setText(ss[0]);
            //顯示地標snippet
            TextView snippet = ((TextView) infoWindow.findViewById(R.id.snippet));
            snippet.setText(marker.getSnippet());
            //顯示圖片
            ImageView imageview = ((ImageView) infoWindow.findViewById(R.id.content_ico));
            imageview.setImageResource(Integer.parseInt(ss[1]));
            return infoWindow;
        }

        @Override
        public View getInfoContents(Marker marker) {
            Toast.makeText(getApplicationContext(), "getInfoContents", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

}


