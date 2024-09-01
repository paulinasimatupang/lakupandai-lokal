package id.co.bankntbsyariah.lakupandai.ui;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.github.gcacace.signaturepad.views.SignaturePad;
import okhttp3.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Executors;
import id.co.bankntbsyariah.lakupandai.R;

public class SignatureActivity extends AppCompatActivity {

    private SignaturePad signaturePad;
    private Button clearButton, saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signature);
        signaturePad = findViewById(R.id.signature_pad);
        clearButton = findViewById(R.id.clear_button);
        saveButton = findViewById(R.id.save_button);

        clearButton.setOnClickListener(view -> signaturePad.clear());

        saveButton.setOnClickListener(view -> {
            if (!signaturePad.isEmpty()) {
                Bitmap signatureBitmap = signaturePad.getSignatureBitmap();
                File file = createFileFromBitmap(signatureBitmap, "coba1.png");

                saveSignatureToServer(file);
            } else {
                Toast.makeText(this, "Please provide a signature.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private File createFileFromBitmap(Bitmap bitmap, String fileName) {
        File file = new File(getCacheDir(), fileName);
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    private void saveSignatureToServer(File file) {
        Executors.newSingleThreadExecutor().execute(() -> {
            OkHttpClient client = new OkHttpClient();

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getName(),
                            RequestBody.create(MediaType.parse("image/png"), file))
                    .build();

            Request request = new Request.Builder()
                    .url("http://108.137.154.8:8081/ARRest/images")
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(SignatureActivity.this, "Signature uploaded successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(SignatureActivity.this, "Failed to upload signature", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(SignatureActivity.this, "Error uploading signature", Toast.LENGTH_SHORT).show());
            }
        });
    }
}
