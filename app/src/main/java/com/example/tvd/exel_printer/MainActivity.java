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
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
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

import com.cie.btp.PrintColumnParam;
import com.cie.btp.PrinterWidth;

import java.io.IOException;
import java.util.List;

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

public class MainActivity extends AppCompatActivity {

    private static int BtpLineWidth = 384;
    private TextView tvStatus;
    private Button btnPrint;
    private Context i;
    private boolean q;
    private int h;
    private List p;
    ProgressDialog pdWorkInProgress;

    private static final int BARCODE_WIDTH = 384;
    private static final int BARCODE_HEIGHT = 100;
    private static byte[] LINE_FEED = new byte[]{10};
    private static byte[] RESET_PRINTER = new byte[]{27, 64};
    public static CieBluetoothPrinter mPrinter = CieBluetoothPrinter.INSTANCE;
    private int imageAlignment = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = (TextView) findViewById(R.id.status_msg);

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

        btnPrint = (Button) findViewById(R.id.btnPrint);
        btnPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPrinter.connectToPrinter();
            }
        });
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
                    new AsyncPrint().execute();
                    //new AsyncPrint2().execute();
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
            mPrinter.sendBytes(b);
            mPrinter.printUnicodeText("ಹುಬ್ಬಳ್ಳಿ ವಿದ್ಯುತ್ ಸರಬರಾಜು ಕಂಪನಿ ನಿಯಮಿತ", Layout.Alignment.ALIGN_CENTER, var4);
            mPrinter.printUnicodeText("ವಿದ್ಯುತ್ ಬಿಲ್   ELECTRICITY BILL  ", Layout.Alignment.ALIGN_CENTER, var2);
            mPrinter.printLineFeed();
            mPrinter.printLineFeed();

            String[] sCol1 = {"ABC","DEFG","H","IJKLM","XYZ"};
            PrintColumnParam pcp1stCol = new PrintColumnParam(sCol1,59, Layout.Alignment.ALIGN_NORMAL,22,Typeface.create(Typeface.SANS_SERIF,Typeface.NORMAL));
            String[] sCol2 = {":",":",":",":",":"};
            PrintColumnParam pcp2ndCol = new PrintColumnParam(sCol2,1,Layout.Alignment.ALIGN_CENTER,22);
            String[] sCol3 = {"₹1.00","₹20.00","₹300.00","₹4,000.00","₹50,000.89"};
            PrintColumnParam pcp3rdCol = new PrintColumnParam(sCol3,40,Layout.Alignment.ALIGN_OPPOSITE,22,Typeface.create(Typeface.SANS_SERIF,Typeface.NORMAL));
            mPrinter.PrintTable(pcp1stCol,pcp2ndCol,pcp3rdCol);


            byte n[] = {0x1d, 0x21, 0x00};
            mPrinter.sendBytes(n);

            mPrinter.setAlignmentLeft();
            // mPrinter.printTextLine("Sub Division     : 540038\n");
            mPrinter.printUnicodeText("  ಉಪ ವಿಭಾಗ/Sub Division:" + "csd2.Belagavi", Layout.Alignment.ALIGN_NORMAL, var2);
            //mPrinter.printTextLine("RRNO             : M62.861\n");
            //mPrinter.printUnicodeText("RRNO/ಆರ್.ಆರ್ ನಂಬರ್", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText(" ಆರ್.ಆರ್.ಸಂಖ್ಯೆ/RRNO             : M62.861", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.setBoldOn();
            byte b8[] = {0x1d, 0x21, 0x01};
            mPrinter.sendBytes(b8);
            //mPrinter.printTextLine("Account ID       : 2120173000\n");
            mPrinter.printUnicodeText(" ಖಾತೆ ಸಂಖ್ಯೆ/Account ID         : 12345", Layout.Alignment.ALIGN_NORMAL, var4);

            byte n1[] = {0x1d, 0x21, 0x00};
            mPrinter.sendBytes(n1);
            mPrinter.setBoldOff();

            mPrinter.setAlignmentCenter();
            mPrinter.printUnicodeText(" ಹೆಸರು ಮತ್ತು ವಿಳಾಸ/Name and Address", Layout.Alignment.ALIGN_CENTER, var2);

            mPrinter.setAlignmentLeft();
            mPrinter.printTextLine(" M/S MADHAV INDUS");
            mPrinter.printTextLine(" SHED NO 1 PL NO 72/2 BELGAUM");
            mPrinter.printLineFeed();

            mPrinter.printUnicodeText(" ಜಕಾತಿ/Tariff                  : 5LT5A", Layout.Alignment.ALIGN_NORMAL, var2);

            mPrinter.printUnicodeText(" ಮಂ.ಪ್ರಮಾಣ/Sanct Load       HP: 9 KW:  0", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText(" ಬ ಿಲ್ಲಿಂಗ್ ಅವಧಿ/Billing Period  : 02/11/2017-02/12/2017", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText(" ರೀಡಿಂಗ ದಿನಾಂಕ/Reading Date      : 02/12/2017", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText(" ಬಿಲ್ ಸಂಖ್ಯೆ/BillNo              : 2120173000-02/12/2017", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText(" ಮೀಟರ್ ಸಂಖ್ಯೆ/Meter SlNo        : 500009812652", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText(" ಇಂದಿನ ಮಾಪನ/Pres Rdg          : 31447  Normal", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText(" ಹಿಂದಿನ ಮಾಪನ/Prev Rdg          : 31411  Meter GL", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText(" ಮಾಪನ ಸ್ಥಿರಾಂಕ/Constant         : 1", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText(" ಬಳಕೆ/Consumption              : 36", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText(" ಸರಾಸರಿ/Averageಿ               : 20", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printLineFeed();
            mPrinter.printLineFeed();

            mPrinter.setAlignmentCenter();
            mPrinter.printUnicodeText(" ನ ಿಗದಿತ ಶುಲ್ಕ/Fixed Charges", Layout.Alignment.ALIGN_CENTER, var2);
            mPrinter.printUnicodeText("             9.0*   45.00                  : 24.00", Layout.Alignment.ALIGN_NORMAL, var2);

            mPrinter.printLineFeed();

            mPrinter.setAlignmentCenter();
            mPrinter.printUnicodeText(" ವಿದ್ಯುತ್ ಶುಲ್ಕ/Energy Charges", Layout.Alignment.ALIGN_CENTER, var2);
            mPrinter.printUnicodeText("             36.0*  5.10                  : 65.00", Layout.Alignment.ALIGN_NORMAL, var2);

            mPrinter.printLineFeed();
            mPrinter.printLineFeed();
            mPrinter.printLineFeed();

            mPrinter.setAlignmentLeft();
            mPrinter.printUnicodeText(" ಎಫ್.ಎ.ಸಿ/FAC  : 36*  0.13       :                         4.68", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText(" ಿಯಾಯಿತಿ/Rebates/TOD   (-)       :                         0.00", Layout.Alignment.ALIGN_NORMAL, var2);

            mPrinter.printUnicodeText(" ಪಿ.ಎಫ್ ದಂಡ/PF Penalty            :                         0.00", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText(" ಎಂ.ಡಿ.ದಂಡ/MD Penalty             :                         0.00", Layout.Alignment.ALIGN_NORMAL, var2);

            mPrinter.printUnicodeText(" ಬಡ್ಡಿ/Interest @1%                :                         2.02", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText(" ಇತರೆ/Others                      :                         0.00", Layout.Alignment.ALIGN_NORMAL, var2);

            mPrinter.printUnicodeText(" ತೆರಿಗೆ/Tax @6%:                    :                         11.01", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText(" ಒಟ್ಟು ಬಿಲ್ ಮೊತ್ತ/Cur Bill Amt       :                         606.31", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText(" ಬಾಕಿ/Arrears                      :                         409.00", Layout.Alignment.ALIGN_NORMAL, var2);

            mPrinter.printUnicodeText(" ಜಮಾ/Credits & Adj (-)           :                         0.00", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText(" ಸರ್ಕಾರದ ಸಹಾಯಧನ/GOK Subsidy (-)   :                         0.00", Layout.Alignment.ALIGN_NORMAL, var2);

            mPrinter.setBoldOn();
            byte b9[] = {0x1d, 0x21, 0x01};
            mPrinter.sendBytes(b9);
            mPrinter.printUnicodeText(" ಪಾವತಿಸಬೇಕಾದ ಮೊತ್ತ/Net Amt Due        :                       1015.00", Layout.Alignment.ALIGN_NORMAL, var4);
            byte n2[] = {0x1d, 0x21, 0x00};
            mPrinter.sendBytes(n2);
            mPrinter.setBoldOff();

            mPrinter.printUnicodeText(" ಪಾವತಿ ಕೊನೆ ದಿನಾಂಕ/Due Date            :                       16/12/2017", Layout.Alignment.ALIGN_NORMAL, var2);
            mPrinter.printUnicodeText(" ಬಿಲ್ ದಿನಾಂಕ/Billed On                :                       22/12/2017", Layout.Alignment.ALIGN_NORMAL, var2);

            mPrinter.printUnicodeText(" ಮಾ.ಓ.ಸಂಕೇತ/Mtr.Rdr.Code:           :                       123456 ", Layout.Alignment.ALIGN_NORMAL, var2);
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
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.download);
            mPrinter.printGrayScaleImage(bitmap, 1);
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

}
