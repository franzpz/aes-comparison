package at.fhj.ims.powermeasurementaes;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import droidninja.filepicker.FilePickerBuilder;

public class MainActivity extends AppCompatActivity {

    private TextView _log;
    private CheckBox _logToDisplay;
    private EditText _filePath;
    private EditText _cipher;

    private boolean shouldLogToDisplay() {
        return _logToDisplay.isChecked();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _log = (TextView)this.findViewById(R.id.logArea);
        _logToDisplay = (CheckBox) this.findViewById(R.id.writeLogsToDisplay);
        _filePath = (EditText)this.findViewById(R.id.filepath);
        _cipher = (EditText)this.findViewById(R.id.cipher);

    }

    public void chooseFile(View view){

        openFile("*/*");
        /**/FilePickerBuilder.getInstance().setMaxCount(1)
                .setActivityTheme(R.style.AppTheme)
                .pickDocument(this);
    }

    public void openFile(String minmeType) {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(minmeType);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // special intent for Samsung file manager
        Intent sIntent = new Intent("com.sec.android.app.myfiles.PICK_DATA");
        // if you want any file type, you can skip next line
        //sIntent.putExtra("CONTENT_TYPE", minmeType);
        sIntent.addCategory(Intent.CATEGORY_DEFAULT);

        Intent chooserIntent;
        if (getPackageManager().resolveActivity(sIntent, 0) != null){
            // it is device with samsung file manager
            chooserIntent = Intent.createChooser(sIntent, "Open file");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { intent});
        }
        else {
            chooserIntent = Intent.createChooser(intent, "Open file");
        }

        try {
            startActivityForResult(chooserIntent, 12);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getApplicationContext(), "No suitable File Manager was found.", Toast.LENGTH_SHORT).show();
        }
    }
/*
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode)
        {
            case FilePickerConst.REQUEST_CODE_DOC:
                if(resultCode== Activity.RESULT_OK && data!=null)
                {
                    docPaths = new ArrayList<>();
                    docPaths.addAll(data.getStringArrayListExtra(FilePickerConst.KEY_SELECTED_DOCS));
                }
                break;
        }
        for (String file: docPaths) {
            log("Selected file: " + file);
        }
    }*/

    private class DecryptTask extends AsyncTask<String, Integer, Float> {
        protected Float doInBackground(String... input) {
            Date start = new Date();

            try {

                // Here you read the cleartext.
                FileInputStream fis = new FileInputStream(input[0]);
                // This stream write the encrypted text. This stream will be wrapped by another stream.
                FileOutputStream fos = new FileOutputStream(input[0] + ".decrypted");


                // Length is 16 byte
                // Careful when taking user input!!! http://stackoverflow.com/a/3452620/1188357
                SecretKeySpec sks = new SecretKeySpec("MyDifficultPassw".getBytes(), input[1]);
                // Create cipher
                Cipher cipher = Cipher.getInstance(input[1]);
                cipher.init(Cipher.DECRYPT_MODE, sks);
                // Wrap the output stream
                CipherOutputStream cos = new CipherOutputStream(fos, cipher);
                // Write bytes
                int b;
                byte[] d = new byte[8];
                while ((b = fis.read(d)) != -1) {
                    cos.write(d, 0, b);
                }
                // Flush and close streams.
                cos.flush();
                cos.close();
                fis.close();
            }
            catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException ex) {
                Log.e("Decrypt", "Exception", ex);
                log("Decrypt - Exception - " + ex.getMessage());
            }

            Date end = new Date();

            return (float)(end.getTime() - start.getTime())/1000;
        }

        protected void onProgressUpdate(Integer... progress) {
        }

        protected void onPostExecute(Float result) {
            log(String.format("Decryption done, took %.4f seconds", result));
        }
    }

    public void decrypt(View view){
        log("Start Decryption");
        new DecryptTask().execute(_filePath.getText().toString(), _cipher.getText().toString());
    }

    private class EncryptTask extends AsyncTask<String, Integer, Float> {
        protected Float doInBackground(String... input) {
            Date start = new Date();

            try {

                // Here you read the cleartext.
                FileInputStream fis = new FileInputStream(input[0]);
                // This stream write the encrypted text. This stream will be wrapped by another stream.
                FileOutputStream fos = new FileOutputStream(input[0] + ".encrypted");


                // Length is 16 byte
                // Careful when taking user input!!! http://stackoverflow.com/a/3452620/1188357
                SecretKeySpec sks = new SecretKeySpec("MyDifficultPassw".getBytes(), input[1]);
                // Create cipher
                Cipher cipher = Cipher.getInstance(input[1]);
                cipher.init(Cipher.ENCRYPT_MODE, sks);
                // Wrap the output stream
                CipherOutputStream cos = new CipherOutputStream(fos, cipher);
                // Write bytes
                int b;
                byte[] d = new byte[8];
                while ((b = fis.read(d)) != -1) {
                    cos.write(d, 0, b);
                }
                // Flush and close streams.
                cos.flush();
                cos.close();
                fis.close();
            }
            catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException ex) {
                Log.e("Encrypt", "Exception", ex);
                log("Encrypt - Exception - " + ex.getMessage());
            }

            Date end = new Date();

            return (float)(end.getTime() - start.getTime())/1000;
        }

        protected void onProgressUpdate(Integer... progress) {
        }

        protected void onPostExecute(Float result) {
            log(String.format(new Date().toString() + " - Encryption done, took %.4f seconds", result));
        }
    }

    public void encrypt(View view){

        log(new Date().toString() + " - Start Encryption");
        new EncryptTask().execute(_filePath.getText().toString(), _cipher.getText().toString());
/*
        Date start = new Date();

        try {
            // Here you read the cleartext.
            FileInputStream fis = new FileInputStream("sdcard/cleartext");
            // This stream write the encrypted text. This stream will be wrapped by another stream.
            FileOutputStream fos = new FileOutputStream("sdcard/encrypted");

            // Length is 16 byte
            // Careful when taking user input!!! http://stackoverflow.com/a/3452620/1188357
            SecretKeySpec sks = new SecretKeySpec("MyDifficultPassw".getBytes(), "AES");
            // Create cipher
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, sks);
            // Wrap the output stream
            CipherOutputStream cos = new CipherOutputStream(fos, cipher);
            // Write bytes
            int b;
            byte[] d = new byte[8];
            while ((b = fis.read(d)) != -1) {
                cos.write(d, 0, b);
            }
            // Flush and close streams.
            cos.flush();
            cos.close();
            fis.close();
        }
        catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException ex) {
            Log.e("Encrypt", "Exception", ex);
            log("Encrypt - Exception - " + ex.getMessage());
        }

        Date end = new Date();

        log(String.format("Encryption done, took %.4f seconds", (float)(end.getTime() - start.getTime())/1000));*/
    }

    private void log(String text){

        Log.i("AES", text);

        if(_logToDisplay.isChecked())
            _log.append(text + "\n");
    }


}
