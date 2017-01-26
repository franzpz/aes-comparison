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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import droidninja.filepicker.FilePickerBuilder;

class CryptoInput {
    public int Iterations;
    public String FilePath;
}

class CryptoOutput {
    public String NewFilePath;
    public String ProviderUsed;
    public Float TimeSpent;
    public String Error = null;
}

public class MainActivity extends AppCompatActivity {

    private TextView _log;
    private CheckBox _logToDisplay;
    private EditText _filePath;
    private EditText _cipher;

    private static String algo = "PBEWithSHA256And256BitAES-CBC-BC";

    private SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");

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

        try {
            log(" Max key length supported on device: " + Cipher.getMaxAllowedKeyLength(algo));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
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


                byte[] salt = new byte[8];
                SecureRandom random = new SecureRandom();
                random.nextBytes(salt);

                // Create the key
                KeySpec keySpec = new PBEKeySpec("TestPassword".toCharArray(), salt, 1);
                SecretKey key = SecretKeyFactory.getInstance(
                        "PBEWithSHA256And256BitAES-CBC-BC").generateSecret(keySpec);

                Cipher ecipher = Cipher.getInstance(key.getAlgorithm());

                // Prepare the parameter to the ciphers
                AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt, 1);

                // Create the ciphers
                ecipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
                /*
                // Length is 16 byte
                // Careful when taking user input!!! http://stackoverflow.com/a/3452620/1188357
                SecretKeySpec sks = new SecretKeySpec("MyDifficultPassw".getBytes(), input[1]);
                // Create cipher
                Cipher cipher = Cipher.getInstance(input[1]);
                cipher.init(Cipher.DECRYPT_MODE, sks);
                // Wrap the output stream*/
                CipherOutputStream cos = new CipherOutputStream(fos, ecipher);
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
            catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | InvalidKeySpecException ex) {
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

    private class EncryptTask extends AsyncTask<CryptoInput, Integer, CryptoOutput> {
        protected CryptoOutput doInBackground(CryptoInput... input) {

            CryptoOutput output = new CryptoOutput();
            Date start = new Date();

            try {

                // Here you read the cleartext.
                FileInputStream fis = new FileInputStream(input[0].FilePath);
                // This stream write the encrypted text. This stream will be wrapped by another stream.
                output.NewFilePath = input[0].FilePath + ".encrypted";
                FileOutputStream fos = new FileOutputStream(output.NewFilePath);

                byte[] salt = new byte[8];
                SecureRandom random = new SecureRandom();
                random.nextBytes(salt);

                // Create the key
                KeySpec keySpec = new PBEKeySpec("TestPassword".toCharArray(), salt, input[0].Iterations);
                SecretKey key = SecretKeyFactory.getInstance(
                        algo).generateSecret(keySpec);

                Cipher ecipher = Cipher.getInstance(key.getAlgorithm());

                // Prepare the parameter to the ciphers
                AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt, input[0].Iterations);

                // Create the ciphers
                ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);


                output.ProviderUsed = ecipher.getProvider().getInfo();

                start = new Date();
                CipherOutputStream cos = new CipherOutputStream(fos, ecipher);
                //byte[] enc = ecipher.doFinal(utf8);

                /*

                SecretKeySpec secret = new SecretKeySpec("MyDifficultPassw".getBytes(), input[1]);
                Cipher ecipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                byte[] iv = new byte[IV_LENGTH];
                SecureRandom random = new SecureRandom();
                random.nextBytes(iv);
                ecipher.init(Cipher.ENCRYPT_MODE, secret, new IvParameterSpec(iv));

                // Length is 16 byte
                // Careful when taking user input!!! http://stackoverflow.com/a/3452620/1188357
                SecretKeySpec sks = new SecretKeySpec("MyDifficultPassw".getBytes(), input[1]);
                // Create cipher
                Cipher cipher = Cipher.getInstance(input[1]);
                cipher.init(Cipher.ENCRYPT_MODE, sks);
                // Wrap the output stream
                CipherOutputStream cos = new CipherOutputStream(fos, cipher);*/
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
            catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | InvalidKeySpecException ex) {
                Log.e("Encrypt", "Exception", ex);
                output.Error = "Encrypt - Exception - " + ex.getMessage();
            }

            Date end = new Date();
            output.TimeSpent = (float)(end.getTime() - start.getTime())/1000;

            return output;
        }

        protected void onProgressUpdate(Integer... progress) {
        }

        protected void onPostExecute(CryptoOutput result) {
            log("Created File: " + result.NewFilePath);
            log("Used Provider: " + result.ProviderUsed);
            if(result.Error != null)
                log("Error: " + result.Error);

            log(String.format(formatter.format(new Date()) + " - Encryption done, took %.4f seconds", result.TimeSpent));
        }
    }

    public void encrypt(View view){

        log(formatter.format(new Date()) + " - Start Encryption");
        CryptoInput input = new CryptoInput();
        input.FilePath = _filePath.getText().toString();
        input.Iterations = Integer.parseInt( _cipher.getText().toString());
        new EncryptTask().execute(input);
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
