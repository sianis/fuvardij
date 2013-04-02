
package hu.sianis.fuvardij;

import com.googlecode.androidannotations.annotations.sharedpreferences.DefaultBoolean;
import com.googlecode.androidannotations.annotations.sharedpreferences.SharedPref;

@SharedPref
public interface DataSaver {

    float consumption();

    float distance();

    float fuelprice();

    int passengers();
    
    int start();
    
    int stop();
    
    @DefaultBoolean(false)
    boolean startStopMode();
}
