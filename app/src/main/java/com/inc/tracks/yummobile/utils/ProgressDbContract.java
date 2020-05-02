package com.inc.tracks.yummobile.utils;

import android.provider.BaseColumns;

public final class ProgressDbContract {
    private ProgressDbContract(){}

    /* Inner classes that defines the tables' contents */
    public static class FeedSavedCardEntry implements BaseColumns{
        public static final String CARD_TABLE = "saved_card";
        public static final String CARD_NUM_COLUMN = "card_num";
        public static final String CVV_COLUMN = "cvv_num";
        public static final String NAME_COLUMN = "holder_name";
        public static final String EXP_MONTH_COLUMN = "expiry_month";
        public static final String EXPIRY_YEAR_COLUMN = "expiry_year";
    }
}
