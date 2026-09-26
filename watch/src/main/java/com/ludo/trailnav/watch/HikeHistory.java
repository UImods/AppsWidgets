package com.ludo.trailnav.watch;import android.content.Context;import java.text.*;import java.util.*;
public final class HikeHistory{private HikeHistory(){}public static String record(Context c,String name,float meters,int steps,long sec,int completion){String e=new SimpleDateFormat("MMM d, yyyy  h:mm a",Locale.US).format(new Date())+"
"+name+"
"+String.format(Locale.US,"%.2f mi • %,d steps • %02d:%02d:%02d • %d%%",meters/1609.344,steps,sec/3600,(sec/60)%60,sec%60,completion);android.content.SharedPreferences p=c.getSharedPreferences("history",0);String old=p.getString("entries","");p.edit().putString("entries",e+(old.isEmpty()?"":"

"+old)).apply();return e;}}
