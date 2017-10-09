package com.paul.gaodetest;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.trace.LBSTraceClient;
import com.amap.api.trace.TraceLocation;
import com.amap.api.trace.TraceStatusListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    MapView mMapView = null;
    AMap mAMap=null;
    TextView mTv2;
    TextView mTv;
    ScrollView mSv;
    Marker mMarker;


    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    //声明定位回调监听器
    public MyLocationListener mLocationListener;
    //声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;

    private MyLocationStyle myLocationStyle;
    private LBSTraceClient mTraceClient=null;//轨迹服务
    private TraceStatusListener mTraceListener;
    private Polyline mPolyline;
    ArrayList<AMapLocation> mLocList=new ArrayList<>();
    ArrayList<LatLng> mAvgLocList=new ArrayList<>();
    int mCoorInterval=5;
    private int mSpan=1000;//设置定位时间间隔
    private double mCurSpeed=0;
    private double mAvgSpeed=0;
    private double mTimeLength=0;
    private double mDistance=0;
    private boolean mStarted=false;
    private boolean mDragMap=true;
    private void init()
    {
        mCurSpeed=0;
        mAvgSpeed=0;
        mTimeLength=0;
        mDistance=0;
        mDragMap=true;
        mTv2=(TextView)findViewById(R.id.textView2);
        mTv=(TextView)findViewById(R.id.textView);
        mSv=(ScrollView)findViewById(R.id.ScrollView);
        mLocList.clear();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();//初始化自定义变量
        //获取地图控件引用
        mMapView = (MapView) findViewById(R.id.map);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mMapView.onCreate(savedInstanceState);
        mAMap=mMapView.getMap();

        mLocationListener = new MyLocationListener();
//初始化定位
        mLocationClient = new AMapLocationClient(getApplicationContext());
//设置定位回调监听
        mLocationClient.setLocationListener(mLocationListener);
//初始化AMapLocationClientOption对象
        mLocationOption = new AMapLocationClientOption();
//设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置定位间隔,单位毫秒,默认为2000ms，最低1000ms。
        mLocationOption.setInterval(mSpan);
        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);

        //显示轨迹，轨迹纠偏
        mTraceListener=new TraceStatusListener() {
            @Override
            public void onTraceStatus(List<TraceLocation> list, List<LatLng> list1, String s) {
                List<LatLng> latLngs = new ArrayList<LatLng>();
                for(LatLng ll:list1)//纠偏后的点集
                {
                    latLngs.add(ll);
                }
                mPolyline =mAMap.addPolyline(new PolylineOptions().
                        addAll(latLngs).width(10).color(Color.argb(255, 1, 1, 1)));
            }
        };
        mTraceClient = LBSTraceClient.getInstance(this.getApplicationContext());
        mTraceClient.startTrace(mTraceListener); //开始采集,需要传入一个状态回调监听。

    }
    //可以通过类implement方式实现AMapLocationListener接口，也可以通过创造接口类对象的方法实现
