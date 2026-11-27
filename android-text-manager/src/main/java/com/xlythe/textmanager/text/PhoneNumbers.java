package com.xlythe.textmanager.text;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Offline lookup using international numbering-plan metadata. Input regions are ISO country codes.
 */
public final class PhoneNumbers {
    private PhoneNumbers() {}

    private static PhoneNumber parse(String address, String region) {
        if (address == null || !address.matches("[+\\p{Nd}() .\\-/]+")) return null;
        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        try {
            PhoneNumber number =
                    util.parse(address, region == null ? "ZZ" : region.toUpperCase(Locale.US));
            return util.isPossibleNumberWithReason(number)
                            == PhoneNumberUtil.ValidationResult.IS_POSSIBLE
                    ? number
                    : null;
        } catch (NumberParseException e) {
            return null;
        }
    }

    /** Returns a lookup key. Short codes, service codes and alphanumeric addresses stay intact. */
    public static String normalize(String address, String region) {
        PhoneNumber number = parse(address, region);
        return number == null
                ? address
                : PhoneNumberUtil.getInstance()
                        .format(number, PhoneNumberUtil.PhoneNumberFormat.E164);
    }

    /** ISO region codes for a calling code. Shared plans may cover several regions. */
    public static List<String> regionsForCallingCode(int callingCode) {
        return Collections.unmodifiableList(
                new ArrayList<>(
                        PhoneNumberUtil.getInstance().getRegionCodesForCountryCode(callingCode)));
    }

    /** Geographic area code without the national trunk prefix, or an empty string. */
    public static String areaCode(String address, String region) {
        PhoneNumber number = parse(address, region);
        if (number == null) return "";
        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        int length = util.getLengthOfGeographicalAreaCode(number);
        return util.getNationalSignificantNumber(number).substring(0, length);
    }

    /**
     * Localized area or country from the numbering plan. An unknown prefix returns an empty string.
     */
    public static String describe(String address, String region, Locale language) {
        PhoneNumber number = parse(address, region);
        return number == null
                ? ""
                : PhoneNumberOfflineGeocoder.getInstance()
                        .getDescriptionForNumber(number, language);
    }
}
