package com.example.owner.mykjbbus;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    TextView busNm;
    Button posBtn;
    TextView line_id, line_name, dir_up_name, dir_down_name, first_run_time, last_run_time, run_interval, line_kind;
    String LINE_NUM;
    String LINE_ID_FINAL;

    ListView listView;

    String strUrl;

    ArrayList<ArrivalBus> arrivalBuses = new ArrayList<ArrivalBus>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        busNm = (TextView) findViewById(R.id.busNumber);
        posBtn = (Button) findViewById(R.id.posBtn);
        line_id = (TextView) findViewById(R.id.busid);
        line_name = (TextView) findViewById(R.id.busname);
        dir_up_name = (TextView) findViewById(R.id.bussplace);
        dir_down_name = (TextView) findViewById(R.id.buseplace);
        first_run_time = (TextView) findViewById(R.id.busftime);
        last_run_time = (TextView) findViewById(R.id.busltime);
        run_interval = (TextView) findViewById(R.id.businterval);
        line_kind = (TextView) findViewById(R.id.buskind);

        listView = (ListView) findViewById(R.id.list);
        String[] values = new String[]{"[간선]송정33", "[급행]수완03", "[간선]금호46", "[간선]봉선37", "[STOP]수완우미린2차", "[STOP]호남대광산캠퍼스", "[STOP]목련마을6단지", "[STOP]우산시영2단지(동)", "[STOP]목련마을8단지", "[STOP]광주송정역"};
        final String[] busNumberValue = new String[]{"26", "4", "34", "29", "3807", "2728", "694", "848", "3276", "817"}; //BUSSTOP_ID

        if(values.length!=busNumberValue.length){
            Toast.makeText(getApplicationContext(),"Please confirm inputs : "+String.valueOf(values.length)+", "+String.valueOf(busNumberValue.length),Toast.LENGTH_SHORT).show();
        } else {
            Log.i("YWLEE", "Input data is good");
        }
        // 33:108, 03:114, 46:95, 37:19 LINE_ID

        // ListView 사용방법
        // 1. Define a new Adapter
        // 2. First parameter - Context
        // 3. Second parameter - Layout for the row
        // 4. Third parameter - ID of the TextView to which the data is written
        // 5. Forth - the Array of data

        Log.i("YWLEE", "MainActivity : " + "ListView Start");

        //개념
        //ListVIew를 이용하여 0-3번까지는 버스종류이고, 4-9는 정류소 정보임
        //버스 종류를 이용하여 버스 위치를 파악하고
        //정류소 정보를 누르면 정류소에 도착하는 버스를 알 수 있음

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, values);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int itemPosition = position;
                String itemValue = (String) listView.getItemAtPosition(position);
                LINE_NUM = busNumberValue[position];

                strUrl = decideSearchInfo(position, LINE_NUM);

                if (position <= 3) {
                    DownloadKJBusWebpageTask busInfoJob = new DownloadKJBusWebpageTask();
                    busInfoJob.execute(strUrl);
                } else {
                    DownloadArrivalWebpageTask arrivalInfoJob = new DownloadArrivalWebpageTask();
                    arrivalInfoJob.execute(strUrl);
                }
            }
        });

        //버튼을 누르면 지도위에 버스 위치를 표시함.
        //이를 위하여 MapsActivity를 사용함
        //이때 국토교통부 버스위치정보 API를 접근하면서 그형식은  광주는 KJB+BUS ID 임
        //예를 들면 송정
        posBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
                intent.putExtra("BUS", "KJB" + LINE_ID_FINAL);
                startActivity(intent);
            }
        });
    }

    //광주광역시 BIS 노선정보
