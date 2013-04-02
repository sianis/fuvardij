
package hu.sianis.fuvardij;

import com.googlecode.androidannotations.annotations.AfterTextChange;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Click;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.InstanceState;
import com.googlecode.androidannotations.annotations.ViewById;
import com.googlecode.androidannotations.annotations.sharedpreferences.Pref;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import java.math.BigDecimal;
import java.math.RoundingMode;

@EActivity(R.layout.activity_main)
public class MainActivity extends Activity {

    @ViewById
    EditText consumption;

    @ViewById
    EditText distance;

    @ViewById
    EditText fuelprice;

    @ViewById
    EditText passengers;

    @ViewById
    TextView total;

    @ViewById
    View startStopContainer;

    @ViewById
    EditText start;

    @ViewById
    EditText stop;

    @InstanceState
    String totalValue = null;

    private int errorCount;

    @Pref
    DataSaver_ pref;

    @AfterViews
    void afterView() {
        if (pref.consumption().exists()) {
            consumption.setText(String.valueOf(pref.consumption().get()));
        }
        if (pref.distance().exists()) {
            distance.setText(String.valueOf(pref.distance().get()));
        }
        if (pref.fuelprice().exists()) {
            fuelprice.setText(String.valueOf(pref.fuelprice().get()));
        }
        if (pref.passengers().exists()) {
            passengers.setText(String.valueOf(pref.passengers().get()));
        }
        if (pref.startStopMode().get()) {
            distance.setEnabled(false);
            if (pref.start().exists()) {
                start.setText(String.valueOf(pref.start().get()));
            }
            if (pref.stop().exists()) {
                stop.setText(String.valueOf(pref.stop().get()));
            }
            startStopContainer.setVisibility(View.VISIBLE);
            consumption.setNextFocusDownId(start.getId());
        }
        if (null != totalValue) {
            total.setText(totalValue);
        }

        passengers.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    calculate();
                    return true;
                }
                return false;
            }
        });
    }

    @Click
    void switchMode() {
        // Reset errors
        EditText[] fields = new EditText[] {
                consumption, distance, fuelprice, start, stop
        };

        for (EditText field : fields) {
            field.setError(null);
        }

        pref.startStopMode().put(!pref.startStopMode().get());
        startStopContainer.setVisibility(pref.startStopMode().get() ? View.VISIBLE : View.GONE);
        distance.setEnabled(!pref.startStopMode().get());
        if (pref.startStopMode().get()) {
            consumption.setNextFocusDownId(start.getId());
            if (pref.start().exists() || pref.stop().exists()) {
                if (pref.start().exists()) {
                    start.setText(String.valueOf(pref.start().get()));
                }
                if (pref.stop().exists()) {
                    stop.setText(String.valueOf(pref.stop().get()));
                }
            } else {
                distance.setText("");
            }
        } else {
            if (pref.distance().exists()) {
                distance.setText(String.valueOf(pref.distance().get()));
            }
            consumption.setNextFocusDownId(View.NO_ID);
        }
    }

    @Click
    void calculate() {

        // Hide soft keyboard
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(passengers.getWindowToken(), 0);

        // Validate manadatory fields
        EditText[] mandtoryFields = pref.startStopMode().get() ? new EditText[] {
                consumption, fuelprice, start, stop
        } : new EditText[] {
                consumption, distance, fuelprice
        };

        errorCount = 0;
        for (EditText editText : mandtoryFields) {
            if (editText.getText().toString().trim().length() == 0) {
                editText.setError(getText(R.string.mandatory));
                errorCount++;
            } else {
                editText.setError(null);
            }
        }

        // If no missing data, run calculation
        if (errorCount == 0) {
            float consumptionValue = getFloatFromEditText(consumption);
            float distanceValue = pref.startStopMode().get() ? getDistanceFromStartStop()
                    : getFloatFromEditText(distance);
            float fuelpriceValue = getFloatFromEditText(fuelprice);
            int passengersValue = 1;

            pref.consumption().put(consumptionValue);
            pref.distance().put(distanceValue);
            pref.fuelprice().put(fuelpriceValue);
            if (passengers.length() > 0) {
                passengersValue = getIntFromEditText(passengers);
                pref.passengers().put(passengersValue);
            }

            // Calculate the total value/passanger
            BigDecimal passangerPrice = new BigDecimal(consumptionValue);
            passangerPrice = passangerPrice.movePointLeft(2);
            passangerPrice = passangerPrice.multiply(new BigDecimal(distanceValue));
            passangerPrice = passangerPrice.multiply(new BigDecimal(fuelpriceValue));

            if (passengersValue > 1) {
                passangerPrice = passangerPrice.divide(new BigDecimal(passengersValue), 0,
                        RoundingMode.HALF_UP);
            }
            passangerPrice.setScale(0, RoundingMode.HALF_UP);
            totalValue = getString(R.string.totalPrice, passangerPrice.intValue());
            total.setText(totalValue);
        }
    }

    private float getFloatFromEditText(EditText editText) {
        float value = Float.parseFloat(editText.getText().toString().trim());

        if (value == Float.NaN || value == Float.NEGATIVE_INFINITY
                || value == Float.POSITIVE_INFINITY) {
            value = Float.MAX_VALUE;
        }
        return value;
    }

    private int getIntFromEditText(EditText editText) {
        return Integer.parseInt(editText.getText().toString().trim());
    }

    @AfterTextChange(R.id.consumption)
    void afterConsumptionTextChanged(Editable text, TextView textView) {
        if (text.toString().trim().length() > 0) {
            pref.consumption().put(getFloatFromEditText(consumption));
        } else {
            pref.consumption().remove();
        }
    }

    @AfterTextChange(R.id.distance)
    void afterDistanceTextChanged(Editable text, TextView textView) {
        if (text.toString().trim().length() > 0) {
            pref.distance().put(getFloatFromEditText(distance));
        } else {
            pref.distance().remove();
        }
    }

    @AfterTextChange(R.id.fuelprice)
    void afterFuelpriceTextChanged(Editable text, TextView textView) {
        if (text.toString().trim().length() > 0) {
            pref.fuelprice().put(getFloatFromEditText(fuelprice));
        } else {
            pref.fuelprice().remove();
        }
    }

    @AfterTextChange(R.id.passengers)
    void afterPassengersTextChanged(Editable text, TextView textView) {
        if (text.toString().trim().length() > 0) {
            pref.passengers().put(getIntFromEditText(passengers));
        } else {
            pref.passengers().remove();
        }
    }

    @AfterTextChange(R.id.start)
    void afterStartTextChanged(Editable text, TextView textView) {
        if (text.toString().trim().length() > 0) {
            pref.start().put(getIntFromEditText(start));
        } else {
            pref.start().remove();
        }
    }

    @AfterTextChange(R.id.stop)
    void afterStopTextChanged(Editable text, TextView textView) {
        if (text.toString().trim().length() > 0) {
            pref.stop().put(getIntFromEditText(stop));
        } else {
            pref.stop().remove();
        }
    }

    private float getDistanceFromStartStop() {
        float ret = 999999;
        int start = getIntFromEditText(this.start);
        int stop = getIntFromEditText(this.stop);
        if (stop >= start && stop - start < 999999f) {
            ret = stop - start;
        }
        distance.setText(String.valueOf(ret));
        return ret;
    }

    @Click
    void switcher() {
        start.setText(stop.getText());
        stop.setText(null);
    }
}
