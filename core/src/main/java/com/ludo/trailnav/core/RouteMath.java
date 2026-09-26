package com.ludo.trailnav.core;
public final class RouteMath {
 public static final double EARTH_M=6371000.0; private RouteMath(){}
 public static double distance(Route.Point a,Route.Point b){double dl=Math.toRadians(b.lat-a.lat),do_=Math.toRadians(b.lon-a.lon);double h=Math.pow(Math.sin(dl/2),2)+Math.cos(Math.toRadians(a.lat))*Math.cos(Math.toRadians(b.lat))*Math.pow(Math.sin(do_/2),2);return 2*EARTH_M*Math.asin(Math.min(1,Math.sqrt(h)));}
 public static double bearing(Route.Point a,Route.Point b){double p1=Math.toRadians(a.lat),p2=Math.toRadians(b.lat),dl=Math.toRadians(b.lon-a.lon);double y=Math.sin(dl)*Math.cos(p2),x=Math.cos(p1)*Math.sin(p2)-Math.sin(p1)*Math.cos(p2)*Math.cos(dl);double d=Math.toDegrees(Math.atan2(y,x));return (d+360)%360;}
 public static String cardinal(double b){String[] d={"N","NE","E","SE","S","SW","W","NW"};return d[(int)Math.round(((b%360)+360)%360/45.0)%8];}
 public static double east(Route.Point o,Route.Point p){return Math.toRadians(p.lon-o.lon)*EARTH_M*Math.cos(Math.toRadians(o.lat));}
 public static double north(Route.Point o,Route.Point p){return Math.toRadians(p.lat-o.lat)*EARTH_M;}
 public static final class Match{public final double alongMeters,offMeters;public final int segment;public Match(double a,double o,int s){alongMeters=a;offMeters=o;segment=s;}}
 public static Match nearest(Route r,Route.Point l,double prior,double maxAdvance){double acc=0;Match best=null;for(int i=1;i<r.points.size();i++){Route.Point a=r.points.get(i-1),b=r.points.get(i);double len=distance(a,b);if(len>450){continue;}double ax=east(l,a),ay=north(l,a),bx=east(l,b),by=north(l,b),dx=bx-ax,dy=by-ay,den=dx*dx+dy*dy;double t=den<1e-5?0:Math.max(0,Math.min(1,-(ax*dx+ay*dy)/den));double off=Math.hypot(ax+t*dx,ay+t*dy),along=acc+len*t;boolean win=along>=Math.max(0,prior-100)&&along<=Math.min(r.meters,prior+maxAdvance);if(win&&(best==null||off<best.offMeters))best=new Match(along,off,i-1);acc+=len;}return best;}
}