//以下为后者的举例：
    public class MyLocationListener implements AMapLocationListener{
        @Override
        public void onLocationChanged(AMapLocation amapLocation) {
            if (amapLocation != null) {
                if (amapLocation.getErrorCode() == 0) {
//可在其中解析amapLocation获取相应内容。
                    //计算统计数据：
                    mLocList.add(amapLocation);
                    mTimeLength+=(mSpan/1000);//秒
//                    LatLng last=new LatLng(mLocList.get(mLocList.size()-1).getLatitude(),mLocList.get(mLocList.size()-1).getLongitude());
//                    LatLng pre=new LatLng(mLocList.get(mLocList.size()-2).getLatitude(),mLocList.get(mLocList.size()-2).getLongitude());
                    if(mLocList.size()%mCoorInterval==0&&mLocList.size()>1)
                    {
                        List<AMapLocation> avgGroup=mLocList.subList(mLocList.size()-mCoorInterval-1,mLocList.size()-1);
                        double avgLat=0,avgLng=0;
                        for(AMapLocation loc:avgGroup)
                        {
                            avgLat+=loc.getLatitude();
                            avgLng+=loc.getLongitude();
                        }
                        avgLat/=mCoorInterval;
                        avgLng/=mCoorInterval;
                        LatLng point=new LatLng(avgLat,avgLng);
                        mAvgLocList.add(point);

                        if(mAvgLocList.size()>1)
                        {
                            LatLng last=mAvgLocList.get(mAvgLocList.size()-1);
                            LatLng pre=mAvgLocList.get(mAvgLocList.size()-2);
                            double shortDis=AMapUtils.calculateLineDistance(last,pre);
                            mDistance+= shortDis;
                            mCurSpeed= shortDis/(mSpan*mCoorInterval/1000);//米/秒
                            mAvgSpeed=mDistance/mTimeLength;
                            //输出统计数据
                            mTv2.setText("当前速度(m/s)："+mCurSpeed+
                                    "\n平均速度(m/s)："+mAvgSpeed+
                                    "\n总距离(m)："+mDistance+
                                    "\n总时长(s)："+mTimeLength+
                                    "\n\n经度："+amapLocation.getLongitude()+
                                    "\n纬度："+amapLocation.getLatitude()+
                                    "\n定位类型："+amapLocation.getLocationType()+
                                    "\n定位范围："+amapLocation.getAccuracy()+
                                    "\n位置信息："+amapLocation.getPoiName());

                            //添加历史记录：
                            Button btn=new Button(getApplicationContext());
                            btn.setHeight(60);
                            btn.setText("里程");
                            int id=(int)(mTimeLength);
                            btn.setId(id);//设置btn_id作为坐标列表索引
                            btn.setTextSize(10);
                            btn.setText("里程："+mDistance+
                            "\n瞬时速度："+mCurSpeed+
                            "\n平均速度："+mAvgSpeed+
                            "\n时间："+mLocList.get(mLocList.size()-1).getTime());
                            btn.setOnClickListener(new View.OnClickListener()
                            {
                                public void onClick(View v)
                                {
                                    if(mMarker!=null)
                                    {
                                        mMarker.remove();
                                    }
                                    Button btn=(Button)v;
                                    AMapLocation loc= mLocList.get(btn.getId());
                                    LatLng latLng = new LatLng(loc.getLatitude(),loc.getLongitude());
                                    mMarker = mAMap.addMarker(new MarkerOptions().position(latLng).title("Run!").snippet("DefaultMarker"));
                                }
                            });
                            mMapView.addView(btn);//将mMapView改为你的分页面

                        }

                        //如何去掉定位偏差造成的距离误判？10个点为一组取平均？

                    }
//                    amapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见定位类型表
//                    amapLocation.getLatitude();//获取纬度
//                    amapLocation.getLongitude();//获取经度
//                    amapLocation.getAccuracy();//获取精度信息
//                    amapLocation.getAddress();//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
//                    amapLocation.getCountry();//国家信息
//                    amapLocation.getProvince();//省信息
//                    amapLocation.getCity();//城市信息
//                    amapLocation.getDistrict();//城区信息
//                    amapLocation.getStreet();//街道信息
//                    amapLocation.getStreetNum();//街道门牌号信息
//                    amapLocation.getCityCode();//城市编码
//                    amapLocation.getAdCode();//地区编码
//                    amapLocation.getAoiName();//获取当前定位点的AOI信息
//                    amapLocation.getBuildingId();//获取当前室内定位的建筑物Id
//                    amapLocation.getFloor();//获取当前室内定位的楼层
//                    amapLocation.getGpsStatus();//获取GPS的当前状态
////获取定位时间
//                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                    Date date = new Date(amapLocation.getTime());
//                    df.format(date);
                }else {
                    //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                    mTv2.setText("location Error, ErrCode:"
                            + amapLocation.getErrorCode() + ", errInfo:"
                            + amapLocation.getErrorInfo());
                }
            }
        }
    }

    public void btn_followOnClick(View v)
    {
        if(!mDragMap){
            myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
            Button btn_fl=(Button)findViewById(R.id.button);
            btn_fl.setText("未跟踪");
            mDragMap=true;
        }
        else{
            myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
            Button btn_fl=(Button)findViewById(R.id.button);
            btn_fl.setText("已跟踪");
            mDragMap=false;
        }
    }

    public void btn_testOnClick(View v)
    {
        if(!mStarted){
            init();
            mTv2.setText("定位开启！请稍等15秒...");
            //显示定位点
            myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
            myLocationStyle.interval(mSpan); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
            myLocationStyle.strokeColor(Color.BLUE);
            mAMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
//aMap.getUiSettings().setMyLocationButtonEnabled(true);设置默认定位按钮是否显示，非必需设置。
            mAMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。

//启动定位
            mLocationClient.startLocation();
            mTv.setText("检测已开始...");
            mStarted=true;
            mAMap.moveCamera(CameraUpdateFactory.zoomTo(12));//设定缩放级别
        }
        else{
            mStarted=false;
            mLocationClient.stopLocation();
            mAMap.setMyLocationEnabled(false);
            mTv.setText("检测结束！");
            if(mPolyline!=null){
                mPolyline.remove();//清楚轨迹
            }
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        mLocationClient.stopLocation();
        mLocationClient.onDestroy();
        mAMap.setMyLocationEnabled(false);
        mMapView.onDestroy();
    }
    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        mMapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mMapView.onPause();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mMapView.onSaveInstanceState(outState);
    }
}
