package com.adarshahd.indianrailinfo.donate;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ahd on 6/8/13.
 */
public class PNRDatabase {
//    private static final String PACKAGE_NAME="com."
    public static PNRDatabase mDatabase;
    public static File mDir;
    public static File mFilePNR;
    public static File mFilePNRTrack;
    private static Context mContext;
    private static ArrayList<String> mPNRList;

    private PNRDatabase(Context context) {
        mContext = context;
        mDir = new File(Environment.getDataDirectory() + "/data/" + mContext.getPackageName() + "/files");
        if(!mDir.exists()) {
            mDir.mkdirs();
        }
        mFilePNR = new File(mDir,"PNRs.dat");
        mFilePNRTrack = new File(mDir, "PNRTrack.dat");
        mPNRList = new ArrayList<String>();
    }

    public static PNRDatabase getPNRDatabase(Context context) {
        if(mDatabase == null) {
            mDatabase = new PNRDatabase(context);
        }
        return mDatabase;
    }

    public static File getPNRDir() {
        return mDir;
    }

    public static boolean addPNR (String pnr, String trainDetails, List<String> passengerDetails) {
        File filePNR = new File(mDir,pnr + ".dat");
        FileOutputStream out;
        OutputStreamWriter writer;
        try {
            out = new FileOutputStream(filePNR);
            writer = new OutputStreamWriter(out);
            writer.write(trainDetails);
            writer.append("\n");
            for(int i=0; i<passengerDetails.size();++i) {
                writer.append(passengerDetails.get(i));
                writer.append("\n");
            }
            writer.flush();
            ArrayList<String> pnrs = (ArrayList<String>) getPNRs();
            if(pnrs.contains(pnr)) {
                Log.i("PNRDatabase: ","PNR already exists in database");
                return true;
            }
            out = mContext.openFileOutput(mFilePNR.getName(),Context.MODE_APPEND);
            writer = new OutputStreamWriter(out);
            writer.append(pnr);
            writer.append("\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static List<String> getPNRs() {
        ArrayList<String> res = new ArrayList<String>();
        try {
            if(!mFilePNR.exists()) {
                return new ArrayList<String>();
            }
            FileInputStream stream = new FileInputStream(mFilePNR);
            InputStreamReader reader = new InputStreamReader(stream);
            BufferedReader read = new BufferedReader(reader);
            String tmp ="";
            while((tmp = read.readLine()) != null) {
                res.add(tmp);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return res;
    }

    public static File getFileForPNR(String pnr) {
        File file = new File(mDir,pnr);
        if(file.exists()) {
            return file;
        } else {
            return null;
        }
    }

    public static void addPNRToTrack(String pnrNumber) {
        if(getPNRTrackList().contains(pnrNumber)) {
            return;
        }
        FileOutputStream out;
        OutputStreamWriter writer;
        try {
            out = mContext.openFileOutput(mFilePNRTrack.getName(),Context.MODE_APPEND);
            writer = new OutputStreamWriter(out);
            writer.append(pnrNumber);
            writer.append("\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> getPNRTrackList() {
        FileInputStream stream;
        InputStreamReader read;
        BufferedReader reader;
        ArrayList<String> list = new ArrayList<String>();
        try {
            stream = new FileInputStream(mFilePNRTrack);
            read = new InputStreamReader(stream);
            reader = new BufferedReader(read);
            String tmp = "";
            while((tmp = reader.readLine()) != null) {
                list.add(tmp);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean clearPNRDatabase() {
        mPNRList = (ArrayList<String>) getPNRs();
        for(String pnr : mPNRList) {
            File pnrFile = new File(mDir,pnr);
            if (pnrFile.exists()) {
                pnrFile.delete();
            }
        }
        if(mFilePNR.exists()) {
            mFilePNR.delete();
        }
        return true;
    }

    public void stopTrackingPNRs(ArrayList<String> listOfPNRsNotToTrack) {
        ArrayList<String> listOfPNRsToTrack = (ArrayList<String>) getPNRTrackList();
        for (String pnr : listOfPNRsNotToTrack) {
            listOfPNRsToTrack.remove(pnr);
        }
        if (mFilePNRTrack.exists()) {
            mFilePNRTrack.delete();
        }
        mFilePNRTrack = new File(mDir, "PNRTrack.dat");
        for(String pnr : listOfPNRsToTrack) {
            FileOutputStream out;
            OutputStreamWriter writer;
            try {
                out = mContext.openFileOutput(mFilePNRTrack.getName(),Context.MODE_APPEND);
                writer = new OutputStreamWriter(out);
                writer.append(pnr);
                writer.append("\n");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