//    <LINE NUM="1">
//    <LINE_ID>112</LINE_ID>
//    <LINE_NAME>순환01(운천저수지행)</LINE_NAME>
//    <DIR_UP_NAME>상무지구</DIR_UP_NAME>
//    <DIR_DOWN_NAME>상무지구</DIR_DOWN_NAME>
//    <LINE_KIND>1</LINE_KIND>
//    </LINE>

    private class DownloadKJBusWebpageTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            try {
                return (String) downloadUrl((String) urls[0]);
            } catch (IOException e) {
                return "다운로드 실패";
            }
        }

        protected void onPostExecute(String result) {

//            line_id.setText(result);

            String LINE = "";

            String LINE_ID = "";
            String LINE_NAME = "";
            String DIR_UP_NAME = "";
            String DIR_DOWN_NAME = "";
            String FIRST_RUN_TIME = "";
            String LAST_RUN_TIME = "";
            String RUN_INTERVAL = "";
            String LINE_KIND = "";

            boolean bSet_LINE = false;

            boolean bSet_LINE_ID = false;
            boolean bSet_LINE_NAME = false;
            boolean bSet_DIR_UP_NAME = false;
            boolean bSet_DIR_DOWN_NAME = false;
            boolean bSet_FIRST_RUN_TIME = false;
            boolean bSet_LAST_RUN_TIME = false;
            boolean bSet_RUN_INTERVAL = false;
            boolean bSet_LINE_KIND = false;

            String lineName = "";

            ///// (1) Bus Route ID /////
            //광주광역시 BIS 노선정보
//            tv.append("===== 노선ID =====\n");
            int count = 0;
            try {
                //1. XmlPullParserFactory 생성
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                //2. XmlPullParser 생성
                XmlPullParser xpp = factory.newPullParser();
                //3. parser에 XML을 넣음
                xpp.setInput(new StringReader(result));

                //4. parser event를 저장할 변수 생성
                int eventType = xpp.getEventType();

                //5. parser event에 따른 실행할 소스 작성
                while (eventType != XmlPullParser.END_DOCUMENT) {

                    switch (eventType) {
                        case XmlPullParser.START_DOCUMENT:
                            break;
                        case XmlPullParser.START_TAG:
                            String tag = xpp.getName();
                            if (tag.equals("LINE")) {
                                bSet_LINE = true;
                                LINE = xpp.getAttributeValue(null, "NUM");
                            } else if (tag.equals("LINE_ID")) {
                                bSet_LINE_ID = true;
                            } else if (tag.equals("LINE_NAME")) {
                                bSet_LINE_NAME = true;
                            } else if (tag.equals("DIR_UP_NAME")) {
                                bSet_DIR_UP_NAME = true;
                            } else if (tag.equals("DIR_DOWN_NAME")) {
                                bSet_DIR_DOWN_NAME = true;
                            } else if (tag.equals("FIRST_RUN_TIME")) {
                                bSet_FIRST_RUN_TIME = true;
                            } else if (tag.equals("LAST_RUN_TIME")) {
                                bSet_LAST_RUN_TIME = true;
                            } else if (tag.equals("RUN_INTERVAL")) {
                                bSet_RUN_INTERVAL = true;
                            } else if (tag.equals("LINE_KIND")) {
                                bSet_LINE_KIND = true;
                            }
                            break;
                        case XmlPullParser.TEXT:

                            if (LINE.equals(LINE_NUM)) {

                                if (bSet_LINE_ID) {
                                    LINE_ID = xpp.getText().trim();
                                    bSet_LINE_ID = false;
                                    line_id.setText(LINE_ID);
                                    LINE_ID_FINAL = LINE_ID;
                                } else if (bSet_LINE_NAME) {
                                    LINE_NAME = xpp.getText().trim();
                                    bSet_LINE_NAME = false;
                                    line_name.setText(LINE_NAME);
                                    busNm.setText(LINE_NAME);
                                } else if (bSet_DIR_UP_NAME) {
                                    DIR_UP_NAME = xpp.getText().trim();
                                    bSet_DIR_UP_NAME = false;
                                    dir_up_name.setText(DIR_UP_NAME);
                                } else if (bSet_DIR_DOWN_NAME) {
                                    DIR_DOWN_NAME = xpp.getText().trim();
                                    bSet_DIR_DOWN_NAME = false;
                                    dir_down_name.setText(DIR_DOWN_NAME);
                                } else if (bSet_FIRST_RUN_TIME) {
                                    FIRST_RUN_TIME = xpp.getText().trim();
                                    bSet_FIRST_RUN_TIME = false;
                                    first_run_time.setText(FIRST_RUN_TIME);
                                } else if (bSet_LAST_RUN_TIME) {
                                    LAST_RUN_TIME = xpp.getText().trim();
                                    bSet_LAST_RUN_TIME = false;
                                    last_run_time.setText(LAST_RUN_TIME);
                                } else if (bSet_RUN_INTERVAL) {
                                    RUN_INTERVAL = xpp.getText().trim();
                                    bSet_RUN_INTERVAL = false;
                                    run_interval.setText(RUN_INTERVAL);
                                } else if (bSet_LINE_KIND) {
                                    LINE_KIND = xpp.getText().trim();
                                    bSet_LINE_KIND = false;
                                    line_kind.setText(LINE_KIND);
                                    Log.i("YWLEE", "DownloadKJBusWebpageTask_PostExecute");
                                }
                            }
                            break;
                        case XmlPullParser.END_TAG:
                            break;
                    }
                    eventType = xpp.next();
                }
            } catch (Exception e) {
                ;
            }
        }
    }


    private class DownloadArrivalWebpageTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            try {
                return (String) downloadUrl((String) urls[0]);
            } catch (IOException e) {
                return "다운로드 실패";
            }
        }

        protected void onPostExecute(String result) {

            //버스 객체를 저장하는 ArraryList에 정거장이 바뀔때 마다 리세트 해야됨
            if (arrivalBuses.isEmpty()) {
            } else {
                arrivalBuses.clear();
            }

            String LINE_ID = "";
            String LINE_NAME = "";
            String BUS_ID = "";
            String CURR_STOP_ID = "";
            String BUSSTOP_NAME = "";
            String REMAIN_MIN = "";
            String REMAIN_STOP = "";
            String DIR_START = "";
            String DIR_END = "";

            boolean bSet_LINE_ID = false;
            boolean bSet_LINE_NAME = false;
            boolean bSet_BUS_ID = false;
            boolean bSet_CURR_STOP_ID = false;
            boolean bSet_BUSSTOP_NAME = false;
            boolean bSet_REMAIN_MIN = false;
            boolean bSet_REMAIN_STOP = false;
            boolean bSet_DIR_START = false;
            boolean bSet_DIR_END = false;

            int count = 0;
            ArrivalBus newBus = null;
            try {
                //1. XmlPullParserFactory 생성
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                //2. XmlPullParser 생성
                XmlPullParser xpp = factory.newPullParser();
                //3. parser에 XML을 넣음
                xpp.setInput(new StringReader(result));

                //4. parser event를 저장할 변수 생성
                int eventType = xpp.getEventType();

                //5. parser event에 따른 실행할 소스 작성
                while (eventType != XmlPullParser.END_DOCUMENT) {


                    switch (eventType) {
                        case XmlPullParser.START_DOCUMENT:
                            Log.i("YWLEE", "S--------------------------S");
                            break;
                        case XmlPullParser.START_TAG:
                            String tag = xpp.getName();
                            if (tag.equals("ARRIVE")) {
                                newBus = new ArrivalBus();
                                count++;
                            }
                            if (tag.equals("LINE_ID")) {
                                bSet_LINE_ID = true;
                            } else if (tag.equals("LINE_NAME")) {
                                bSet_LINE_NAME = true;
                            } else if (tag.equals("BUS_ID")) {
                                bSet_BUS_ID = true;
                            } else if (tag.equals("CURR_STOP_ID")) {
                                bSet_CURR_STOP_ID = true;
                            } else if (tag.equals("BUSSTOP_NAME")) {
                                bSet_BUSSTOP_NAME = true;
                            } else if (tag.equals("REMAIN_MIN")) {
                                bSet_REMAIN_MIN = true;
                            } else if (tag.equals("REMAIN_STOP")) {
                                bSet_REMAIN_STOP = true;
                            } else if (tag.equals("DIR_START")) {
                                bSet_DIR_START = true;
                            } else if (tag.equals("DIR_END")) {
                                bSet_DIR_END = true;
                            }
                            break;
                        case XmlPullParser.TEXT:
                            if (bSet_LINE_ID) {
                                LINE_ID = xpp.getText().trim();
                                bSet_LINE_ID = false;
                                newBus.setLINE_ID(LINE_ID);
                                Log.i("YWLEE", "LINE_ID =" + LINE_ID);
                            } else if (bSet_LINE_NAME) {
                                LINE_NAME = xpp.getText().trim();
                                bSet_LINE_NAME = false;
                                newBus.setLINE_NAME(LINE_NAME);
                                Log.i("YWLEE", "LINE_NAME =" + LINE_NAME);
                            } else if (bSet_BUS_ID) {
                                BUS_ID = xpp.getText().trim();
                                bSet_BUS_ID = false;
                                newBus.setBUS_ID(BUS_ID);
                                Log.i("YWLEE", "BUS_ID =" + BUS_ID);
                            } else if (bSet_CURR_STOP_ID) {
                                CURR_STOP_ID = xpp.getText().trim();
                                bSet_CURR_STOP_ID = false;
                                newBus.setCURR_STOP_ID(CURR_STOP_ID);
                                Log.i("YWLEE", "CURR_STOP_ID =" + CURR_STOP_ID);
                            } else if (bSet_BUSSTOP_NAME) {
                                BUSSTOP_NAME = xpp.getText().trim();
                                bSet_BUSSTOP_NAME = false;
                                newBus.setBUSSTOP_NAME(BUSSTOP_NAME);
                                Log.i("YWLEE", "BUSSTOP_NAME =" + BUSSTOP_NAME);
                            } else if (bSet_REMAIN_MIN) {
                                REMAIN_MIN = xpp.getText().trim();
                                bSet_REMAIN_MIN = false;
                                newBus.setREMAIN_MIN(REMAIN_MIN);
                                Log.i("YWLEE", "REMAIN_MIN =" + REMAIN_MIN);
                            } else if (bSet_REMAIN_STOP) {
                                REMAIN_STOP = xpp.getText().trim();
                                bSet_REMAIN_STOP = false;
                                newBus.setREMAIN_STOP(REMAIN_STOP);
                                Log.i("YWLEE", "REMAIN_STOP =" + REMAIN_STOP);
                            } else if (bSet_DIR_START) {
                                DIR_START = xpp.getText().trim();
                                bSet_DIR_START = false;
                                newBus.setDIR_START(DIR_START);
                                Log.i("YWLEE", "DIR_START =" + DIR_START);
                            } else if (bSet_DIR_END) {
                                DIR_END = xpp.getText().trim();
                                bSet_DIR_END = false;
                                newBus.setDIR_END(DIR_END);
                                Log.i("YWLEE", "DIR_END =" + DIR_END);
                                Log.i("YWLEE", "E--------------------------E");
                                //마지막 데이터가 저장된 시점인 여기.....
                                arrivalBuses.add(newBus); //여기에서 객체를 리스트에 저장해야됨
//                                Log.i("YWLEE", "--------------------------");
                            }


                            break;
                        case XmlPullParser.END_TAG:
                            break;
                    }

                    eventType = xpp.next();
                }
            } catch (Exception e) {
//                tv.setText(e.getMessage());
            }

            Log.i("YWLEE", "count = " + count);
            Log.i("YWLEE", "도착 버스 수 : " + Integer.toString(arrivalBuses.size()));

            //버스 도착 알림 Activity 호출
            //여기서 중요한 것은 ArrayList를 전달하는 것임.
            //사례를 잘보고 다음에 활용할 것.
            Intent displayIntent = new Intent(MainActivity.this, ArrivalDisplayActivity.class);
            displayIntent.putParcelableArrayListExtra("BusArrival", arrivalBuses);
            startActivity(displayIntent);
        }
    }

    public String decideSearchInfo(int position, String data) {
        String sUrl, sKey, searchUrl;

        if (position <= 3) {
            //광주광역시 BIS 노선 정보
            sUrl = "http://api.gjcity.net/xml/lineInfo";
            sKey = "%2F7DNvbEWxYBMWQVpGs6%2BWe1DaaKCWWTfTeypLNtgiBvGAUpg%2FdmMlO65C3yevYY73LQtoflMp5NenQRhxK%2BbEg%3D%3D";
            searchUrl = sUrl + "?serviceKey=" + sKey;
            return searchUrl;
        } else {
            //국토교통부 버스위치정보
            sUrl = "http://api.gjcity.net/xml/arriveInfo";
            sKey = "%2F7DNvbEWxYBMWQVpGs6%2BWe1DaaKCWWTfTeypLNtgiBvGAUpg%2FdmMlO65C3yevYY73LQtoflMp5NenQRhxK%2BbEg%3D%3D";
            searchUrl = sUrl + "?serviceKey=" + sKey + "&BUSSTOP_ID=" + data;
            return searchUrl;
        }

    }

    private String downloadUrl(String myurl) throws IOException {

        HttpURLConnection conn = null;
        try {
            URL url = new URL(myurl);
            conn = (HttpURLConnection) url.openConnection();
            BufferedInputStream buf = new BufferedInputStream(conn.getInputStream());
            BufferedReader bufreader = new BufferedReader(new InputStreamReader(buf, "utf-8"));
            String line = null;
            String page = "";
            while ((line = bufreader.readLine()) != null) {
                page += line;
            }

            return page;
        } finally {
            conn.disconnect();
        }
    }


    //인텐트로 보내기 위해서는 보따리에 쌀 필요
    //따라서 간단 Bus 객체를 Parcelable에 싸서 보냄
    //그리고 여기엔 참 중요한 것이 하나 있음.
    //여기에서 보내진 ArrayList를 ListView에 보일때 객체 이름이 보여지게 되므로 문제가 발생함
    //이를 해결하기 위해서 명품자바의 pp.329-330의 toString()를 overriding하여 사용함. //매우중요
    public static class ArrivalBus implements Parcelable {

        private String LINE_ID;
        private String LINE_NAME;
        private String BUS_ID;
        private String CURR_STOP_ID;
        private String BUSSTOP_NAME;
        private String REMAIN_MIN;
        private String REMAIN_STOP;
        private String DIR_START;
        private String DIR_END;

        public ArrivalBus() {
        }

        public ArrivalBus(Parcel source) {
            this.LINE_ID = source.readString();
            this.LINE_NAME = source.readString();
            this.BUS_ID = source.readString();
            this.CURR_STOP_ID = source.readString();
            this.BUSSTOP_NAME = source.readString();
            this.REMAIN_MIN = source.readString();
            this.REMAIN_STOP = source.readString();
            this.DIR_START = source.readString();
            this.DIR_END = source.readString();
        }


        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.LINE_ID);
            dest.writeString(this.LINE_NAME);
            dest.writeString(this.BUS_ID);
            dest.writeString(this.CURR_STOP_ID);
            dest.writeString(this.BUSSTOP_NAME);
            dest.writeString(this.REMAIN_MIN);
            dest.writeString(this.REMAIN_STOP);
            dest.writeString(this.DIR_START);
            dest.writeString(this.DIR_END);

        }

        public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
            @Override
            public ArrivalBus createFromParcel(Parcel source) {
                return new ArrivalBus(source);
            }

            @Override
            public ArrivalBus[] newArray(int size) {
                return new ArrivalBus[size];
            }
        };

        public String getLINE_ID() {
            return LINE_ID;
        }

        public void setLINE_ID(String id) {
            this.LINE_ID = id;
        }

        public String getLINE_NAME() {
            return LINE_NAME;
        }

        public void setLINE_NAME(String name) {
            this.LINE_NAME = name;
        }

        public String getBUS_ID() {
            return BUS_ID;
        }

        public void setBUS_ID(String bid) {
            this.BUS_ID = bid;
        }

        public String getCURR_STOP_ID() {
            return CURR_STOP_ID;
        }

        public void setCURR_STOP_ID(String csid) {
            this.CURR_STOP_ID = csid;
        }

        public String getBUSSTOP_NAME() {
            return BUSSTOP_NAME;
        }

        public void setBUSSTOP_NAME(String bsname) {
            this.BUSSTOP_NAME = bsname;
        }


        public String getREMAIN_MIN() {
            return REMAIN_MIN;
        }

        public void setREMAIN_MIN(String rm) {
            this.REMAIN_MIN = rm;
        }

        public String getREMAIN_STOP() {
            return REMAIN_STOP;
        }

        public void setREMAIN_STOP(String rs) {
            this.REMAIN_STOP = rs;
        }

        public String getDIR_START() {
            return DIR_START;
        }

        public void setDIR_START(String ds) {
            this.DIR_START = ds;
        }

        public String getDIR_END() {
            return DIR_END;
        }

        public void setDIR_END(String de) {
            this.DIR_END = de;
        }

        //명품자바책 329쪽 참조
        //객체 이름을 다른 형태로 표시
        public String toString() {
            return getLINE_NAME() + " -> 약 " + getREMAIN_MIN() + " 분";
        }
    }
}