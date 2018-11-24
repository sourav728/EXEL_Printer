package com.example.tvd.exel_printer;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.cie.btp.Barcode;
import com.cie.btp.CieBluetoothPrinter;
import com.cie.btp.DebugLog;

import com.cie.btp.FontStyle;
import com.cie.btp.FontType;
import com.cie.btp.PrintColumnParam;
import com.cie.btp.PrinterWidth;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.cie.btp.BtpConsts.RECEIPT_PRINTER_CONN_DEVICE_NAME;
import static com.cie.btp.BtpConsts.RECEIPT_PRINTER_CONN_STATE_CONNECTED;
import static com.cie.btp.BtpConsts.RECEIPT_PRINTER_CONN_STATE_CONNECTING;
import static com.cie.btp.BtpConsts.RECEIPT_PRINTER_CONN_STATE_LISTEN;
import static com.cie.btp.BtpConsts.RECEIPT_PRINTER_CONN_STATE_NONE;
import static com.cie.btp.BtpConsts.RECEIPT_PRINTER_MESSAGES;
import static com.cie.btp.BtpConsts.RECEIPT_PRINTER_MSG;
import static com.cie.btp.BtpConsts.RECEIPT_PRINTER_NAME;
import static com.cie.btp.BtpConsts.RECEIPT_PRINTER_NOTIFICATION_ERROR_MSG;
import static com.cie.btp.BtpConsts.RECEIPT_PRINTER_NOTIFICATION_MSG;
import static com.cie.btp.BtpConsts.RECEIPT_PRINTER_NOT_CONNECTED;
import static com.cie.btp.BtpConsts.RECEIPT_PRINTER_NOT_FOUND;
import static com.cie.btp.BtpConsts.RECEIPT_PRINTER_SAVED;
import static com.cie.btp.BtpConsts.RECEIPT_PRINTER_STATUS;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static int BtpLineWidth = 384;
    private TextView tvStatus;
    private Button btnPrint, btnConvert, btnbitmap;
    private Context i;
    private boolean q;
    private int h;
    private List p;
    ProgressDialog pdWorkInProgress;
    private static final int BARCODE_WIDTH = 384;
    private static final int BARCODE_HEIGHT = 130;
    private static byte[] LINE_FEED = new byte[]{10};
    private static byte[] RESET_PRINTER = new byte[]{27, 64};
    public static CieBluetoothPrinter mPrinter = CieBluetoothPrinter.INSTANCE;
    private int imageAlignment = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvStatus = (TextView) findViewById(R.id.status_msg);
        btnConvert = (Button) findViewById(R.id.bt_convert);
        btnPrint = (Button) findViewById(R.id.btnPrint);
        pdWorkInProgress = new ProgressDialog(this);
        pdWorkInProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null) {
            Toast.makeText(this, R.string.bt_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        try {
            mPrinter.initService(MainActivity.this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        btnConvert.setOnClickListener(this);
        btnPrint.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        DebugLog.logTrace();
        mPrinter.onActivityResume();
        super.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onPause() {
        DebugLog.logTrace();
        mPrinter.onActivityPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        DebugLog.logTrace("onDestroy");
        mPrinter.onActivityDestroy();
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RECEIPT_PRINTER_MESSAGES);
        LocalBroadcastManager.getInstance(this).registerReceiver(ReceiptPrinterMessageReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(ReceiptPrinterMessageReceiver);
        } catch (Exception e) {
            DebugLog.logException(e);
        }
    }

    private final BroadcastReceiver ReceiptPrinterMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            DebugLog.logTrace("Printer Message Received");
            Bundle b = intent.getExtras();

            switch (b.getInt(RECEIPT_PRINTER_STATUS)) {
                case RECEIPT_PRINTER_CONN_STATE_NONE:
                    tvStatus.setText(R.string.printer_not_conn);
                    break;
                case RECEIPT_PRINTER_CONN_STATE_LISTEN:
                    tvStatus.setText(R.string.ready_for_conn);
                    break;
                case RECEIPT_PRINTER_CONN_STATE_CONNECTING:
                    tvStatus.setText(R.string.printer_connecting);
                    break;
                case RECEIPT_PRINTER_CONN_STATE_CONNECTED:
                    tvStatus.setText(R.string.printer_connected);
                    //new AsyncPrint().execute();
                    //new AsyncPrint2().execute();
                    new AsyncPrint3().execute();
                    break;
                case RECEIPT_PRINTER_CONN_DEVICE_NAME:
                    savePrinterMac(b.getString(RECEIPT_PRINTER_NAME));
                    break;
                case RECEIPT_PRINTER_NOTIFICATION_ERROR_MSG:
                    String n = b.getString(RECEIPT_PRINTER_MSG);
                    tvStatus.setText(n);
                    break;
                case RECEIPT_PRINTER_NOTIFICATION_MSG:
                    String m = b.getString(RECEIPT_PRINTER_MSG);
                    tvStatus.setText(m);
                    break;
                case RECEIPT_PRINTER_NOT_CONNECTED:
                    tvStatus.setText("Status : Printer Not Connected");
                    break;
                case RECEIPT_PRINTER_NOT_FOUND:
                    tvStatus.setText("Status : Printer Not Found");
                    break;
                case RECEIPT_PRINTER_SAVED:
                    tvStatus.setText(R.string.printer_saved);
                    break;
            }
        }
    };

    private void savePrinterMac(String sMacAddr) {
        if (sMacAddr.length() > 4) {
            tvStatus.setText("Preferred Printer saved");
        } else {
            tvStatus.setText("Preferred Printer cleared");
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_convert:
                Write_Image();
                break;
            case R.id.btnPrint:
                mPrinter.connectToPrinter();
                break;
        }
    }

    //Billing
/*    private class AsyncPrint extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            btnPrint.setEnabled(false);
            pdWorkInProgress.setIndeterminate(true);
            pdWorkInProgress.setMessage("Printing ...");
            pdWorkInProgress.setCancelable(false); // disable dismiss by tapping outside of the dialog
            pdWorkInProgress.show();
        }

        @Override
        protected Void doInBackground(Void... params) {

            mPrinter.setPrinterWidth(PrinterWidth.PRINT_WIDTH_72MM);
            mPrinter.resetPrinter();

            mPrinter.setAlignmentCenter();

            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.download);
            mPrinter.printGrayScaleImage(bitmap, 1);

            //mPrinter.setBoldOn();
            byte b[] = {0x1d,0x21,0x00};
            mPrinter.sendBytes(b);
            mPrinter.printTextLine("HUBLI ELECTRICITY SUPPLY COMPANY LTD\n");
           // mPrinter.printTextLine("Company Ltd\n");

            *//*byte b4[] = {0x1d,0x21,0x01};
            mPrinter.sendBytes(b4);
            mPrinter.printTextLine("HUBLI ELECTRICITY SUPPLY COMPANY LTD\n");*//*

            byte n[] = {0x1d,0x21,0x00};
            mPrinter.sendBytes(n);

           // mPrinter.setBoldOff();

            mPrinter.setAlignmentCenter();
            mPrinter.printTextLine("CSD2. BELGAUM\n");

            mPrinter.setAlignmentLeft();
            mPrinter.printTextLine("Sub Division     : 540038\n");
            mPrinter.printTextLine("RRNO             : M62.861\n");

            mPrinter.setBoldOn();
            byte b8[] = {0x1d,0x21,0x01};
            mPrinter.sendBytes(b8);
            mPrinter.printTextLine("Account ID       : 2120173000\n");

            byte n1[] = {0x1d,0x21,0x00};
            mPrinter.sendBytes(n1);
            mPrinter.setBoldOff();

            mPrinter.printTextLine("MRCode:54003817 RAVINDRA B KOWADKAR\n");

            mPrinter.setAlignmentCenter();
            mPrinter.printTextLine("Name and Address\n");

            mPrinter.setAlignmentLeft();
            mPrinter.printTextLine("M/S MADHAV INDUS\n");
            mPrinter.printTextLine("SHED NO 1 PL NO 72/2 BELGAUM\n");
            mPrinter.printLineFeed();

            mPrinter.printTextLine("Tariff            : 5LT5A\n");
            mPrinter.printTextLine("Sanct Load        : HP:   9 KW:  0\n");
            mPrinter.printTextLine("Billing Period    : 02/11/2017-02/12/2017\n");
            mPrinter.printTextLine("Reading Date      : 02/12/2017\n");
            mPrinter.printTextLine("BillNo            : 2120173000-02/12/2017\n");
            mPrinter.printTextLine("Meter SLNo        : 500009812652\n");
            mPrinter.printTextLine("Pres Rdg          : 31447  Normal\n");
            mPrinter.printTextLine("Prev Rdg          : 31411  Meter GL\n");
            mPrinter.printTextLine("Constant          : 1\n");
            mPrinter.printTextLine("Consumption       : 36\n");
            mPrinter.printTextLine("Average           : 20\n");
            mPrinter.printLineFeed();
            mPrinter.printLineFeed();

            mPrinter.setAlignmentCenter();
            mPrinter.printTextLine("Fixed Charges\n");
            mPrinter.printTextLine("9.0*   45.00                     405.00\n");

            mPrinter.printLineFeed();

            mPrinter.setAlignmentCenter();
            mPrinter.printTextLine("Energy Charges\n");
            mPrinter.printTextLine("36.0*  5.10                      183.60\n");

            mPrinter.printLineFeed();
            mPrinter.printLineFeed();
            mPrinter.printLineFeed();

            mPrinter.setAlignmentLeft();
            mPrinter.printTextLine("FAC  : 36*  0.13                       4.68\n");
            mPrinter.printTextLine("Rebates/TOD   (-):                     0.00\n");

            mPrinter.printTextLine("PF Penalty       :                     0.00\n");
            mPrinter.printTextLine("MD Penalty       :                     0.00\n");

            mPrinter.printTextLine("Interest      @1%:                     2.02\n");
            mPrinter.printTextLine("Others           :                     0.00\n");

            mPrinter.printTextLine("Tax           @6%:                    11.01\n");
            mPrinter.printTextLine("Cur Bill Amt     :                   606.31\n");
            mPrinter.printTextLine("Arrears          :                   409.00\n");

            mPrinter.printTextLine("Credits & Adj (-):                     0.00\n");
            mPrinter.printTextLine("GOK Subsidy   (-):                     0.00\n");

            mPrinter.setBoldOn();
            byte b9[] = {0x1d,0x21,0x01};
            mPrinter.sendBytes(b9);
            mPrinter.printTextLine("Net Amt Due      :                  1015.00\n");
            byte n2[] = {0x1d,0x21,0x00};
            mPrinter.sendBytes(n2);
            mPrinter.setBoldOff();

            mPrinter.printTextLine("Due Date         :               16/12/2017\n");
            mPrinter.printTextLine("Billed On        :               22/12/2017\n");

            mPrinter.printLineFeed();
            mPrinter.printBarcode("1234567890123", Barcode.CODE_128, BARCODE_WIDTH, 50, imageAlignment);
            mPrinter.setAlignmentCenter();
            mPrinter.printTextLine("1234567890123");
            mPrinter.printLineFeed();

            *//*

            mPrinter.printLineFeed();
            mPrinter.printLineFeed();
            mPrinter.printLineFeed();
            mPrinter.printLineFeed();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mPrinter.disconnectFromPrinter();
            btnPrint.setEnabled(true);
            pdWorkInProgress.cancel();
        }
    }*/

    //todo image printing code
   /* private class AsyncPrint extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            btnPrint.setEnabled(false);
            pdWorkInProgress.setIndeterminate(true);
            pdWorkInProgress.setMessage("Printing ...");
            pdWorkInProgress.setCancelable(false); // disable dismiss by tapping outside of the dialog
            pdWorkInProgress.show();
        }

        @Override
        protected Void doInBackground(Void... params) {

            mPrinter.setPrinterWidth(PrinterWidth.PRINT_WIDTH_72MM);
            mPrinter.resetPrinter();

            mPrinter.setAlignmentCenter();
            //todo below code isto set font_size for image printing
            TextPaint var2 = new TextPaint();
            var2.setColor(-16777216);
            byte var3 = 23;
            var2.setTextSize((float) var3);
            //todo end

            //todo below code isto set font_size for image printing
            TextPaint var4 = new TextPaint();
            var4.setColor(-16777216);
            byte var5 = 35;
            var4.setTextSize((float) var5);
            //todo end

            byte b[] = {0x1d, 0x21, 0x00};
            mPrinter.sendBytes(b);
            mPrinter.printUnicodeText("HUBLI ELECTRICITY SUPPLY COMPANY LTD", Layout.Alignment.ALIGN_NORMAL, var4);

            byte n[] = {0x1d, 0x21, 0x00};
            mPrinter.sendBytes(n);

            // mPrinter.setBoldOff();

            mPrinter.setAlignmentCenter();
            mPrinter.printUnicodeText("CSD2. BELGAUM", Layout.Alignment.ALIGN_NORMAL, var2);

            mPrinter.setAlignmentLeft();
            // mPrinter.printTextLine("Sub Division     : 540038\n");
            mPrinter.printUnicodeText("Sub Division/ಉಪ ವಿಭಾಗ", Layout.Alignment.ALIGN_NORMAL, var2);
            //mPrinter.printTextLine("RRNO             : M62.861\n");
            mPrinter.printUnicodeText("RRNO/ಆರ್.ಆರ್ ನಂಬರ್", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.setBoldOn();
            byte b8[] = {0x1d, 0x21, 0x01};
            mPrinter.sendBytes(b8);
            //mPrinter.printTextLine("Account ID       : 2120173000\n");
            mPrinter.printUnicodeText(space("Account ID/ಖಾತೆ ID", 21), Layout.Alignment.ALIGN_NORMAL, var4);

            byte n1[] = {0x1d, 0x21, 0x00};
            mPrinter.sendBytes(n1);
            mPrinter.setBoldOff();

            mPrinter.printTextLine("MRCode:54003817 RAVINDRA B KOWADKAR");

            mPrinter.setAlignmentCenter();
            mPrinter.printUnicodeText("Name and Address/ಹೆಸರು ಮತ್ತು ವಿಳಾಸ", Layout.Alignment.ALIGN_NORMAL, var2);

            mPrinter.setAlignmentLeft();
            mPrinter.printTextLine("M/S MADHAV INDUS");
            mPrinter.printTextLine("SHED NO 1 PL NO 72/2 BELGAUM");
            mPrinter.printLineFeed();

            mPrinter.printUnicodeText(space("Tariff/ಸುಂಕ", 21) + ":" + " " + "5LT5A", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText(space("Sanct Load/ಮಂ.ಪ್ರಮಾಣ", 21) + ":" + " " + "HP:   9 KW:  0", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText(space("Billing Period/ಬಿಲ್ಲಿಂಗ್ ಅವಧಿ", 21) + ":" + " " + "02/11/2017-02/12/2017", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText("Reading Date/ರಿeಡಿಂಗ ದಿನಾಂಕ     : 02/12/2017", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText("BillNo/ಬಿಲ್ ಸಂಖ್ಯೆ              : 2120173000-02/12/2017", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText("Meter SLNo/ಮೀಟರ್ ಸಂಖ್ಯೆ        : 500009812652", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText("Pres Rdg/ಇಂದಿನ ಮಾಪನ          : 31447  Normal", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText("Prev Rdg/ಹಿಂದಿನ ಮಾಪನ          : 31411  Meter GL", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText("Constant/ಮಾಪನ ಸ್ಟಿರಾಂಕ         : 1", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText("Consumption/ಬಳಕೆ              : 36", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText("Average/ಸರಾಸರಿ                : 20", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printLineFeed();
            mPrinter.printLineFeed();

            mPrinter.setAlignmentCenter();
            mPrinter.printUnicodeText("Fixed Charges/ನಿಗದಿತ ಶುಲ್ಕ", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText("9.0*   45.00                : 24.00", Layout.Alignment.ALIGN_NORMAL, var2);

            mPrinter.printLineFeed();

            mPrinter.setAlignmentCenter();
            mPrinter.printUnicodeText("Energy Charges/ವಿದ್ಯುತ್ ಶುಲ್ಕ", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText("36.0*  5.10                  : 65.00", Layout.Alignment.ALIGN_NORMAL, var2);

            mPrinter.printLineFeed();
            mPrinter.printLineFeed();
            mPrinter.printLineFeed();

            mPrinter.setAlignmentLeft();
            mPrinter.printUnicodeText("FAC  : 36*  0.13              : 4.68", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText("Rebates/TOD   (-)             : 0.00", Layout.Alignment.ALIGN_NORMAL, var2);

            mPrinter.printUnicodeText("PF Penalty/ವಿದ್ಯುತ್ಅಂಶ ದಂಡ       : 0.00", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText("MD Penalty/ಎಂ.ಡಿ.ದಂಡ           : 0.00", Layout.Alignment.ALIGN_NORMAL, var2);

            mPrinter.printUnicodeText("Interest      @1%              : 2.02", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText("Others/ಇತರೆ                     :  0.00", Layout.Alignment.ALIGN_NORMAL, var2);

            mPrinter.printUnicodeText("Tax/ತೆರಿಗೆ       @6%:             :  11.01", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText("Cur Bill Amt/ಒಟ್ಟು ಬಿಲ್ ಮೊತ್ತ      :  606.31", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText("Arrears/ಬಾಕಿ                     :  409.00", Layout.Alignment.ALIGN_NORMAL, var2);

            mPrinter.printUnicodeText("Credits & Adj/ಜಮಾ (-)          :  0.00", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText("GOK Subsidy   (-)               :   0.00", Layout.Alignment.ALIGN_NORMAL, var2);

            mPrinter.setBoldOn();
            byte b9[] = {0x1d, 0x21, 0x01};
            mPrinter.sendBytes(b9);
            mPrinter.printUnicodeText("Net Amt Due/ಪಾವತಿ ಮೊತ್ತ            :  1015.00", Layout.Alignment.ALIGN_NORMAL, var4);
            byte n2[] = {0x1d, 0x21, 0x00};
            mPrinter.sendBytes(n2);
            mPrinter.setBoldOff();

            mPrinter.printUnicodeText("Due Date/ಪಾವತಿ ಕೊನೆ ದಿನಾಂಕ          :  16/12/2017", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText("Billed On                        :  22/12/2017", Layout.Alignment.ALIGN_NORMAL, var2);

            mPrinter.printLineFeed();
            mPrinter.printBarcode("1234567890123", Barcode.CODE_128, BARCODE_WIDTH, 50, imageAlignment);
            mPrinter.setAlignmentCenter();
            mPrinter.printTextLine("1234567890123");
            mPrinter.printLineFeed();


            mPrinter.printLineFeed();
            mPrinter.printLineFeed();
            mPrinter.printLineFeed();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mPrinter.disconnectFromPrinter();
            btnPrint.setEnabled(true);
            pdWorkInProgress.cancel();
        }
    }*/

    private class AsyncPrint extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            btnPrint.setEnabled(false);
            pdWorkInProgress.setIndeterminate(true);
            pdWorkInProgress.setMessage("Printing ...");
            pdWorkInProgress.setCancelable(false); // disable dismiss by tapping outside of the dialog
            pdWorkInProgress.show();
        }

        @Override
        protected Void doInBackground(Void... params) {

            mPrinter.setPrinterWidth(PrinterWidth.PRINT_WIDTH_72MM);
            mPrinter.resetPrinter();

            mPrinter.setAlignmentCenter();
            //todo below code isto set font_size for image printing
            TextPaint var2 = new TextPaint();
            var2.setColor(-16777216);
            byte var3 = 23;
            var2.setTextSize((float) var3);
            //todo end

            //todo below code isto set font_size for image printing
            TextPaint var4 = new TextPaint();
            var4.setColor(-16777216);
            byte var5 = 30;
            var4.setTextSize((float) var5);
            //todo end

            byte b[] = {0x1d, 0x21, 0x00};
           /* mPrinter.sendBytes(b);
            mPrinter.printUnicodeText("HUBLI ELECTRICITY SUPPLY",Layout.Alignment.ALIGN_CENTER,var4);
            mPrinter.printUnicodeText("COMPANY LTD",Layout.Alignment.ALIGN_CENTER,var4);
            mPrinter.printLineFeed();*/

            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.img3);
            mPrinter.printGrayScaleImage(bitmap, 1);

            String[] sCol1 = {"  ಉಪ ವಿಭಾಗ/Sub Division", "  ಆರ್.ಆರ್ ನಂಬರ್/RRNO"};
            PrintColumnParam pcp1stCol = new PrintColumnParam(sCol1, 59, Layout.Alignment.ALIGN_NORMAL, 24, Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            String[] sCol2 = {":", ":"};
            PrintColumnParam pcp2ndCol = new PrintColumnParam(sCol2, 1, Layout.Alignment.ALIGN_CENTER, 24);
            String[] sCol3 = {"540037  ", "22.362  "};
            PrintColumnParam pcp3rdCol = new PrintColumnParam(sCol3, 40, Layout.Alignment.ALIGN_NORMAL, 24, Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            mPrinter.PrintTable(pcp1stCol, pcp2ndCol, pcp3rdCol);


            mPrinter.setBoldOn();
            byte b8[] = {0x1d, 0x21, 0x01};
            mPrinter.sendBytes(b8);
            mPrinter.printUnicodeText(space(" ಖಾತೆ ಸಂಖ್ಯೆ/Account ID" + "  " + ":" + " 0010573000", 21), Layout.Alignment.ALIGN_NORMAL, var4);

            byte n1[] = {0x1d, 0x21, 0x00};
            mPrinter.sendBytes(n1);
            mPrinter.setBoldOff();

            mPrinter.printLineFeed();

            mPrinter.printUnicodeText(" ಹೆಸರು ಮತ್ತು ವಿಳಾಸ/Name and Address", Layout.Alignment.ALIGN_CENTER, var2);
            mPrinter.printUnicodeText(" I K SHIRAGAONKAR", Layout.Alignment.ALIGN_CENTER, var2);
            mPrinter.printUnicodeText(" R/S -20 LAXMI TEK BELGAUM", Layout.Alignment.ALIGN_CENTER, var2);
            mPrinter.printLineFeed();


            String[] sCol7 = {"  ಜಕಾತಿ/Tariff", "  ಮಂ.ಪ್ರಮಾಣ/Sanct Load", "  ಬಲ್ಲಿಂಗ್ ಅವಧಿ/Billing Period", "  ರೀಡಿಂಗ ದಿನಾಂಕ/Reading Date", "  ಬಿಲ್ ಸಂಖ್ಯೆ/BillNo", "  ಮೀಟರ್ ಸಂಖ್ಯೆ/Meter SlNo", "  ಇಂದಿನ ಮಾಪನ/Pres Rdg", "  ಹಿಂದಿನ ಮಾಪನ/Prev Rdg", "  ಮಾಪನ ಸ್ಥಿರಾಂಕ/Constant", "  ಬಳಕೆ/Consumption", "  ಸರಾಸರಿ/Average"};
            PrintColumnParam pcp7stCol = new PrintColumnParam(sCol7, 55, Layout.Alignment.ALIGN_NORMAL, 24, Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            String[] sCol8 = {":", ":", ":", ":", ":", ":", ":", ":", ":", ":", ":"};
            PrintColumnParam pcp8ndCol = new PrintColumnParam(sCol8, 1, Layout.Alignment.ALIGN_CENTER, 24);
            String[] sCol9 = {"5LT-2A1-N", "HP: 0 KW:  3", "02/07/2018-02/08/2018", "02/08/2018", "001057300008201801", "500009790065", "13660", "13583", "1", "77", "50"};
            PrintColumnParam pcp9rdCol = new PrintColumnParam(sCol9, 44, Layout.Alignment.ALIGN_NORMAL, 24, Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            mPrinter.PrintTable(pcp7stCol, pcp8ndCol, pcp9rdCol);
            mPrinter.printLineFeed();

            mPrinter.printUnicodeText(" ನಿಗದಿತ ಶುಲ್ಕ/Fixed Charges", Layout.Alignment.ALIGN_CENTER, var2);
            //mPrinter.printUnicodeText("             9.0*   45.00                  : 24.00", Layout.Alignment.ALIGN_NORMAL, var2);

            String[] sCol10 = {"  1.0  *        50.00"};
            PrintColumnParam pcp10stCol = new PrintColumnParam(sCol10, 59, Layout.Alignment.ALIGN_NORMAL, 24, Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            String[] sCol11 = {":"};
            PrintColumnParam pcp11ndCol = new PrintColumnParam(sCol11, 1, Layout.Alignment.ALIGN_CENTER, 24);
            String[] sCol12 = {"50.00"};
            PrintColumnParam pcp12rdCol = new PrintColumnParam(sCol12, 40, Layout.Alignment.ALIGN_OPPOSITE, 24, Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            mPrinter.PrintTable(pcp10stCol, pcp11ndCol, pcp12rdCol);

            String[] sCol28 = {"  2.0  *        60.00"};
            PrintColumnParam pcp28stCol = new PrintColumnParam(sCol28, 59, Layout.Alignment.ALIGN_NORMAL, 24, Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            String[] sCol29 = {":"};
            PrintColumnParam pcp29ndCol = new PrintColumnParam(sCol29, 1, Layout.Alignment.ALIGN_CENTER, 24);
            String[] sCol30 = {"120.00"};
            PrintColumnParam pcp30rdCol = new PrintColumnParam(sCol30, 40, Layout.Alignment.ALIGN_OPPOSITE, 24, Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            mPrinter.PrintTable(pcp28stCol, pcp29ndCol, pcp30rdCol);

            //mPrinter.printLineFeed();

            mPrinter.setAlignmentCenter();
            mPrinter.printUnicodeText(" ವಿದ್ಯುತ್ ಶುಲ್ಕ/Energy Charges", Layout.Alignment.ALIGN_CENTER, var2);
            //mPrinter.printUnicodeText("             36.0*  5.10                  : 65.00", Layout.Alignment.ALIGN_NORMAL, var2);
            String[] sCol13 = {"  30.0  *       3.45"};
            PrintColumnParam pcp13stCol = new PrintColumnParam(sCol13, 59, Layout.Alignment.ALIGN_NORMAL, 24, Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            String[] sCol14 = {":"};
            PrintColumnParam pcp14ndCol = new PrintColumnParam(sCol14, 1, Layout.Alignment.ALIGN_CENTER, 24);
            String[] sCol15 = {"103.50"};
            PrintColumnParam pcp15rdCol = new PrintColumnParam(sCol15, 40, Layout.Alignment.ALIGN_OPPOSITE, 24, Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            mPrinter.PrintTable(pcp13stCol, pcp14ndCol, pcp15rdCol);

            //mPrinter.printUnicodeText("             36.0*  5.10                  : 65.00", Layout.Alignment.ALIGN_NORMAL, var2);
            String[] sCol31 = {"  47.0  *       4.95"};
            PrintColumnParam pcp31stCol = new PrintColumnParam(sCol31, 59, Layout.Alignment.ALIGN_NORMAL, 24, Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            String[] sCol32 = {":"};
            PrintColumnParam pcp32ndCol = new PrintColumnParam(sCol32, 1, Layout.Alignment.ALIGN_CENTER, 24);
            String[] sCol33 = {"232.65"};
            PrintColumnParam pcp33rdCol = new PrintColumnParam(sCol33, 40, Layout.Alignment.ALIGN_OPPOSITE, 24, Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            mPrinter.PrintTable(pcp31stCol, pcp32ndCol, pcp33rdCol);

           /* mPrinter.printLineFeed();
            mPrinter.printLineFeed();
*/
            mPrinter.printUnicodeText(" ಎಫ್.ಎ.ಸಿ/FAC", Layout.Alignment.ALIGN_CENTER, var2);
            //mPrinter.printUnicodeText("             36.0*  5.10                  : 65.00", Layout.Alignment.ALIGN_NORMAL, var2);
            String[] sCol25 = {"  77.0*  0.00"};
            PrintColumnParam pcp25stCol = new PrintColumnParam(sCol25, 59, Layout.Alignment.ALIGN_NORMAL, 24, Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            String[] sCol26 = {":"};
            PrintColumnParam pcp26ndCol = new PrintColumnParam(sCol26, 1, Layout.Alignment.ALIGN_CENTER, 24);
            String[] sCol27 = {"0.00"};
            PrintColumnParam pcp26rdCol = new PrintColumnParam(sCol27, 40, Layout.Alignment.ALIGN_OPPOSITE, 24, Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            mPrinter.PrintTable(pcp25stCol, pcp26ndCol, pcp26rdCol);

            String[] sCol16 = {"  ಪಿ.ಎಫ್ ದಂಡ/PF Penalty", "  ಎಂ.ಡಿ.ದಂಡ/MD Penalty", "  ಬಡ್ಡಿ/Interest @1%", "  ಇತರೆ/Others", "  ತೆರಿಗೆ/Tax @6%", "  ಒಟ್ಟು ಬಿಲ್ ಮೊತ್ತ/Cur Bill Amt", "  ಬಾಕಿ/Arrears", "  ಜಮಾ/Credits & Adj", "  ಐ.ಒ.ಡಿ/IOD", "  ಸಹಾಯಧನ/GOK"};
            PrintColumnParam pcp16stCol = new PrintColumnParam(sCol16, 59, Layout.Alignment.ALIGN_NORMAL, 24, Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            String[] sCol17 = {":", ":", ":", ":", ":", ":", ":", ":", ":"};
            PrintColumnParam pcp17ndCol = new PrintColumnParam(sCol17, 1, Layout.Alignment.ALIGN_CENTER, 24);
            String[] sCol18 = {"0.00", "0.00", "5.00", "0.00", "20.17", "531.32", "972.00", "0.00", "0.00", "0.00"};
            PrintColumnParam pcp18rdCol = new PrintColumnParam(sCol18, 40, Layout.Alignment.ALIGN_OPPOSITE, 24, Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            mPrinter.PrintTable(pcp16stCol, pcp17ndCol, pcp18rdCol);

            mPrinter.setBoldOn();
            byte b9[] = {0x1d, 0x21, 0x01};
            mPrinter.sendBytes(b9);
            mPrinter.printUnicodeText(space(" ಪಾವತಿಸಬೇಕಾದ ಮೊತ್ತ/NetAmtDue:" + " 1503.00", 21), Layout.Alignment.ALIGN_NORMAL, var4);

            byte n2[] = {0x1d, 0x21, 0x00};
            mPrinter.sendBytes(n2);
            mPrinter.setBoldOff();


            String[] sCol22 = {"  ಪಾವತಿ ಕೊನೆ ದಿನಾಂಕ/Due Date", "  ಬಿಲ್ ದಿನಾಂಕ/Billed On", "  ಮಾ.ಓ.ಸಂಕೇತ/Mtr.Rdr.Code"};
            PrintColumnParam pcp22stCol = new PrintColumnParam(sCol22, 59, Layout.Alignment.ALIGN_NORMAL, 24, Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            String[] sCol23 = {":", ":", ":"};
            PrintColumnParam pcp23ndCol = new PrintColumnParam(sCol23, 1, Layout.Alignment.ALIGN_CENTER, 24);
            String[] sCol24 = {"16/08/2018", "02/08/2018 10:17", "54003717SURESH CHOUGULE"};
            PrintColumnParam pcp24rdCol = new PrintColumnParam(sCol24, 40, Layout.Alignment.ALIGN_OPPOSITE, 24, Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            mPrinter.PrintTable(pcp22stCol, pcp23ndCol, pcp24rdCol);


            mPrinter.printBarcode("1234567890123", Barcode.CODE_128, BARCODE_WIDTH, BARCODE_HEIGHT, imageAlignment);
            //mPrinter.setAlignmentCenter();
            // mPrinter.setCharRightSpacing(10);
            //mPrinter.printTextLine("35251408350069854003717\n");
            //mPrinter.printUnicodeText("35251408350069854003717",Layout.Alignment.ALIGN_CENTER,var2);
            mPrinter.printLineFeed();
            mPrinter.printLineFeed();
            mPrinter.printLineFeed();
            mPrinter.printLineFeed();
            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mPrinter.disconnectFromPrinter();
            btnPrint.setEnabled(true);
            pdWorkInProgress.cancel();
        }
    }


    private class AsyncPrint3 extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            btnPrint.setEnabled(false);
            pdWorkInProgress.setIndeterminate(true);
            pdWorkInProgress.setMessage("Printing ...");
            pdWorkInProgress.setCancelable(false); // disable dismiss by tapping outside of the dialog
            pdWorkInProgress.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            mPrinter.setPrinterWidth(PrinterWidth.PRINT_WIDTH_72MM);
            mPrinter.resetPrinter();
            File file = new File(android.os.Environment.getExternalStorageDirectory(), "exel_image.jpg");
            if (file.exists()) {
                Bitmap bmp = BitmapFactory.decodeFile("/sdcard/exel_image.jpg");
                mPrinter.printGrayScaleImage(bmp, 1);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mPrinter.disconnectFromPrinter();
            btnPrint.setEnabled(true);
            pdWorkInProgress.cancel();
        }
    }

    private void Write_Image() {
        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/DroidSansMono.ttf");
        TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        // paint.setARGB(255, 0, 0, 0);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(typeface);

        Bitmap image = Bitmap.createBitmap(576, 1450, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(image);
        canvas.drawColor(Color.WHITE);
        Typeface bold = Typeface.create(typeface, Typeface.BOLD);
        paint.setTypeface(bold);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.img5);
        float left = 10;
        float top = 10;
        RectF dst = new RectF(left, top, left + 560, top + 120); // width=100, height=120
        canvas.drawBitmap(bitmap, null, dst, null);

       /* paint.setTextSize(27);
        canvas.drawText("ಹುಬ್ಬಳ್ಳಿ ವಿದ್ಯುತ್ ಸರಬರಾಜು ಕಂಪನಿ ನಿಯಮಿತ", 10, 50, paint);*/
        paint.setTextSize(23);
        canvas.drawText("ಉಪ ವಿಭಾಗ/Sub Division", 10, 150, paint);
        canvas.drawText(":", 340, 150, paint);
        canvas.drawText(" 540038", 350, 150, paint);
        canvas.drawText("ಆರ್.ಆರ್.ಸಂಖ್ಯೆ/RRNO", 10, 190, paint);
        canvas.drawText(":", 340, 190, paint);
        canvas.drawText(" IP57.228", 350, 190, paint);
        paint.setTextSize(28);
        canvas.drawText("ಖಾತೆ ಸಂಖ್ಯೆ/AccountID", 10, 230, paint);
        canvas.drawText(":", 340, 230, paint);
        canvas.drawText(" 1234567890", 350, 230, paint);
        paint.setTextSize(23);
        canvas.drawText("ಜಕಾತಿ/Tariff", 10, 270, paint);
        canvas.drawText(":", 340, 270, paint);
        canvas.drawText(" 5LT6B-M", 350, 270, paint);
        canvas.drawText("ಮಂ.ಪ್ರಮಾಣ/Sanct Load", 10, 310, paint);
        canvas.drawText(":", 340, 310, paint);
        canvas.drawText(" HP:3 KW:2", 350, 310, paint);
        canvas.drawText("Billing Period", 10, 350, paint);
        canvas.drawText(":", 210, 350, paint);
        canvas.drawText(" 01-10-2018" + " - " + "01-11-2018", 220, 350, paint);
        // canvas.drawText("ೀಡಿಂಗ ದಿನಾಂಕ/Reading Date:", 10, 330, paint);
        //canvas.drawText(" 01-11-2018", 310, 330, paint);
        canvas.drawText("ಮೀಟರ್ ಸಂಖ್ಯೆ/Meter SlNo", 10, 390, paint);
        canvas.drawText(":", 340, 390, paint);
        canvas.drawText(" 12345678", 350, 390, paint);
        canvas.drawText("ಇಂದಿನ ಮಾಪನ/Pres Rdg", 10, 430, paint);
        canvas.drawText(":", 340, 430, paint);
        canvas.drawText(" 100", 350, 430, paint);
        canvas.drawText("ಹಿಂದಿನ ಮಾಪನ/Prev Rdg", 10, 470, paint);
        canvas.drawText(":", 340, 470, paint);
        canvas.drawText(" 50", 350, 470, paint);

        canvas.drawText("ಮಾಪನ ಸ್ಥಿರಾಂಕ/Constant", 10, 510, paint);
        canvas.drawText(":", 340, 510, paint);
        canvas.drawText(" 1", 350, 510, paint);
        canvas.drawText("ಬಳಕೆ/Consumption", 10, 550, paint);
        canvas.drawText(":", 340, 550, paint);
        canvas.drawText(" 50", 350, 550, paint);
        canvas.drawText("ಸರಾಸರಿ/Average", 10, 590, paint);
        canvas.drawText(":", 340, 590, paint);
        canvas.drawText(" 51", 350, 590, paint);

        canvas.drawText("ದಾಖಲಿತ ಬೇಡಿಕೆ/Recorded MD", 10, 630, paint);
        canvas.drawText(":", 340, 630, paint);
        canvas.drawText(" 10", 350, 630, paint);
        canvas.drawText("ಪವರ ಫ್ಯಾಕ್ಟರ/Power Factor", 10, 670, paint);
        canvas.drawText(":", 340, 670, paint);
        canvas.drawText(" 0.85", 350, 670, paint);

        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("ನಿಗದಿತ ಶುಲ್ಕ/Fixed Charges", 290, 710, paint);

        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(centeralign2("3.0 x 60.00", 10) + rightspacing("180.00", 29), 10, 750, paint);

        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("ವಿದ್ಯುತ್ ಶುಲ್ಕ/Energy Charges", 290, 790, paint);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(centeralign2("3.0 x 60.00", 10) + rightspacing("180.00", 29), 10, 830, paint);

        canvas.drawText("ಎಫ್.ಎ.ಸಿ/FAC", 10, 870, paint);
        canvas.drawText(":", 340, 870, paint);
        canvas.drawText(rightspacing("10.00", 16), 350, 870, paint);

        canvas.drawText("ರಿಯಾಯಿತಿ/Rebates/TOD", 10, 910, paint);
        canvas.drawText(":", 340, 910, paint);
        canvas.drawText(rightspacing("10.00", 16), 910, 950, paint);

        canvas.drawText("ಪಿ.ಎಫ್ ದಂಡ/PF Penalty", 10, 950, paint);
        canvas.drawText(":", 340, 950, paint);
        canvas.drawText(rightspacing("10.00", 16), 350, 950, paint);

        canvas.drawText("ಎಂ.ಡಿ.ದಂಡ/MD Penalty", 10, 990, paint);
        canvas.drawText(":", 340, 990, paint);
        canvas.drawText(rightspacing("100.00", 16), 350, 990, paint);

        canvas.drawText("ಬಡ್ಡಿ/Interest @1%", 10, 1030, paint);
        canvas.drawText(":", 340, 1030, paint);
        canvas.drawText(rightspacing("1000.00", 16), 350, 1030, paint);

        canvas.drawText("ಇತರೆ/Others:", 10, 1070, paint);
        canvas.drawText(":", 340, 1070, paint);
        canvas.drawText(rightspacing("600.00", 16), 350, 1070, paint);

        canvas.drawText("ತೆರಿಗೆ/Tax @9%", 10, 1110, paint);
        canvas.drawText(":", 340, 1110, paint);
        canvas.drawText(rightspacing("6000.00", 16), 350, 1110, paint);

        canvas.drawText("ಬಿಲ್ ಮೊತ್ತ/Cur Bill Amt", 10, 1150, paint);
        canvas.drawText(":", 340, 1150, paint);
        canvas.drawText(rightspacing("60000.00", 16), 350, 1150, paint);

        canvas.drawText("ಬಾಕಿ/Arrears", 10, 1190, paint);
        canvas.drawText(":", 340, 1190, paint);
        canvas.drawText(rightspacing("1258.00", 16), 350, 1190, paint);

        canvas.drawText("ಜಮಾ/Credits & Adj", 10, 1230, paint);
        canvas.drawText(":", 340, 1230, paint);
        canvas.drawText(rightspacing("320.01", 16), 350, 1230, paint);

        canvas.drawText("ಐ.ಒ.ಡಿ/IOD", 10, 1270, paint);
        canvas.drawText(":", 340, 1270, paint);
        canvas.drawText(rightspacing("520.01", 16), 350, 1270, paint);

        paint.setTextSize(28);
        canvas.drawText("Net Amt Due", 10, 1310, paint);
        canvas.drawText(":", 220, 1310, paint);
        canvas.drawText(rightspacing("5000000.00", 16), 300, 1310, paint);
        try {
            savebitmap(image);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static File savebitmap(Bitmap bmp) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        File f = new File(Environment.getExternalStorageDirectory()
                + File.separator + "exel_image.jpg");
        f.createNewFile();
        FileOutputStream fo = new FileOutputStream(f);
        fo.write(bytes.toByteArray());
        fo.close();
        return f;
    }

    //Collection
    private class AsyncPrint2 extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            btnPrint.setEnabled(false);
            pdWorkInProgress.setIndeterminate(true);
            pdWorkInProgress.setMessage("Printing ...");
            pdWorkInProgress.setCancelable(false); // disable dismiss by tapping outside of the dialog
            pdWorkInProgress.show();
        }

        @Override
        protected Void doInBackground(Void... params) {

            mPrinter.setPrinterWidth(PrinterWidth.PRINT_WIDTH_72MM);
            mPrinter.resetPrinter();

           /* Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.download);
            mPrinter.printGrayScaleImage(bitmap, 1);*/

            mPrinter.setAlignmentCenter();
            //mPrinter.setBoldOn();
            mPrinter.setAlignmentLeft();
            mPrinter.printTextLine("----------------------------------------------\n");
            mPrinter.setAlignmentCenter();
            mPrinter.setBoldOn();
            byte b5[] = {0x1d, 0x21, 0x01};
            mPrinter.sendBytes(b5);
            mPrinter.printTextLine("Hubli Electricity Supply Company Ltd\n");
            byte n6[] = {0x1d, 0x21, 0x00};
            mPrinter.sendBytes(n6);
            mPrinter.setBoldOff();

            // mPrinter.printTextLine("Company Ltd\n");
            mPrinter.setAlignmentLeft();
            mPrinter.printTextLine("----------------------------------------------\n");
            /*byte b4[] = {0x1d,0x21,0x01};
            mPrinter.sendBytes(b4);
            mPrinter.printTextLine("HUBLI ELECTRICITY SUPPLY COMPANY LTD\n");*/
            mPrinter.setAlignmentCenter();
            mPrinter.setBoldOn();
            byte b7[] = {0x1d, 0x21, 0x01};
            mPrinter.sendBytes(b7);
            mPrinter.printTextLine("HESCOM COPY\n");
            mPrinter.printTextLine("CASH RECEIPT (RAPDRP-MCC\n");
            byte n8[] = {0x1d, 0x21, 0x00};
            mPrinter.sendBytes(n8);
            mPrinter.setBoldOff();

            mPrinter.setAlignmentLeft();
            mPrinter.printTextLine("----------------------------------------------\n");


            mPrinter.printTextLine("SUB Division           : 540037\n");
            mPrinter.printTextLine("RAPDRP MCC TR No.:\n");
            mPrinter.printTextLine("00000RAPDRPMCC540037AA030012018\n");
            mPrinter.printTextLine("Customer Name          : The Divisional Engineer\n");
            mPrinter.printTextLine("RRNo.                  : 56.203\n");

            mPrinter.setBoldOn();
            byte b1[] = {0x1d, 0x21, 0x01};
            mPrinter.sendBytes(b1);
            mPrinter.printTextLine("Account ID             : 0235773000\n");
            byte n2[] = {0x1d, 0x21, 0x00};
            mPrinter.sendBytes(n2);
            mPrinter.setBoldOff();

            mPrinter.printTextLine("Receipt No             : 201810540037000027\n");
            mPrinter.printTextLine("Receipt Type           : Revenue\n");
            mPrinter.printTextLine("Payment Mode           : CASH\n");
            mPrinter.printTextLine("Receipt Date           : 30/01/2018 12:10\n");
            mPrinter.printTextLine("Meter Reader Name      : AAO\n");
            mPrinter.printTextLine("Meter Reader Code      : 10540037\n");

            mPrinter.setBoldOn();
            byte b3[] = {0x1d, 0x21, 0x01};
            mPrinter.sendBytes(b3);
            mPrinter.printTextLine("Amount Paid            : Rs: 438.0 /-\n");
            byte n4[] = {0x1d, 0x21, 0x00};
            mPrinter.sendBytes(n4);
            mPrinter.setBoldOff();
            mPrinter.printTextLine("Amount in words : Rs. Four hundred and thirty eight only\n");

            mPrinter.printLineFeed();
            mPrinter.setAlignmentLeft();
            mPrinter.printBarcode("1234567890123", Barcode.CODE_128, BARCODE_WIDTH, 50, imageAlignment);
            mPrinter.setAlignmentCenter();
            mPrinter.setAlignmentLeft();
            mPrinter.printTextLine("Reference Id  : 5L10540037023577300012018\n");
            // mPrinter.printTextLine("1234567890123");
            mPrinter.printLineFeed();

            mPrinter.printTextLine("----------------------------------------------\n");
            mPrinter.printLineFeed();
            mPrinter.printLineFeed();
            mPrinter.printLineFeed();
            mPrinter.printLineFeed();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mPrinter.disconnectFromPrinter();
            btnPrint.setEnabled(true);
            pdWorkInProgress.cancel();
        }
    }


    public String space(String s, int len) {
        int temp;
        StringBuilder spaces = new StringBuilder();
        temp = len - s.length();
        for (int i = 0; i < temp; i++) {
            spaces.append(" ");
        }
        return (s + spaces);
    }

    private String currentDateandTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String centeralign2(String text, int width) {
        int count = text.length();
        int value = width - count;
        int append = (value / 2);
        //return space1(" ", append) + text;
        return space1(String.format("%s", ""), append) + text;
    }

    private String space1(String s, int length) {
        int temp;
        StringBuilder spaces = new StringBuilder();
        temp = length - s.length();
        for (int i = 0; i < temp; i++) {
            spaces.insert(0, String.format("%" + i + "s", ""));
        }
        return (s + spaces);
    }

    private String rightspacing(String s1, int len) {
        int i;
        StringBuilder s1Builder = new StringBuilder(s1);
        for (i = 0; i < len - s1Builder.length(); i++) {
        }
        s1Builder.insert(0, String.format("%" + i + "s", ""));
        s1 = s1Builder.toString();
        return (s1);
    }
}
