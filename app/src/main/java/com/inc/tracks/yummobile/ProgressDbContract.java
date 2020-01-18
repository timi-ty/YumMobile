package com.inc.tracks.yummobile;

import android.provider.BaseColumns;

final class ProgressDbContract {
    private ProgressDbContract(){}

    /* Inner classes that defines the tables' contents */
    static class FeedSavedCardEntry implements BaseColumns{
        static final String CARD_TABLE = "saved_card";
        static final String CARD_NUM_COLUMN = "card_num";
        static final String CVV_COLUMN = "cvv_num";
        static final String EXP_MONTH_COLUMN = "expiry_month";
        static final String EXPIRY_YEAR_COLUMN = "expiry_year";
    }
}
