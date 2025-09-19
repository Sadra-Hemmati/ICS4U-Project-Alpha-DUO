package Util;
import com.esotericsoftware.kryo.Kryo;

import Constants.Constants;

public class Helpers {
    public static void registerClasses(Kryo kryo) {
        for(Class<?> cls:Constants.NetworkConstants.registeredClasses){
            kryo.register(cls);
        }
    }
}
