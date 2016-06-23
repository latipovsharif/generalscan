package generalscan.esc;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.Button;

public class Demo_ad_escActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    public static BluetoothAdapter myBluetoothAdapter;
    public String SelectedBDAddress;
    StatusBox statusBox;
    public static String ErrorMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        if (!ListBluetoothDevice()) {
            finish();
        }

        ErrorMessage = "";

        Button Button1 = (Button) findViewById(R.id.button1);
        statusBox = new StatusBox(this, Button1);
        Button1.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View arg0) {
                Print1(SelectedBDAddress);
            }
        });

        Button Button2 = (Button) findViewById(R.id.button2);
        Button2.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View arg0) {
                Print2(SelectedBDAddress);
            }
        });

        Button Button3 = (Button) findViewById(R.id.button3);
        Button3.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View arg0) {
                Print3(SelectedBDAddress);
            }
        });
    }

    public boolean ListBluetoothDevice() {
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        ListView listView = (ListView) findViewById(R.id.listView1);
        SimpleAdapter m_adapter = new SimpleAdapter(this, list,
                android.R.layout.simple_list_item_2,
                new String[]{"DeviceName", "BDAddress"},
                new int[]{android.R.id.text1, android.R.id.text2}
        );
        listView.setAdapter(m_adapter);

        if ((myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()) == null) {
            Toast.makeText(this, "Can not find the Generalscan Bluetooth Printer", Toast.LENGTH_LONG).show();
            return false;
        }

        if (!myBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 2);
        }

        Set<BluetoothDevice> pairedDevices = myBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() <= 0) return false;
        for (BluetoothDevice device : pairedDevices) {
            Map<String, String> map = new HashMap<String, String>();
            map.put("DeviceName", device.getName());
            map.put("BDAddress", device.getAddress());
            list.add(map);
        }
        listView.setOnItemClickListener(new ListView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SelectedBDAddress = list.get(position).get("BDAddress");
                if (((ListView) parent).getTag() != null) {
                    ((View) ((ListView) parent).getTag()).setBackgroundDrawable(null);
                }
                ((ListView) parent).setTag(view);
                view.setBackgroundColor(Color.BLUE);
            }
        });
        return true;
    }

    public static int zp_realtime_status(int timeout) {
        byte data[] = new byte[10];
        data[0] = 0x1f;
        data[1] = 0x00;
        data[2] = 0x06;
        data[3] = 0x00;
        data[4] = 0x07;
        data[5] = 0x14;
        data[6] = 0x18;
        data[7] = 0x23;
        data[8] = 0x25;
        data[9] = 0x32;
        BtSPP.SPPWrite(data, 10);
        byte readata[] = new byte[1];
        if (!BtSPP.SPPReadTimeout(readata, 1, timeout)) {
            return -1;
        }
        int status = readata[0];
        if ((status & 1) != 0) ErrorMessage = "Printer cover is opened!";
        if ((status & 2) != 0) ErrorMessage = "No paper";
        if ((status & 4) != 0) ErrorMessage = "Printer Head is over heat";
        return status;
    }


    public void showMessage(String str) {
        Toast.makeText(this, str, Toast.LENGTH_LONG).show();
    }

    public void Print1(String BDAddress) {
        statusBox.Show("Printing...");
        if (!BtSPP.OpenPrinter(BDAddress)) {
            Toast.makeText(this, BtSPP.ErrorMessage, Toast.LENGTH_LONG).show();
            statusBox.Close();
            return;
        }
        try {
            BtSPP.SPPWrite(new byte[]{0x1B, 0x40});        //Reset Bluetooth Printer
            BtSPP.SPPWrite(new byte[]{0x1B, 0x33, 0x00});    //Set Line Space=0
            BtSPP.SPPWrite("\n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1B, 0x61, 0x00});    //Set Not Align Center
            BtSPP.SPPWrite(new byte[]{0x1d, 0x21, 0x01});    //Set Double Height
            BtSPP.SPPWrite(String.format("    %-10s", "Generalscan Logistic").getBytes("UTF-8"));
            BtSPP.SPPWrite("\n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1d, 0x48, 0x02});    //Set the Barcode Information below the barcode
            BtSPP.SPPWrite(new byte[]{0x1d, 0x77, 0x03});    //Set the Barcode Width =0.375
            BtSPP.SPPWrite(new byte[]{0x1d, 0x68, 0x40});    //Set the Barcode Height=64
            //Print Code128 Barcode
            BtSPP.SPPWrite(new byte[]{0x1D, 0x6B, 0x08});
            BtSPP.SPPWrite("1234567890\0".getBytes("UTF-8"));
            BtSPP.SPPWrite("\n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1d, 0x21, 0x00});    //Set Not Double Height
            BtSPP.SPPWrite(String.format("Set Not Double Height\n").getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1d, 0x21, 0x01});    //Set Not Double Height
            BtSPP.SPPWrite(String.format("From %-10s To %-10s\n", "Germany", "Generalscan").getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1d, 0x21, 0x00});    //Set Not Double Height
            BtSPP.SPPWrite(String.format("Set Not Double Height\n").getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1d, 0x21, 0x01});    //Set Not Double Height
            BtSPP.SPPWrite(String.format("Piece %6d/%-5d Bill No. %-13d\n", 1, 222, 55).getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1d, 0x21, 0x00});    //Set Not Double Height
            BtSPP.SPPWrite(String.format("Set Not Double Height\n").getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1d, 0x21, 0x01});    //Set Double Height
            BtSPP.SPPWrite(String.format("Receieve %-28s \n", "To Daniel Lee").getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1d, 0x21, 0x00});    //Set Not Double Height
            BtSPP.SPPWrite(String.format("Set Not Double Height\n").getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1d, 0x21, 0x01});    //Set Double Height
            BtSPP.SPPWrite(String.format("Operator %-10s Name%-14s \n", "Generalscan", "GuangZhou").getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1d, 0x21, 0x00});    //Set Not Double Height
            BtSPP.SPPWrite(String.format("Some text should here\n").getBytes("UTF-8"));

            BtSPP.SPPWrite(new byte[]{0x1b, 0x61, 0x01});    //Set Align Center
            BtSPP.SPPWrite(new byte[]{0x1d, 0x21, 0x01});    //Set Double Height
            BtSPP.SPPWrite(String.format("Date %10s\n", "2015-05-16").getBytes("UTF-8"));

            BtSPP.SPPWrite("\n\n\n\n".getBytes("UTF-8"));
            if (zp_realtime_status(5000) > 0) {
                showMessage(ErrorMessage);
            }
            statusBox.Close();

        } catch (UnsupportedEncodingException e) {
        }

        BtSPP.SPPClose();
    }

    public void Print2(String BDAddress) {
        statusBox.Show("Printing...");
        if (!BtSPP.OpenPrinter(BDAddress)) {
            Toast.makeText(this, BtSPP.ErrorMessage, Toast.LENGTH_LONG).show();
            statusBox.Close();
            return;
        }
        try {
            BtSPP.SPPWrite(new byte[]{0x1B, 0x40});
            BtSPP.SPPWrite(String.format("\n").getBytes("UTF-8"));
            BtSPP.SPPWrite(String.format("012345www.generalscan.com6789\n").getBytes("UTF-8"));
            BtSPP.SPPWrite(String.format("Traffice Violation \nPenalty Ticket\n").getBytes("UTF-8"));
            BtSPP.SPPWrite(String.format("Ticket NO.10012001091023009\n").getBytes("UTF-8"));
            BtSPP.SPPWrite(String.format("Name:Daniel Lee\n").getBytes("UTF-8"));
            BtSPP.SPPWrite(String.format("ID:21021119580822291X ______\n").getBytes("UTF-8"));
            BtSPP.SPPWrite(String.format("Age:39 ________________________\n").getBytes("UTF-8"));
            BtSPP.SPPWrite(String.format("Address:Unit A608,Building 3, YaDi Technology Park,____\n").getBytes("UTF-8"));
            BtSPP.SPPWrite(String.format("CarType:Bus  CarNumber:GS.11045\n").getBytes("UTF-8"));
            BtSPP.SPPWrite(String.format("Time:2001/11/22/09:24\n").getBytes("UTF-8"));
            BtSPP.SPPWrite(String.format("Place:Hetai Road, HuangShi Street").getBytes("UTF-8"));
            BtSPP.SPPWrite(String.format(" Baiyun District,Guangzhou\n").getBytes("UTF-8"));
            BtSPP.SPPWrite(String.format("Detail:driving car intoxicated\n").getBytes("UTF-8"));


            BtSPP.SPPWrite(String.format("\n\n\n\n").getBytes("UTF-8"));
            if (zp_realtime_status(8000) > 0)
                showMessage(ErrorMessage);

            statusBox.Close();
        } catch (UnsupportedEncodingException e) {
        }
        BtSPP.SPPClose();
    }

    public void Print3(String BDAddress) {
        statusBox.Show("Printing...");
        if (!BtSPP.OpenPrinter(BDAddress)) {
            Toast.makeText(this, BtSPP.ErrorMessage, Toast.LENGTH_LONG).show();
            statusBox.Close();
            return;
        }
        try {
            BtSPP.SPPWrite(new byte[]{0x1B, 0x40});        //Printer Reset
            BtSPP.SPPWrite("\n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1B, 0x61, 0x01});    //Center
            BtSPP.SPPWrite("*****Split Line*****\n".getBytes("UTF-8"));

            BtSPP.SPPWrite(new byte[]{0x1B, 0x61, 0x00});    //Align Left
            BtSPP.SPPWrite("  Generalscan Bluetooth Printer \n          Test Program\n".getBytes("UTF-8"));

            BtSPP.SPPWrite("(1.Title,Center,3 Times Font,Bold, Underline)\n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1B, 0x61, 0x01});    //Center
            BtSPP.SPPWrite(new byte[]{0x1D, 0x21, 0x22});    //Three times Font//
            BtSPP.SPPWrite(new byte[]{0x1B, 0x45, 0x01});    //Bold
            BtSPP.SPPWrite(new byte[]{0x1B, 0x2D, 0x32});    //2point under line
            BtSPP.SPPWrite("GENERALSCAN\n".getBytes("UTF-8"));

            BtSPP.SPPWrite(new byte[]{0x1B, 0x61, 0x00});    //Left Alignment
            BtSPP.SPPWrite(new byte[]{0x1B, 0x21, 0x00});    //Reset Default font size,cancel underline ,cancel bold font
            BtSPP.SPPWrite("(2.Barcode Left Align)\n".getBytes("UTF-8"));
            //BtSPP.SPPWrite(new byte[]{0x1D,0x6B,0x45,0x09,0x31,0x39,0x41,0x54,0x5A});	//Code39 Barcode
            BtSPP.SPPWrite(new byte[]{0x1D, 0x6B, 0x45, 0x0B, 0x47, 0x45, 0x4e, 0x45, 0x52, 0x41, 0x4c, 0x53, 0x43, 0x41, 0x4e});    //Code39 Barcode
            //0x47,0x45,0x4e,0x45,0x52,0x41,0x4c,0x53,0x43,0x41,0x4e
            BtSPP.SPPWrite("\n".getBytes("UTF-8"));

            BtSPP.SPPWrite("(3.Content-1Default Print Mode Absolute Print Position5mm)\n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1B, 0x24, 0x28, (byte) 0x80});    // Set Absolute Print Position5mm 80 means 00
            BtSPP.SPPWrite("1.0123456789-ABCD-9876543210 www.generalscan.com. 2.0123456789-ABCD-9876543210.www.generalscan.com 3.0123456789-ABCD-9876543210.www.generalscan.com\n".getBytes("UTF-8"));

            BtSPP.SPPWrite("4.Continue..., Align Center)\n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1B, 0x61, 0x01});    //Center
            BtSPP.SPPWrite(new byte[]{0x1D, 0x6B, 0x45, 0x0B, 0x47, 0x45, 0x4e, 0x45, 0x52, 0x41, 0x4c, 0x53, 0x43, 0x41, 0x4e});    //Code39 Barcode //code 39 GENERALSCANBtSPP.SPPWrite("\n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1B, 0x61, 0x00});    //Left Alignment
            BtSPP.SPPWrite("\n".getBytes("UTF-8"));

            BtSPP.SPPWrite("(5.Left Align  0x3F Point Line Space)\n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1B, 0x33, 0x1E});    //Set Line Space =3.75mm
            BtSPP.SPPWrite("www.companionscanner.com\n".getBytes("UTF-8"));
            BtSPP.SPPWrite("www.generalscan.com\n".getBytes("UTF-8"));

            BtSPP.SPPWrite(new byte[]{0x1B, 0x32});    //Set Default Line Space =1mm
            BtSPP.SPPWrite("(6Default Line Space)\n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1B, 0x24, 0x28, (byte) 0x80});    //Set absolute print position =5mm 90 mean 00
            BtSPP.SPPWrite("10123456789-ABCD-9876543210www.generalscan.com. 20123456789-ABCD-9876543210www.generalscan.com30123456789-ABCD-9876543210www.generalscan.com.\n".getBytes("UTF-8"));

            BtSPP.SPPWrite("(7 .Barcode)\n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1D, 0x6B, 0x45, 0x0B, 0x47, 0x45, 0x4e, 0x45, 0x52, 0x41, 0x4c, 0x53, 0x43, 0x41, 0x4e});    //Code39 Barcode
            BtSPP.SPPWrite("\n".getBytes("UTF-8"));

            // 0x77,0x77,0x77,0x2e,0x67,0x65,0x6e,0x65,0x72,0x61,0x6c,0x73,0x63,0x61,0x6e,0x2e,0x63,0x6f,0x6d
            BtSPP.SPPWrite("(8. 0 Line Space)\n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1B, 0x33, 0x00});    //Set Line Space=0mm
            BtSPP.SPPWrite(new byte[]{0x1B, 0x24, 0x28, (byte) 0x80});    //Set Absolute Postion5 mm.80 Mean 00
            BtSPP.SPPWrite("10123456789-ABCD-9876543210www.generalscan.com .20123456789-ABCD-9876543210. 3.0123456789-ABCD-9876543210.\n".getBytes("UTF-8"));

            BtSPP.SPPWrite("9. Barcode\n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1D, 0x6B, 0x03, 0x30, 0x31, 0x37, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x00});    //EAN13 0171234567895
            BtSPP.SPPWrite("\n".getBytes("UTF-8"));

            BtSPP.SPPWrite("103 Storage Picture\n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1C, 0x50, 0x01});    //Print Number01 Picture
            BtSPP.SPPWrite(new byte[]{0x1C, 0x50, 0x02});    //Print Number02 Picture
            BtSPP.SPPWrite(new byte[]{0x1C, 0x50, 0x03});    //Print Number03 Picture
            BtSPP.SPPWrite("\n".getBytes("UTF-8"));

            BtSPP.SPPWrite("1116point character\n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1B, 0x40});        //Reset Printer
            BtSPP.SPPWrite(new byte[]{0x1B, 0x4D, 0x01});        //Choose 16 point Character
            BtSPP.SPPWrite("10123456789-ABCD-987654321020123456789-ABCD-987654321030123456789-ABCD-9876543210\n".getBytes("UTF-8"));

            BtSPP.SPPWrite(new byte[]{0x1B, 0x4D, 0x00});        //Select 24 point Font
            BtSPP.SPPWrite("12Default Font Size,PDF 417 Stack Barcode\n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1D, 0x77, 0x04});        //4 Times Barcode Width
            BtSPP.SPPWrite(new byte[]{0x1D, 0x5A, 0x00});        //Select PDF417 Barcode
            /*BtSPP.SPPWrite(new byte[]{
						0x1B,0x5A,0x02,0x02,0x03,0x39,0x00,(byte) 0xC9,(byte) 0xCF,(byte) 0xBA,(byte) 0xA3,(byte) 0xD6,(byte) 0xA5,
						(byte) 0xBF,(byte) 0xC2,(byte) 0xB4,(byte) 0xF2,(byte) 0xD3,(byte) 0xA1,(byte) 0xBC,(byte) 0xBC,(byte) 0xCA,
						(byte) 0xF5,(byte) 0xD3,(byte) 0xD0,(byte) 0xCF,(byte) 0xDE,(byte) 0xB9,(byte) 0xAB,(byte) 0xCB,(byte) 0xBE,
						0x20,0x20,(byte) 0xB5,(byte) 0xD8,(byte) 0xD6,(byte) 0xB7,(byte) 0xA3,(byte) 0xBA,(byte) 0xC9,(byte) 0xCF,
						(byte) 0xBA,(byte) 0xA3,(byte) 0xC6,(byte) 0xD6,(byte) 0xB6,(byte) 0xAB,(byte) 0xD0,(byte) 0xE3,(byte) 0xC6,
						(byte) 0xCC,(byte) 0xC2,(byte) 0xB7,0x33,0x39,0x39,0x39,(byte) 0xBA,(byte) 0xC5,0x36,(byte) 0xBA,(byte) 0xC5,
						(byte) 0xC2,(byte) 0xA5 });
						*/
            BtSPP.SPPWrite(new byte[]{
                    0x1B, 0x5A, 0x02, 0x02, 0x03, 0x24, 0x00, (byte) 0x47, (byte) 0x45, (byte) 0x4E, (byte) 0x45, (byte) 0x52, (byte) 0x41,
                    (byte) 0x4C, (byte) 0x53, (byte) 0x43, (byte) 0x41, (byte) 0x4E, (byte) 0x20, (byte) 0x45, (byte) 0x4C, (byte) 0x45,
                    (byte) 0x43, (byte) 0x54, (byte) 0x52, (byte) 0x4F, (byte) 0x4E, (byte) 0x49, (byte) 0x43, (byte) 0x53, (byte) 0x20,
                    (byte) 0x43, (byte) 0x4F, (byte) 0x2E, (byte) 0x2C, (byte) 0x20, (byte) 0x4C, (byte) 0x49, (byte) 0x4D, (byte) 0x49,
                    (byte) 0x54, (byte) 0x45, (byte) 0x44});        //Print PDF417 Barcode

            BtSPP.SPPWrite("\n".getBytes("UTF-8"));

            BtSPP.SPPWrite("13 Invert Print\n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1B, 0x40});        //Reset Printer
            BtSPP.SPPWrite(new byte[]{0x1D, 0x42, 0x01});        //Select Revert Print
            BtSPP.SPPWrite("10123456789-ABCD-9876543210 www.generalscan.com\n".getBytes("UTF-8"));

            BtSPP.SPPWrite(new byte[]{0x1B, 0x40});        //Reset Machine
            BtSPP.SPPWrite(new byte[]{0x1D, 0x42, 0x00});        //Cancel Revert Print
            BtSPP.SPPWrite("(14 Barcode.10 point Height Barcode)\n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1D, 0x68, 0x0A});        //Set Print Height 10 point 1.25mm
            BtSPP.SPPWrite(new byte[]{0x1D, 0x6B, 0x04, 0x2A, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x2A, 0x00});        //Print Code39 *123456* //��ӡcode39�룺*123456*
            BtSPP.SPPWrite("\n".getBytes("UTF-8"));

            BtSPP.SPPWrite("15 Context\n".getBytes("UTF-8"));
            BtSPP.SPPWrite("10123456789-GENERALSCAN -9876543210\n".getBytes("UTF-8"));

            BtSPP.SPPWrite("16 Barcode 18 Point Height Barcode\n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1D, 0x68, 0x12});        //Set Barcode Height to 18 point 2.25mm
            BtSPP.SPPWrite(new byte[]{0x1D, 0x6B, 0x04, 0x2A, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x2A, 0x00});        //code39*123456*
            BtSPP.SPPWrite("\n".getBytes("UTF-8"));

            BtSPP.SPPWrite("17 8 Tab Character\n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1B, 0x44, 0x04, 0x09, 0x0F, 0x14, 0x19, 0x1F, 0x24, 0x29, 0x2F, 0x34, 0x39, 0x3F, (byte) 0xff});        //����12��ˮƽ�Ʊ�λ����Ϊ���Ϊ8�����к���4����Ч
            BtSPP.SPPWrite(new byte[]{0x09});        //First
            BtSPP.SPPWrite("First".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x09});        //Second
            BtSPP.SPPWrite("Second".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x09});        //Third
            BtSPP.SPPWrite("Third".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x09});        //Fourth
            BtSPP.SPPWrite("Fourth".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x09});        //Fifth
            BtSPP.SPPWrite("Fifth".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x09});        //Sixth
            BtSPP.SPPWrite("Sixth".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x09});        //Seventh
            BtSPP.SPPWrite("Seventh".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x09});        //Eighth
            BtSPP.SPPWrite("Eighth".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x09});        //Ninth
            BtSPP.SPPWrite("Ninth".getBytes("UTF-8"));
            BtSPP.SPPWrite("\n".getBytes("UTF-8"));

            BtSPP.SPPWrite("18 Barcode\n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1D, 0x6B, 0x01, 0x30, 0x31, 0x39, 0x37, 0x37, 0x31, 0x31, 0x30, 0x00});        //Print UPC-EBarcode
            BtSPP.SPPWrite("\n".getBytes("UTF-8"));
            //30 31 39 37 37 31 31 30
            BtSPP.SPPWrite("19 0X30 Point Character Spacing\n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1B, 0x20, 0x30});        //Set for Character Spacing
            BtSPP.SPPWrite("10123456789-GENERALSCAN-9876543210\n".getBytes("UTF-8"));

            BtSPP.SPPWrite(new byte[]{0x1B, 0x20, 0x00});        //Set Character Spacing=00
            BtSPP.SPPWrite("20 Bold Underline 2 Time\n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1b, 0x21, (byte) 0xfe});        //Set Font Bold,Underline ,2 Times
            BtSPP.SPPWrite("10123456789-GENERALSCAN-9876543210\n".getBytes("UTF-8"));

            BtSPP.SPPWrite(new byte[]{0x1B, 0x40});
            BtSPP.SPPWrite("21 Barcode\n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1D, 0x6B, 0x01, 0x30, 0x31, 0x39, 0x37, 0x37, 0x31, 0x31, 0x30, 0x00});        //Print UPC-EBarcode
            BtSPP.SPPWrite("\n".getBytes("UTF-8"));

            BtSPP.SPPWrite("22 Left Margin 0x2F\n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1D, 0x4C, 0x2F, 0x00});        //Set Left Margin 0x2F
            BtSPP.SPPWrite("10123456789-GENERALSCAN-987654321020123456789-GENERALSCAN-987654321030123456789-GENERALSCAN-987654321040123456789-GENERALSCAN-9876543210\n".getBytes("UTF-8"));
            BtSPP.SPPWrite("23 Vertical Barcode\n".getBytes("UTF-8"));

            BtSPP.SPPWrite("(24 Barcode) \n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1D, 0x6B, 0x01, 0x30, 0x31, 0x39, 0x37, 0x37, 0x31, 0x31, 0x30, 0x00});        //Print UPC-EBarcode
            BtSPP.SPPWrite("\n".getBytes("UTF-8"));

            BtSPP.SPPWrite("(25 Rotation 90 degree) \n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1B, 0x40});        //Reset Printer
            BtSPP.SPPWrite(new byte[]{0x1B, 0x56, 0x01});        //Set clockwise 90 degree
            BtSPP.SPPWrite("10123456789-GENERALSCAN-9876543210 \n".getBytes("UTF-8"));

            BtSPP.SPPWrite(new byte[]{0x1B, 0x56, 0x00});        //Set No Rotation
            BtSPP.SPPWrite("(26 Barcode) \n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1D, 0x6B, 0x01, 0x30, 0x31, 0x39, 0x37, 0x37, 0x31, 0x31, 0x30, 0x00});        //Print UPC-EBarcode
            BtSPP.SPPWrite("\n".getBytes("UTF-8"));

            BtSPP.SPPWrite("(27 Roation 180 degree )\n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1B, 0x56, 0x02});        //Set clockwise 180 degree
            BtSPP.SPPWrite("10123456789-GENERALSCAN-9876543210 \n".getBytes("UTF-8"));

            BtSPP.SPPWrite(new byte[]{0x1B, 0x56, 0x00});        //Set No Rotation
            BtSPP.SPPWrite("(28 2D DataMatrix Barcode )\n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1D, 0x77, 0x04});        //Set 4 times barcode
            //BtSPP.SPPWrite(new byte[]{0x1D,0x5A,0x01});		//Select DataMatrix Barcode
            //BtSPP.SPPWrite(new byte[]{
            //		0x1B,0x5A,0x00,0x00,0x00,0x36,0x00,(byte) 0x47,(byte) 0x45,(byte) 0x4E,(byte) 0x45,(byte) 0x52,(byte) 0x41,
            //		(byte) 0x4C,(byte) 0x53,(byte) 0x43,(byte) 0x41,(byte) 0x4E,(byte) 0x20,(byte) 0x45,(byte) 0x4C,(byte) 0x45,
            //		(byte) 0x43,(byte) 0x54,(byte) 0x52,(byte) 0x4F,(byte) 0x4E,(byte) 0x49,(byte) 0x43,(byte) 0x53,(byte) 0x20,
            //		(byte) 0x43,(byte) 0x4F,(byte) 0x2E,(byte) 0x2C,(byte) 0x20,(byte) 0x4C,(byte) 0x49,(byte) 0x4D,(byte) 0x49,
            //		(byte) 0x54,(byte) 0x45,(byte) 0x44 });		//Print DataMatrix

            BtSPP.SPPWrite(new byte[]{0x1D, 0x5A, 0x01});        //DataMatrix
            zp_realtime_status(100);
            BtSPP.SPPWrite(new byte[]{
                    0x1B, 0x5A, 0x00, 0x00, 0x00, 0x39, 0x00, (byte) 0xC9, (byte) 0xCF, (byte) 0xBA, (byte) 0xA3,
                    (byte) 0xD6, (byte) 0xA5, (byte) 0xBF, (byte) 0xC2, (byte) 0xB4, (byte) 0xF2, (byte) 0xD3,
                    (byte) 0xA1, (byte) 0xBC, (byte) 0xBC, (byte) 0xCA, (byte) 0xF5, (byte) 0xD3, (byte) 0xD0,
                    (byte) 0xCF, (byte) 0xDE, (byte) 0xB9, (byte) 0xAB, (byte) 0xCB, (byte) 0xBE, 0x20, 0x20,
                    (byte) 0xB5, (byte) 0xD8, (byte) 0xD6, (byte) 0xB7, (byte) 0xA3, (byte) 0xBA, (byte) 0xC9,
                    (byte) 0xCF, (byte) 0xBA, (byte) 0xA3, (byte) 0xC6, (byte) 0xD6, (byte) 0xB6, (byte) 0xAB,
                    (byte) 0xD0, (byte) 0xE3, (byte) 0xC6, (byte) 0xCC, (byte) 0xC2, (byte) 0xB7, 0x33, 0x39,
                    0x39, 0x39, (byte) 0xBA, (byte) 0xC5, 0x36, (byte) 0xBA, (byte) 0xC5, (byte) 0xC2, (byte) 0xA5
            });

            BtSPP.SPPWrite("\n".getBytes("UTF-8"));


            BtSPP.SPPWrite("(29 Rotation 270 degree)\n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1B, 0x56, 0x03});        //Set clockwise 270 degree
            BtSPP.SPPWrite("10123456789-GENERALSCAN-9876543210 \n".getBytes("UTF-8"));

            BtSPP.SPPWrite(new byte[]{0x1B, 0x56, 0x00});        //Set No Rotation
            BtSPP.SPPWrite("(30 2D QR-CODE )\n".getBytes("UTF-8"));
            BtSPP.SPPWrite(new byte[]{0x1D, 0x77, 0x04});        //Set 4 times barcode width
            BtSPP.SPPWrite(new byte[]{0x1D, 0x5A, 0x02});        //Select QR Code


            BtSPP.SPPWrite(new byte[]{
                    0x1B, 0x5A, 0x00, 0x00, 0x00, 0x24, 0x00, (byte) 0x47, (byte) 0x45, (byte) 0x4E, (byte) 0x45, (byte) 0x52, (byte) 0x41,
                    (byte) 0x4C, (byte) 0x53, (byte) 0x43, (byte) 0x41, (byte) 0x4E, (byte) 0x20, (byte) 0x45, (byte) 0x4C, (byte) 0x45,
                    (byte) 0x43, (byte) 0x54, (byte) 0x52, (byte) 0x4F, (byte) 0x4E, (byte) 0x49, (byte) 0x43, (byte) 0x53, (byte) 0x20,
                    (byte) 0x43, (byte) 0x4F, (byte) 0x2E, (byte) 0x2C, (byte) 0x20, (byte) 0x4C, (byte) 0x49, (byte) 0x4D, (byte) 0x49,
                    (byte) 0x54, (byte) 0x45, (byte) 0x44});        //Print QR

            BtSPP.SPPWrite("\n".getBytes("UTF-8"));


            BtSPP.SPPWrite(new byte[]{0x1B, 0x40});        //Reset Printer
            BtSPP.SPPWrite(new byte[]{0x1B, 0x61, 0x01});    //Center
            BtSPP.SPPWrite("===================================== \n".getBytes("UTF-8"));
            BtSPP.SPPWrite("\n \n".getBytes("UTF-8"));
            BtSPP.SPPWrite("******** Generalscan Bluetooth \n Printer Test Finish ******** \n".getBytes("UTF-8"));
            BtSPP.SPPWrite("\n \n".getBytes("UTF-8"));
            if (zp_realtime_status(5000) > 0)
                showMessage(ErrorMessage);
            statusBox.Close();
        } catch (UnsupportedEncodingException e) {
        }
        BtSPP.SPPClose();
    }

}