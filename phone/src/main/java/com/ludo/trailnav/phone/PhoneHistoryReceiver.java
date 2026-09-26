package com.ludo.trailnav.phone;
import com.google.android.gms.wearable.*;import java.util.*;
public final class PhoneHistoryReceiver extends WearableListenerService{@Override public void onDataChanged(DataEventBuffer events){for(DataEvent e:events){if(e.getType()!=DataEvent.TYPE_CHANGED||!e.getDataItem().getUri().getPath().startsWith("/trail/history"))continue;DataMap m=DataMapItem.fromDataItem(e.getDataItem()).getDataMap();String v=m.getString("entry","");if(!v.isEmpty()){android.content.SharedPreferences p=getSharedPreferences("history",0);String old=p.getString("entries","");if(!old.contains(v))p.edit().putString("entries",v+(old.isEmpty()?"":"

"+old)).apply();}}}}
