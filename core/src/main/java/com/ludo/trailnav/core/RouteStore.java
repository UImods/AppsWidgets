package com.ludo.trailnav.core;
import android.content.Context;import java.io.*;
public final class RouteStore{private RouteStore(){}private static File file(Context c){return new File(c.getFilesDir(),"selected_route.gpx");}
 public static byte[] readAll(InputStream in)throws Exception{ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){o.write(b,0,n);if(o.size()>3000000)throw new IllegalArgumentException("GPX exceeds 3MB");}return o.toByteArray();}
 public static Route save(Context c,byte[] d)throws Exception{Route r=GpxParser.parse(new ByteArrayInputStream(d));try(FileOutputStream o=new FileOutputStream(file(c))){o.write(d);o.getFD().sync();}c.getSharedPreferences("route",0).edit().putLong("revision",System.currentTimeMillis()).apply();return r;}
 public static Route load(Context c)throws Exception{try(FileInputStream i=new FileInputStream(file(c))){return GpxParser.parse(i);}}
 public static byte[] bytes(Context c)throws Exception{try(FileInputStream i=new FileInputStream(file(c))){return readAll(i);}}
 public static boolean exists(Context c){return file(c).isFile();}public static void clear(Context c){File f=file(c);if(f.exists())f.delete();c.getSharedPreferences("route",0).edit().putLong("revision",System.currentTimeMillis()).apply();}
 public static Route seedSample(Context c)throws Exception{if(exists(c))return load(c);try(InputStream i=c.getAssets().open("chapel_branch.gpx")){return save(c,readAll(i));}}
}
