package com.ludo.trailnav.watch;

import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import com.ludo.trailnav.core.*;
import java.text.SimpleDateFormat;
import java.util.*;

public final class TrailMapView extends View {
    private static final int BLACK = Color.BLACK;
    private static final int GREEN = Color.rgb(100,255,62);
    private static final int GREEN_SOFT = Color.rgb(66,231,112);
    private static final int GRAY = Color.rgb(154,158,160);
    private static final int GRAY_DARK = Color.rgb(58,61,63);
    private static final int WHITE = Color.WHITE;
    private static final int AMBER = Color.rgb(255,180,60);

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetrics fm = new Paint.FontMetrics();
    private final android.content.SharedPreferences prefs;
    private final Route route;
    private final float density;
    private final SimpleDateFormat clock = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private float cx, cy, radius;
    private float leftX, rightX, topControlY, bottomControlY, controlRadius;
    private float arrowY;
    private double zoom = 1.0;
    private boolean active = false, ambient = false, follow = true, northUp = false;
    private long lastCenterTap = 0;
    private Runnable sessionMenu;

    public TrailMapView(Context c, Route r) {
        super(c);
        route = r;
        prefs = c.getSharedPreferences("hike", 0);
        density = getResources().getDisplayMetrics().density;
        setClickable(true);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setActive(boolean a) { active = a; invalidate(); }
    public void setAmbient(boolean a) { ambient = a; invalidate(); }
    public void setSessionMenuAction(Runnable r) { sessionMenu = r; }
    public void followLocation() { follow = true; northUp = false; zoom = 1.0; invalidate(); }

    private float dp(float v) { return v * density; }
    private float sp(float v) { return v * getResources().getDisplayMetrics().scaledDensity; }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        cx = w / 2f; cy = h / 2f; radius = Math.min(w, h) / 2f;
        controlRadius = dp(26);
        leftX = cx - radius + dp(47);
        rightX = cx + radius - dp(47);
        topControlY = cy - dp(42);
        bottomControlY = cy + dp(42);
        arrowY = h * .585f;

        c.save();
        Path clip = new Path(); clip.addCircle(cx, cy, radius, Path.Direction.CW); c.clipPath(clip);
        c.drawColor(BLACK);
        if (route != null) {
            if (ambient) drawAmbient(c);
            else drawHud(c);
        }
        c.restore();
        if (active) postInvalidateDelayed(ambient ? 60000 : 900);
    }

    private void drawHud(Canvas c) {
        drawTopStatus(c);
        drawTrailName(c);
        if (isFullOverview()) drawFullOverview(c, false); else drawPerspectiveRoute(c, false);
        drawControls(c);
        drawMetricPanel(c);
        drawWarnings(c);
    }

    private void drawAmbient(Canvas c) {
        drawFullOverview(c, true);
        text(c, clock.format(new Date()), cx, dp(25), 11, WHITE, true, Paint.Align.CENTER);
        float prog = progress();
        String d = String.format(Locale.US, "%.2f mi", prog / 1609.344);
        text(c, d, cx, getHeight() - dp(22), 9, Color.rgb(205,205,205), true, Paint.Align.CENTER);
    }

