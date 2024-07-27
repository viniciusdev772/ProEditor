package com.viniciusdev.proeditor;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

public class ColorPickerDialog extends Dialog {

    private int red, green, blue, transparency;
    private TextView colorPreview, colorHex, colorArgb, textViewRValue, textViewGValue, textViewBValue, textViewTValue;
    private SeekBar seekBarR, seekBarG, seekBarB, seekBarT;
    private int initialColor;

    public ColorPickerDialog(@NonNull Context context, float initialColor) {
        super(context, R.style.TransparentDialog);
        this.initialColor = (int) initialColor;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_color_picker);

        // Ajustar a largura do diálogo para ocupar toda a largura da tela
        Window window = getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(layoutParams);
        }

        colorPreview = findViewById(R.id.color_preview);
        colorHex = findViewById(R.id.color_hex);
        colorArgb = findViewById(R.id.color_argb);
        textViewRValue = findViewById(R.id.textview_r_value);
        textViewGValue = findViewById(R.id.textview_g_value);
        textViewBValue = findViewById(R.id.textview_b_value);
        textViewTValue = findViewById(R.id.textview_t_value);

        seekBarR = findViewById(R.id.seekbar_r);
        seekBarG = findViewById(R.id.seekbar_g);
        seekBarB = findViewById(R.id.seekbar_b);
        seekBarT = findViewById(R.id.seekbar_t);
        Button buttonApply = findViewById(R.id.button_apply);

        // Inicializar os valores dos SeekBars com base na cor inicial
        transparency = Color.alpha(initialColor);
        red = Color.red(initialColor);
        green = Color.green(initialColor);
        blue = Color.blue(initialColor);

        seekBarR.setProgress(red);
        seekBarG.setProgress(green);
        seekBarB.setProgress(blue);
        seekBarT.setProgress(transparency);

        textViewRValue.setText(String.valueOf(red));
        textViewGValue.setText(String.valueOf(green));
        textViewBValue.setText(String.valueOf(blue));
        textViewTValue.setText(String.valueOf(transparency));

        seekBarR.setOnSeekBarChangeListener(colorSeekBarChangeListener);
        seekBarG.setOnSeekBarChangeListener(colorSeekBarChangeListener);
        seekBarB.setOnSeekBarChangeListener(colorSeekBarChangeListener);
        seekBarT.setOnSeekBarChangeListener(colorSeekBarChangeListener);

        seekBarR.getProgressDrawable().setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN);
        seekBarR.getThumb().setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN);

        seekBarG.getProgressDrawable().setColorFilter(Color.GREEN, android.graphics.PorterDuff.Mode.SRC_IN);
        seekBarG.getThumb().setColorFilter(Color.GREEN, android.graphics.PorterDuff.Mode.SRC_IN);

        seekBarB.getProgressDrawable().setColorFilter(Color.BLUE, android.graphics.PorterDuff.Mode.SRC_IN);
        seekBarB.getThumb().setColorFilter(Color.BLUE, android.graphics.PorterDuff.Mode.SRC_IN);

        seekBarT.getProgressDrawable().setColorFilter(Color.BLACK, android.graphics.PorterDuff.Mode.SRC_IN);
        seekBarT.getThumb().setColorFilter(Color.BLACK, android.graphics.PorterDuff.Mode.SRC_IN);

        buttonApply.setOnClickListener(v -> copyColorToClipboard());

        updateColorPreview();
    }

    private final SeekBar.OnSeekBarChangeListener colorSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            int seekBarId = seekBar.getId();
            if (seekBarId == R.id.seekbar_r) {
                red = progress;
                textViewRValue.setText(String.valueOf(progress));
            } else if (seekBarId == R.id.seekbar_g) {
                green = progress;
                textViewGValue.setText(String.valueOf(progress));
            } else if (seekBarId == R.id.seekbar_b) {
                blue = progress;
                textViewBValue.setText(String.valueOf(progress));
            } else if (seekBarId == R.id.seekbar_t) {
                transparency = progress;
                textViewTValue.setText(String.valueOf(progress));
            }
            updateColorPreview();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // Not needed
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // Not needed
        }
    };

    private void updateColorPreview() {
        int color = Color.argb(transparency, red, green, blue);
        colorPreview.setBackgroundColor(color);

        // Calcular o brilho da cor
        double brightness = Math.sqrt(
                0.299 * Math.pow(red, 2) +
                        0.587 * Math.pow(green, 2) +
                        0.114 * Math.pow(blue, 2)
        );

        // Ajustar a cor do texto com base no brilho
        if (brightness < 130) {
            colorPreview.setTextColor(Color.WHITE);
        } else {
            colorPreview.setTextColor(Color.BLACK);
        }

        String hexColor = String.format("#%02X%02X%02X%02X", transparency, red, green, blue);
        colorHex.setText("HEX: " + hexColor);
        colorArgb.setText(String.format("ARGB: %d, %d, %d, %d", transparency, red, green, blue));
    }

    private void copyColorToClipboard() {
        int color = Color.argb(transparency, red, green, blue);
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Color", String.valueOf(color));
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getContext(), "Color copied: " + color, Toast.LENGTH_SHORT).show();
    }
}
