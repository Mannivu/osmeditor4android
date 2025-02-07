package de.blau.android.presets;

import android.support.annotation.NonNull;
import android.support.v4.widget.TextViewCompat;
import android.util.TypedValue;
import android.widget.TextView;

public final class TextSize {
    
    /**
     * Private default constructor
     */
    private TextSize() {
        // nothing
    }

    /**
     * Wrapper for flavor compatibility
     * 
     * @param v the TextView we want to set the size on
     */
    static void setIconTextSize(@NonNull TextView v) {
        int[] sizes = { 8, 10 };
        TextViewCompat.setAutoSizeTextTypeUniformWithPresetSizes(v, sizes, TypedValue.COMPLEX_UNIT_SP);
    }
}
