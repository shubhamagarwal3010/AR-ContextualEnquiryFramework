package com.thoughtworks.onboarding;

import android.os.Bundle;

import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide;
import com.thoughtworks.onboarding.utils.Constants;
import com.thoughtworks.onboarding.utils.Prefs;

public class TutorialActivity extends IntroActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Prefs.putBoolean(Constants.PREF_IS_FIRST_TIME, true);

        setButtonBackVisible(false);
        setButtonNextVisible(false);
        setButtonCtaVisible(true);
        setButtonCtaTintMode(BUTTON_CTA_TINT_MODE_TEXT);

        addSlide(new SimpleSlide.Builder()
                .title(R.string.title_ar_intro1)
                .description(R.string.description_ar_intro1)
                .image(R.drawable.logo)
                .background(R.color.color_material_metaphor)
                .backgroundDark(R.color.color_dark_material_metaphor)
                .build());

        addSlide(new SimpleSlide.Builder()
                .title(R.string.title_ar_intro2)
                .description(R.string.description_ar_intro2)
                .image(R.drawable.logo)
                .background(R.color.color_material_bold)
                .backgroundDark(R.color.color_dark_material_bold)
                .build());
    }
}