    private void drawTopStatus(Canvas c) {
        text(c, clock.format(new Date()), cx - dp(9), dp(26), 11.5f, WHITE, false, Paint.Align.CENTER);
        int level = -1;
        try {
            BatteryManager bm = (BatteryManager)getContext().getSystemService(Context.BATTERY_SERVICE);
            if (bm != null) level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        } catch (Exception ignored) {}
        float bx = cx + dp(35), by = dp(18), bw = dp(19), bh = dp(10);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(1.4f)); p.setColor(GREEN);
        c.drawRoundRect(new RectF(bx, by, bx + bw, by + bh), dp(1.5f), dp(1.5f), p);
        c.drawRect(bx + bw, by + dp(3), bx + bw + dp(2.5f), by + bh - dp(3), p);
        if (level >= 0) {
            p.setStyle(Paint.Style.FILL); p.setColor(GREEN);
            float fill = (bw - dp(4)) * Math.max(0, Math.min(100, level)) / 100f;
            c.drawRect(bx + dp(2), by + dp(2), bx + dp(2) + fill, by + bh - dp(2), p);
        }
    }

    private void drawTrailName(Canvas c) {
        float w = getWidth() * .72f, h = dp(42), x = cx - w/2, y = dp(45);
        LinearGradient g = new LinearGradient(0, y, 0, y+h, Color.rgb(55,58,59), Color.rgb(26,28,29), Shader.TileMode.CLAMP);
        p.setShader(g); p.setStyle(Paint.Style.FILL); c.drawRoundRect(new RectF(x,y,x+w,y+h), h/2,h/2,p); p.setShader(null);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(.9f)); p.setColor(Color.rgb(106,109,110)); c.drawRoundRect(new RectF(x,y,x+w,y+h),h/2,h/2,p);
        drawHiker(c, x + dp(24), y + h/2, dp(11));
        String n = route.name == null ? "Imported Trail" : route.name;
        p.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD)); p.setTextSize(sp(11.4f));
        float max = w - dp(64);
        while (p.measureText(n) > max && n.length() > 6) n = n.substring(0,n.length()-2) + "…";
        p.setColor(WHITE); p.setStyle(Paint.Style.FILL); p.setTextAlign(Paint.Align.LEFT); p.getFontMetrics(fm);
        c.drawText(n, x + dp(42), y + h/2 - (fm.ascent+fm.descent)/2, p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(2)); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND); p.setColor(Color.rgb(150,153,154));
        Path chev = new Path(); float qx=x+w-dp(18), qy=y+h/2; chev.moveTo(qx-dp(3),qy-dp(5));chev.lineTo(qx+dp(2),qy);chev.lineTo(qx-dp(3),qy+dp(5));c.drawPath(chev,p); p.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawHiker(Canvas c,float x,float y,float s){
        p.setColor(GREEN);p.setStyle(Paint.Style.FILL);c.drawCircle(x,y-s*.75f,s*.23f,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(dp(1.8f),s*.20f));p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
        c.drawLine(x,y-s*.48f,x-s*.12f,y+s*.10f,p);c.drawLine(x-s*.12f,y+s*.10f,x-s*.45f,y+s*.70f,p);c.drawLine(x-s*.12f,y+s*.10f,x+s*.30f,y+s*.64f,p);c.drawLine(x-s*.06f,y-s*.26f,x+s*.42f,y-s*.04f,p);c.drawLine(x+s*.50f,y-s*.18f,x+s*.50f,y+s*.72f,p);p.setStrokeCap(Paint.Cap.BUTT);
    }

    private float progress() { return Math.min((float)route.meters, Math.max(0, prefs.getFloat("progress",0))); }
    private boolean gpsValid() { long t=prefs.getLong("lastGpsElapsed",0); return active && prefs.getBoolean("gps_valid",false) && t>0 && SystemClock.elapsedRealtime()-t<12000; }
    private boolean isFullOverview() { return zoom <= .50; }

    private void drawPerspectiveRoute(Canvas c, boolean lowPower) {
        float prog = progress();
        double heading = northUp ? 0 : headingAt(prog, 22);
        double ahead = Math.min(Math.max(80, 205 / Math.max(.68, zoom)), Math.max(80, route.meters));
        double behind = Math.min(46 / Math.max(.75, zoom), Math.max(20, prog));
        double activeAhead = Math.min(10, Math.max(0, route.meters-prog));
        Route.Point origin = pointAt(prog);
        if (origin == null) return;

        double start = Math.max(0, prog - behind);
        double greenEnd = Math.min(route.meters, prog + activeAhead);
        double end = Math.min(route.meters, prog + ahead);
        drawProjectedRange(c, origin, heading, greenEnd, end, GRAY, lowPower, false);
        drawProjectedRange(c, origin, heading, start, greenEnd, GREEN, lowPower, true);
        drawArrow(c, cx, arrowY, lowPower);
    }

    private void drawProjectedRange(Canvas c, Route.Point origin, double heading, double start, double end, int color, boolean lowPower, boolean glow) {
        if (end <= start) return;
        double step = Math.max(3.5, (end-start)/42.0);
        Route.Point prev = pointAt(start);
        PointF a = project(prev, origin, heading);
        for (double d=start+step; d<=end+0.001; d+=step) {
            Route.Point now = pointAt(Math.min(end,d));
            PointF b = project(now, origin, heading);
            double mid=(Math.min(end,d)+Math.max(start,d-step))/2.0;
            double forward=Math.max(0, mid-progress());
            float near=(float)(1.0/(1.0+forward/180.0));
            float width=dp((lowPower?3.2f:5.0f)+(lowPower?3.2f:8.6f)*near);
            if (glow && !lowPower) {
                p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(width+dp(8));p.setColor(Color.argb(48,70,255,55));c.drawLine(a.x,a.y,b.x,b.y,p);
            }
            p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(width+dp(lowPower?1.0f:2.0f));p.setColor(color==GREEN?Color.rgb(31,113,28):GRAY_DARK);c.drawLine(a.x,a.y,b.x,b.y,p);
            p.setStrokeWidth(width);p.setColor(color);c.drawLine(a.x,a.y,b.x,b.y,p);
            if (!lowPower) {
                p.setStrokeWidth(Math.max(dp(.8f),width*.18f));p.setColor(color==GREEN?Color.rgb(180,255,145):Color.rgb(210,212,213));c.drawLine(a.x,a.y-dp(.5f),b.x,b.y-dp(.5f),p);
            }
            a=b; prev=now;
            if (d>=end) break;
        }
        p.setStrokeCap(Paint.Cap.BUTT);
    }

    private PointF project(Route.Point q, Route.Point origin, double heading) {
        double lat0=Math.toRadians(origin.lat);
        double east=Math.toRadians(q.lon-origin.lon)*RouteMath.EARTH_M*Math.cos(lat0);
        double north=Math.toRadians(q.lat-origin.lat)*RouteMath.EARTH_M;
        double r=Math.toRadians(heading);
        double forward=east*Math.sin(r)+north*Math.cos(r);
        double lateral=east*Math.cos(r)-north*Math.sin(r);
        double depth=1.0+Math.max(0,forward)/210.0;
        double ppm=1.68*Math.max(.70,Math.min(2.0,zoom));
        float x=(float)(cx+lateral*ppm/depth);
        float y=(float)(arrowY-forward*ppm/depth);
        return new PointF(x,y);
    }

    private void drawArrow(Canvas c,float x,float y,boolean lowPower){
        float s=dp(lowPower?13:17);
        Path a=new Path();a.moveTo(x,y-s);a.lineTo(x-s*.82f,y+s*.72f);a.lineTo(x,y+s*.40f);a.lineTo(x+s*.82f,y+s*.72f);a.close();
        if(!lowPower){p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(70,80,255,55));c.drawPath(a,p);p.setShadowLayer(dp(8),0,0,Color.argb(150,80,255,60));c.drawPath(a,p);p.clearShadowLayer();}
        p.setStyle(Paint.Style.FILL);p.setColor(lowPower?Color.BLACK:Color.rgb(37,116,34));c.drawPath(a,p);p.setStyle(Paint.Style.STROKE);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeWidth(dp(lowPower?1.5f:2.4f));p.setColor(WHITE);c.drawPath(a,p);p.setStrokeJoin(Paint.Join.MITER);
    }

    private void drawFullOverview(Canvas c, boolean lowPower) {
        if (route.points.size()<2) return;
        double minE=1e99,maxE=-1e99,minN=1e99,maxN=-1e99;
        Route.Point o=route.points.get(0);
        for(Route.Point q:route.points){double e=east(q,o),n=north(q,o);minE=Math.min(minE,e);maxE=Math.max(maxE,e);minN=Math.min(minN,n);maxN=Math.max(maxN,n);}
        float top=lowPower?dp(55):dp(102), bottom=lowPower?getHeight()-dp(54):getHeight()*.69f;
        float availW=getWidth()-(lowPower?dp(70):dp(120)), availH=bottom-top;
        double sc=Math.min(availW/Math.max(1,maxE-minE),availH/Math.max(1,maxN-minN));
        double ce=(minE+maxE)/2,cn=(minN+maxN)/2;
        Path whole=new Path();boolean first=true;
        for(int i=0;i<route.points.size();i++){Route.Point q=route.points.get(i);if(i>0&&RouteMath.distance(route.points.get(i-1),q)>450){first=true;continue;}float x=(float)(cx+(east(q,o)-ce)*sc),y=(float)((top+bottom)/2-(north(q,o)-cn)*sc);if(first){whole.moveTo(x,y);first=false;}else whole.lineTo(x,y);}
        p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setColor(GRAY);p.setStrokeWidth(dp(lowPower?2.8f:4.4f));c.drawPath(whole,p);
        drawOverviewProgress(c,o,ce,cn,sc,top,bottom,lowPower);
        Route.Point here=pointAt(progress());float hx=(float)(cx+(east(here,o)-ce)*sc),hy=(float)((top+bottom)/2-(north(here,o)-cn)*sc);drawArrow(c,hx,hy,lowPower);
        p.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawOverviewProgress(Canvas c,Route.Point o,double ce,double cn,double sc,float top,float bottom,boolean lowPower){
        float prog=progress();boolean rev=prefs.getBoolean("reverse",false);Path path=new Path();boolean started=false;double walked=0;
        if(!rev){for(int i=0;i<route.points.size()-1;i++){Route.Point a=route.points.get(i),b=route.points.get(i+1);double len=RouteMath.distance(a,b);if(len>450)continue;if(walked>=prog)break;double f=Math.min(1,(prog-walked)/Math.max(1e-6,len));Route.Point z=interpolate(a,b,f);float ax=(float)(cx+(east(a,o)-ce)*sc),ay=(float)((top+bottom)/2-(north(a,o)-cn)*sc),zx=(float)(cx+(east(z,o)-ce)*sc),zy=(float)((top+bottom)/2-(north(z,o)-cn)*sc);if(!started){path.moveTo(ax,ay);started=true;}path.lineTo(zx,zy);walked+=len;if(f<1)break;}}
        else{for(int i=route.points.size()-1;i>0;i--){Route.Point a=route.points.get(i),b=route.points.get(i-1);double len=RouteMath.distance(a,b);if(len>450)continue;if(walked>=prog)break;double f=Math.min(1,(prog-walked)/Math.max(1e-6,len));Route.Point z=interpolate(a,b,f);float ax=(float)(cx+(east(a,o)-ce)*sc),ay=(float)((top+bottom)/2-(north(a,o)-cn)*sc),zx=(float)(cx+(east(z,o)-ce)*sc),zy=(float)((top+bottom)/2-(north(z,o)-cn)*sc);if(!started){path.moveTo(ax,ay);started=true;}path.lineTo(zx,zy);walked+=len;if(f<1)break;}}
        p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setColor(GREEN);p.setStrokeWidth(dp(lowPower?3.2f:5.4f));c.drawPath(path,p);
    }

    private double east(Route.Point q,Route.Point o){return Math.toRadians(q.lon-o.lon)*RouteMath.EARTH_M*Math.cos(Math.toRadians(o.lat));}
    private double north(Route.Point q,Route.Point o){return Math.toRadians(q.lat-o.lat)*RouteMath.EARTH_M;}

    private void drawControls(Canvas c) {
        control(c,leftX,topControlY,"+");control(c,leftX,bottomControlY,"−");
        compassControl(c,rightX,topControlY);recenterControl(c,rightX,bottomControlY);
    }

    private void control(Canvas c,float x,float y,String label){
        buttonBase(c,x,y);text(c,label,x,y+(label.equals("+")?dp(1):0),25,WHITE,false,Paint.Align.CENTER);
    }
    private void buttonBase(Canvas c,float x,float y){
        RadialGradient g=new RadialGradient(x-dp(6),y-dp(7),controlRadius*1.25f,Color.rgb(72,75,77),Color.rgb(20,22,23),Shader.TileMode.CLAMP);p.setShader(g);p.setStyle(Paint.Style.FILL);c.drawCircle(x,y,controlRadius,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(.9f));p.setColor(Color.rgb(115,118,119));c.drawCircle(x,y,controlRadius,p);
    }
    private void compassControl(Canvas c,float x,float y){buttonBase(c,x,y);Path tri=new Path();tri.moveTo(x,y-dp(18));tri.lineTo(x-dp(6),y-dp(7));tri.lineTo(x+dp(6),y-dp(7));tri.close();p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(255,73,24));c.drawPath(tri,p);text(c,"N",x,y+dp(8),17,WHITE,true,Paint.Align.CENTER);}
    private void recenterControl(Canvas c,float x,float y){buttonBase(c,x,y);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2.2f));p.setColor(WHITE);c.drawCircle(x,y,dp(10),p);c.drawCircle(x,y,dp(3),p);c.drawLine(x-dp(15),y,x-dp(10),y,p);c.drawLine(x+dp(10),y,x+dp(15),y,p);c.drawLine(x,y-dp(15),x,y-dp(10),p);c.drawLine(x,y+dp(10),x,y+dp(15),p);}

    private void drawMetricPanel(Canvas c) {
        float l=dp(20),r=getWidth()-dp(20),t=getHeight()*.725f,b=getHeight()-dp(17);
        LinearGradient g=new LinearGradient(0,t,0,b,Color.argb(245,39,45,46),Color.argb(245,15,20,21),Shader.TileMode.CLAMP);p.setShader(g);p.setStyle(Paint.Style.FILL);Path panel=new Path();float rad=dp(30);panel.moveTo(l+rad,t);panel.lineTo(r-rad,t);panel.quadTo(r,t,r,b-rad*.55f);panel.quadTo(r,b,l+rad*.42f,b);panel.quadTo(l,b,l,t+rad);panel.quadTo(l,t,l+rad,t);panel.close();c.drawPath(panel,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(.8f));p.setColor(Color.rgb(102,106,107));c.drawPath(panel,p);

        float colW=(r-l)/4f;for(int i=1;i<4;i++){float x=l+colW*i;p.setColor(Color.rgb(78,83,84));p.setStrokeWidth(dp(.6f));c.drawLine(x,t+dp(12),x,b-dp(13),p);}
        long st=prefs.getLong("startElapsed",0);long sec=active&&st>0?Math.max(0,(SystemClock.elapsedRealtime()-st)/1000):0;
        String time=String.format(Locale.US,"%d:%02d:%02d",sec/3600,(sec/60)%60,sec%60);
        String dist=String.format(Locale.US,"%.1f mi",progress()/1609.344);
        String steps=String.format(Locale.US,"%,d",Math.max(0,prefs.getInt("steps",0)));
        Route.Point at=pointAt(progress());String elev=Double.isFinite(at.ele)?String.format(Locale.US,"%,.0f ft",at.ele*3.28084):"— ft";
        float iconY=t+dp(18),numY=t+dp(42),labY=t+dp(60);
        String[] nums={time,dist,steps,elev}, labs={"Time","Distance","Steps","Elevation"};
        for(int i=0;i<4;i++){float x=l+colW*(i+.5f);drawMetricIcon(c,i,x,iconY);float sz=i==0?11.5f:13.2f;text(c,nums[i],x,numY,sz,WHITE,true,Paint.Align.CENTER);text(c,labs[i],x,labY,8.4f,Color.rgb(200,201,202),false,Paint.Align.CENTER);}
    }

    private void drawMetricIcon(Canvas c,int i,float x,float y){p.setColor(WHITE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1.7f));p.setStrokeCap(Paint.Cap.ROUND);if(i==0){c.drawCircle(x,y,dp(6.5f),p);c.drawLine(x,y,x,y-dp(4),p);c.drawLine(x,y,x+dp(3),y,p);c.drawLine(x-dp(3),y-dp(9),x+dp(3),y-dp(9),p);c.drawLine(x,y-dp(9),x,y-dp(7),p);}else if(i==1){Path q=new Path();q.moveTo(x,y+dp(8));q.cubicTo(x-dp(11),y-dp(1),x-dp(6),y-dp(10),x,y-dp(10));q.cubicTo(x+dp(6),y-dp(10),x+dp(11),y-dp(1),x,y+dp(8));c.drawPath(q,p);p.setStyle(Paint.Style.FILL);c.drawCircle(x,y-dp(3),dp(2.5f),p);}else if(i==2){p.setStyle(Paint.Style.FILL);c.drawOval(new RectF(x-dp(7),y-dp(8),x-dp(1),y+dp(2)),p);c.drawOval(new RectF(x+dp(1),y-dp(11),x+dp(7),y-dp(1)),p);c.drawCircle(x-dp(4),y+dp(5),dp(2),p);c.drawCircle(x+dp(4),y+dp(2),dp(2),p);}else{p.setStyle(Paint.Style.FILL);Path m=new Path();m.moveTo(x-dp(10),y+dp(7));m.lineTo(x-dp(2),y-dp(8));m.lineTo(x+dp(3),y);m.lineTo(x+dp(7),y-dp(5));m.lineTo(x+dp(12),y+dp(7));m.close();c.drawPath(m,p);}p.setStrokeCap(Paint.Cap.BUTT);}

    private void drawWarnings(Canvas c){
        if(!active)return;String s=null;int col=AMBER;if(!gpsValid())s=prefs.getBoolean("gps_weak",false)?"GPS WEAK":"WAITING FOR GPS";else if(prefs.getBoolean("off_trail",false))s="OFF TRAIL";else if(prefs.getBoolean("return_mode",false)){Route.Point cur=new Route.Point(prefs.getFloat("lat",0),prefs.getFloat("lon",0));Route.Point start=prefs.getBoolean("reverse",false)?route.points.get(route.points.size()-1):route.points.get(0);double m=RouteMath.distance(cur,start),b=RouteMath.bearing(cur,start);s=String.format(Locale.US,"RETURN • %s • %s",m<160.934?String.format(Locale.US,"%.0f FT",m*3.28084):String.format(Locale.US,"%.2f MI",m/1609.344),RouteMath.cardinal(b));col=GREEN_SOFT;}if(s!=null)text(c,s,cx,getHeight()*.685f,8.1f,col,true,Paint.Align.CENTER);
    }

    private void text(Canvas c,String s,float x,float centerY,float sizeSp,int color,boolean bold,Paint.Align align){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));p.setTextSize(sp(sizeSp));p.setTextAlign(align);p.getFontMetrics(fm);c.drawText(s,x,centerY-(fm.ascent+fm.descent)/2,p);}

    private double headingAt(double prog,double look){Route.Point a=pointAt(prog),b=pointAt(Math.min(route.meters,prog+Math.max(7,look)));return a==null||b==null?0:RouteMath.bearing(a,b);}
    private Route.Point pointAt(double prog){boolean rev=prefs.getBoolean("reverse",false);prog=Math.max(0,Math.min(route.meters,prog));double walk=0;if(!rev){for(int i=0;i<route.points.size()-1;i++){Route.Point a=route.points.get(i),b=route.points.get(i+1);double l=RouteMath.distance(a,b);if(l>450)continue;if(walk+l>=prog){double f=l<=0?0:(prog-walk)/l;return interpolate(a,b,f);}walk+=l;}return route.points.get(route.points.size()-1);}for(int i=route.points.size()-1;i>0;i--){Route.Point a=route.points.get(i),b=route.points.get(i-1);double l=RouteMath.distance(a,b);if(l>450)continue;if(walk+l>=prog){double f=l<=0?0:(prog-walk)/l;return interpolate(a,b,f);}walk+=l;}return route.points.get(0);}
    private Route.Point interpolate(Route.Point a,Route.Point b,double f){double e;if(Double.isFinite(a.ele)&&Double.isFinite(b.ele))e=a.ele+(b.ele-a.ele)*f;else e=Double.isFinite(a.ele)?a.ele:b.ele;return new Route.Point(a.lat+(b.lat-a.lat)*f,a.lon+(b.lon-a.lon)*f,e);}

    @Override public boolean onTouchEvent(MotionEvent e){
        if(route==null||!active||ambient)return true;
        if(e.getAction()!=MotionEvent.ACTION_UP)return true;
        float x=e.getX(),y=e.getY();
        if(hit(x,y,leftX,topControlY)){zoom=Math.min(2.4,zoom*1.42);follow=true;invalidate();return true;}
        if(hit(x,y,leftX,bottomControlY)){zoom=Math.max(.38,zoom/1.42);follow=true;invalidate();return true;}
        if(hit(x,y,rightX,topControlY)){northUp=!northUp;invalidate();return true;}
        if(hit(x,y,rightX,bottomControlY)){followLocation();return true;}
        if(Math.hypot(x-cx,y-arrowY)<=dp(75)){long now=SystemClock.elapsedRealtime();if(lastCenterTap>0&&now-lastCenterTap<=380){lastCenterTap=0;if(sessionMenu!=null)sessionMenu.run();}else lastCenterTap=now;return true;}
        return true;
    }
    private boolean hit(float x,float y,float bx,float by){return Math.hypot(x-bx,y-by)<=controlRadius+dp(9);}
    @Override public boolean performClick(){super.performClick();return true;}
}
