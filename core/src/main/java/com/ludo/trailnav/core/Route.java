package com.ludo.trailnav.core;
import java.util.*;
public final class Route {
 public final String name; public final List<Point> points; public final double meters;
 public Route(String name,List<Point> pts){if(pts==null||pts.size()<2)throw new IllegalArgumentException("Route needs >=2 points");this.name=name==null||name.trim().isEmpty()?"Imported Trail":name.trim();this.points=Collections.unmodifiableList(new ArrayList<>(pts));double d=0;for(int i=1;i<pts.size();i++){double s=RouteMath.distance(pts.get(i-1),pts.get(i));if(s<450)d+=s;}meters=d;}
 public static final class Point { public final double lat,lon,ele; public Point(double lat,double lon){this(lat,lon,Double.NaN);} public Point(double lat,double lon,double ele){if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)throw new IllegalArgumentException("Bad coordinate");this.lat=lat;this.lon=lon;this.ele=ele;} }
}
