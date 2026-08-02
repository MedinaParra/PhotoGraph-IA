package cl.medina.atatis;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {
    private GameView game;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        immersive();
        game = new GameView(this);
        setContentView(game);
    }

    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        if (focused) immersive();
    }

    @Override protected void onPause() {
        super.onPause();
        if (game != null) game.save();
    }

    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    static final class GameView extends View {
        private static final String[] ROOMS = {"Dormitorio", "Cocina", "Baño", "Jardín", "Vestidor"};
        private static final String[] NAV = {"DORMIR", "COMER", "BAÑO", "JUGAR", "ESTILO"};
        private static final String[] STYLES = {"Arcoíris", "Princesa", "Exploradora", "Pijama", "Fiesta"};
        private static final String[] COLORS = {"Canela", "Rosita", "Lavanda", "Menta", "Celeste"};
        private static final int[] FUR = {0xFFA97855, 0xFFE6A7B9, 0xFFB6A0D8, 0xFF8FC8B5, 0xFF91B8D8};
        private static final int[] MAGIC = {0xFFFF70A6, 0xFFFFD166, 0xFF67D5FF, 0xFF7EE2A8, 0xFFB28DFF};

        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF[] navRects = new RectF[5];
        private final RectF leftAction = new RectF();
        private final RectF rightAction = new RectF();
        private final SharedPreferences pref;
        private final Random rnd = new Random();
        private final List<Particle> particles = new ArrayList<>();

        private int room, style, fur, stars;
        private float happy, food, energy, clean, t;
        private boolean sleeping;
        private long sleepUntil, lastFrame = SystemClock.elapsedRealtime(), messageUntil;
        private String message = "¡Hola! Soy A-tatis ✨";

        GameView(Context context) {
            super(context);
            setKeepScreenOn(true);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            setContentDescription("A-tatis, mascota virtual capibara unicornio");
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeCap(Paint.Cap.ROUND);
            pref = context.getSharedPreferences("atatis_save", Context.MODE_PRIVATE);
            load();
        }

        private void load() {
            room = limit(pref.getInt("room", 0), 0, 4);
            style = limit(pref.getInt("style", 0), 0, 4);
            fur = limit(pref.getInt("fur", 0), 0, 4);
            stars = Math.max(0, pref.getInt("stars", 0));
            happy = cap(pref.getFloat("happy", 88));
            food = cap(pref.getFloat("food", 82));
            energy = cap(pref.getFloat("energy", 80));
            clean = cap(pref.getFloat("clean", 86));
            float hours = Math.min(24f, Math.max(0, System.currentTimeMillis() -
                    pref.getLong("saved", System.currentTimeMillis())) / 3_600_000f);
            happy = cap(happy - hours); food = cap(food - hours * 1.5f);
            energy = cap(energy - hours * 1.2f); clean = cap(clean - hours * .8f);
        }

        void save() {
            pref.edit().putInt("room", room).putInt("style", style).putInt("fur", fur)
                    .putInt("stars", stars).putFloat("happy", happy).putFloat("food", food)
                    .putFloat("energy", energy).putFloat("clean", clean)
                    .putLong("saved", System.currentTimeMillis()).apply();
        }

        @Override protected void onDraw(Canvas c) {
            long now = SystemClock.elapsedRealtime();
            float dt = Math.min(.05f, (now - lastFrame) / 1000f);
            lastFrame = now; t += dt;
            happy = cap(happy - dt / 130f); food = cap(food - dt / 95f);
            energy = cap(energy - dt / 110f); clean = cap(clean - dt / 180f);
            if (sleeping) {
                energy = cap(energy + dt * 8f);
                if (now > sleepUntil || energy > 99) {
                    sleeping = false; say("¡A-tatis despertó con mucha energía!"); burst(getWidth()/2f, getHeight()*.48f, 18);
                }
            }
            updateParticles(dt);
            float w = getWidth(), h = getHeight();
            drawRoom(c, w, h);
            drawHeader(c, w, h);
            drawCapybara(c, w*.5f, h*.51f, Math.min(w, h)*.24f);
            drawActions(c, w, h);
            drawNavigation(c, w, h);
            drawParticles(c);
            if (messageUntil == 0 || now < messageUntil) drawMessage(c, w, h);
            postInvalidateOnAnimation();
        }

        private void drawRoom(Canvas c, float w, float h) {
            int top, bottom;
            switch (room) {
                case 1: top=0xFFFFF4B8; bottom=0xFFFFC6A8; break;
                case 2: top=0xFFBDEFF2; bottom=0xFF91D4C6; break;
                case 3: top=0xFF9DE4FF; bottom=0xFFB8EEA7; break;
                case 4: top=0xFFEAD7FF; bottom=0xFFFFD5EA; break;
                default: top=0xFFFFE4F2; bottom=0xFFCDB7FF;
            }
            p.setShader(new LinearGradient(0,0,0,h,top,bottom, Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,p); p.setShader(null);
            if (room==0) bedroom(c,w,h); else if(room==1) kitchen(c,w,h);
            else if(room==2) bathroom(c,w,h); else if(room==3) garden(c,w,h); else closet(c,w,h);
        }

        private void bedroom(Canvas c,float w,float h){
            p.setColor(0xFF6B52A3); round(c,w*.07f,h*.19f,w*.34f,h*.41f,24,p);
            p.setColor(0xFFFFE37A); for(int i=0;i<6;i++) star(c,w*(.11f+i*.035f),h*(.23f+(i%2)*.06f),5,p);
            p.setColor(0xFFF9A8D4); round(c,w*.65f,h*.57f,w,h*.72f,30,p);
            p.setColor(Color.WHITE); round(c,w*.67f,h*.56f,w*.79f,h*.63f,20,p);
        }
        private void kitchen(Canvas c,float w,float h){
            p.setColor(0xFFFFFAEA); round(c,w*.05f,h*.19f,w*.29f,h*.61f,22,p);
            p.setColor(0xFFB6E1E8); round(c,w*.075f,h*.22f,w*.265f,h*.39f,16,p);
            p.setColor(0xFFF5F5F5); c.drawRect(0,h*.62f,w,h*.69f,p);
            p.setColor(0xFFFF7043); c.drawCircle(w*.80f,h*.27f,w*.055f,p);
            p.setColor(0xFF4CAF50); c.drawOval(new RectF(w*.79f,h*.19f,w*.87f,h*.25f),p);
        }
        private void bathroom(Canvas c,float w,float h){
            line.setColor(0x55FFFFFF); line.setStrokeWidth(3); float tile=w/7f;
            for(float x=0;x<w;x+=tile)c.drawLine(x,0,x,h*.74f,line);
            for(float y=0;y<h*.74f;y+=tile)c.drawLine(0,y,w,y,line);
            p.setColor(Color.WHITE); round(c,w*.60f,h*.54f,w,h*.72f,50,p);
            p.setColor(0xFF7CD6E8); round(c,w*.63f,h*.58f,w*.97f,h*.68f,40,p);
            p.setColor(0xAAFFFFFF); for(int i=0;i<8;i++)c.drawCircle(w*(.66f+(i%4)*.075f),h*(.52f-(i/4)*.055f),9+(i%3)*4,p);
        }
        private void garden(Canvas c,float w,float h){
            p.setColor(0xFFFFE37A); c.drawCircle(w*.84f,h*.18f,w*.07f,p);
            cloud(c,w*.18f,h*.24f,w*.11f); cloud(c,w*.68f,h*.31f,w*.09f);
            p.setColor(0xFF72C769); c.drawOval(new RectF(-w*.2f,h*.58f,w*.75f,h),p);
            p.setColor(0xFF5FB45B); c.drawOval(new RectF(w*.25f,h*.55f,w*1.2f,h),p);
            flower(c,w*.13f,h*.66f,15,0xFFFF70A6); flower(c,w*.85f,h*.64f,17,0xFFB28DFF);
        }
        private void closet(Canvas c,float w,float h){
            p.setColor(0xFF8D6AB8); round(c,w*.03f,h*.19f,w*.28f,h*.65f,22,p);
            p.setColor(0xFFC6A7E2); round(c,w*.055f,h*.22f,w*.255f,h*.62f,16,p);
            line.setColor(0xFF745395); line.setStrokeWidth(6); c.drawLine(w*.155f,h*.22f,w*.155f,h*.62f,line);
            p.setColor(Color.WHITE); c.drawOval(new RectF(w*.73f,h*.22f,w*.98f,h*.59f),p);
            line.setColor(0xFFFFA7CF); line.setStrokeWidth(13); c.drawOval(new RectF(w*.73f,h*.22f,w*.98f,h*.59f),line);
            hanger(c,w*.45f,h*.32f,w*.13f,0xFFFF70A6); hanger(c,w*.58f,h*.29f,w*.12f,0xFF67D5FF);
        }

        private void drawHeader(Canvas c,float w,float h){
            p.setColor(0xEFFFFFFF); round(c,w*.025f,h*.015f,w*.975f,h*.17f,28,p);
            p.setColor(0xFF5D3A77); p.setTextAlign(Paint.Align.LEFT); p.setFakeBoldText(true); p.setTextSize(w*.055f);
            c.drawText("A-tatis",w*.055f,h*.066f,p); p.setFakeBoldText(false); p.setTextSize(w*.027f);
            c.drawText(ROOMS[room]+" · "+STYLES[style],w*.055f,h*.097f,p);
            p.setTextAlign(Paint.Align.RIGHT); p.setFakeBoldText(true); p.setTextSize(w*.036f); c.drawText("★ "+stars,w*.94f,h*.067f,p); p.setFakeBoldText(false);
            float y=h*.115f,gap=w*.016f,bw=(w*.89f-gap*3)/4f;
            stat(c,w*.055f,y,bw,h*.038f,happy,"ALEGRÍA",0xFFFF6FAE);
            stat(c,w*.055f+bw+gap,y,bw,h*.038f,food,"COMIDA",0xFFFFA84D);
            stat(c,w*.055f+(bw+gap)*2,y,bw,h*.038f,energy,"ENERGÍA",0xFF7C8CFF);
            stat(c,w*.055f+(bw+gap)*3,y,bw,h*.038f,clean,"LIMPIEZA",0xFF45C9C0);
        }
        private void stat(Canvas c,float x,float y,float w,float h,float v,String s,int color){
            p.setTextAlign(Paint.Align.LEFT); p.setFakeBoldText(true); p.setTextSize(getWidth()*.018f); p.setColor(0xFF6A5475); c.drawText(s,x,y,p); p.setFakeBoldText(false);
            float yy=y+h*.22f,bh=h*.42f; p.setColor(0xFFE5DDE9); round(c,x,yy,x+w,yy+bh,bh/2,p); p.setColor(color); round(c,x,yy,x+w*v/100f,yy+bh,bh/2,p);
        }

        private void drawCapybara(Canvas c,float cx,float cy,float s){
            cy += (float)Math.sin(t*(sleeping?1.2:2.2))*s*.015f;
            int base=FUR[fur], light=blend(base,Color.WHITE,.28f), dark=blend(base,Color.BLACK,.22f);
            p.setColor(0x33000000); c.drawOval(new RectF(cx-s*.72f,cy+s*.42f,cx+s*.78f,cy+s*.62f),p);
            p.setColor(base); c.drawOval(new RectF(cx-s*.72f,cy-s*.12f,cx+s*.72f,cy+s*.58f),p);
            p.setColor(dark); round(c,cx-s*.50f,cy+s*.32f,cx-s*.25f,cy+s*.69f,s*.09f,p); round(c,cx+s*.20f,cy+s*.32f,cx+s*.45f,cy+s*.69f,s*.09f,p);
            p.setColor(base); c.drawCircle(cx-s*.30f,cy-s*.25f,s*.43f,p);
            p.setColor(dark); c.drawCircle(cx-s*.55f,cy-s*.53f,s*.15f,p); c.drawCircle(cx-s*.08f,cy-s*.56f,s*.14f,p);
            p.setColor(light); c.drawCircle(cx-s*.55f,cy-s*.53f,s*.08f,p); c.drawCircle(cx-s*.08f,cy-s*.56f,s*.075f,p);
            p.setColor(light); c.drawOval(new RectF(cx-s*.55f,cy-s*.28f,cx+s*.02f,cy+s*.12f),p);
            horn(c,cx-s*.28f,cy-s*.67f,s*.33f); mane(c,cx+s*.02f,cy-s*.43f,s);
            if(sleeping){
                line.setColor(0xFF342934); line.setStrokeWidth(s*.035f); c.drawArc(new RectF(cx-s*.47f,cy-s*.31f,cx-s*.28f,cy-s*.17f),15,150,false,line);
                p.setColor(0xFF6B4E8A); p.setTextAlign(Paint.Align.LEFT); p.setTextSize(s*.22f); c.drawText("Z",cx+s*.18f,cy-s*.50f,p);
            }else{
                p.setColor(0xFF2D2233); c.drawCircle(cx-s*.39f,cy-s*.25f,s*.047f,p); p.setColor(Color.WHITE); c.drawCircle(cx-s*.405f,cy-s*.267f,s*.014f,p);
            }
            p.setColor(0xFFFF8FAD); c.drawCircle(cx-s*.50f,cy-s*.05f,s*.07f,p);
            p.setColor(0xFF4C342E); c.drawOval(new RectF(cx-s*.35f,cy-s*.12f,cx-s*.21f,cy-s*.02f),p);
            line.setColor(0xFF4C342E); line.setStrokeWidth(s*.018f); c.drawArc(new RectF(cx-s*.31f,cy-s*.04f,cx-s*.12f,cy+s*.10f),15,115,false,line);
            accessory(c,cx,cy,s);
        }
        private void horn(Canvas c,float x,float y,float s){
            Path q=new Path(); q.moveTo(x,y-s); q.lineTo(x-s*.22f,y+s*.12f); q.lineTo(x+s*.22f,y+s*.12f); q.close(); p.setColor(0xFFFFF7D6); c.drawPath(q,p);
            line.setStrokeWidth(Math.max(3,s*.04f)); for(int i=0;i<4;i++){line.setColor(MAGIC[i]);float yy=y-s*(.72f-i*.22f);c.drawLine(x-s*(.07f+i*.025f),yy,x+s*(.07f+i*.025f),yy+s*.05f,line);}
        }
        private void mane(Canvas c,float x,float y,float s){for(int i=0;i<5;i++){p.setColor(MAGIC[i]);c.drawCircle(x+s*.04f*i,y+s*.13f*i,s*(.105f-i*.007f),p);}}
        private void accessory(Canvas c,float cx,float cy,float s){
            if(style==1){
                Path crown=new Path(); crown.moveTo(cx-s*.53f,cy-s*.64f); crown.lineTo(cx-s*.48f,cy-s*.93f); crown.lineTo(cx-s*.32f,cy-s*.77f); crown.lineTo(cx-s*.20f,cy-s*1.0f); crown.lineTo(cx-s*.05f,cy-s*.75f); crown.lineTo(cx+s*.06f,cy-s*.93f); crown.lineTo(cx,cy-s*.63f); crown.close(); p.setColor(0xFFFFD166);c.drawPath(crown,p);
                p.setColor(0xAAFFB4D8);c.drawOval(new RectF(cx-s*.72f,cy+s*.20f,cx+s*.72f,cy+s*.52f),p);
            }else if(style==2){
                p.setColor(0xFFB58A54);c.drawOval(new RectF(cx-s*.65f,cy-s*.69f,cx+s*.08f,cy-s*.52f),p);round(c,cx-s*.53f,cy-s*.92f,cx-s*.05f,cy-s*.58f,s*.09f,p);
                p.setColor(0xFF76543A);round(c,cx+s*.28f,cy-s*.03f,cx+s*.62f,cy+s*.40f,s*.07f,p);
            }else if(style==3){
                Path cap=new Path();cap.moveTo(cx-s*.57f,cy-s*.59f);cap.quadTo(cx-s*.23f,cy-s*1.05f,cx+s*.32f,cy-s*.85f);cap.quadTo(cx,cy-s*.64f,cx-s*.05f,cy-s*.53f);cap.close();p.setColor(0xFF7C8CFF);c.drawPath(cap,p);p.setColor(0xFFFFE37A);c.drawCircle(cx+s*.32f,cy-s*.85f,s*.065f,p);
                p.setColor(0x996D7CE8);round(c,cx-s*.70f,cy+s*.02f,cx+s*.64f,cy+s*.43f,s*.13f,p);
            }else if(style==4){
                line.setColor(0xFF5D3A77);line.setStrokeWidth(s*.035f);c.drawCircle(cx-s*.39f,cy-s*.25f,s*.13f,line);c.drawCircle(cx-s*.10f,cy-s*.25f,s*.13f,line);c.drawLine(cx-s*.26f,cy-s*.25f,cx-s*.23f,cy-s*.25f,line);
                bow(c,cx-s*.02f,cy+s*.09f,s*.17f);
            }else{
                line.setColor(0xFFFFD166);line.setStrokeWidth(s*.035f);c.drawArc(new RectF(cx-s*.47f,cy-s*.03f,cx+s*.31f,cy+s*.28f),15,130,false,line);heart(c,cx-s*.04f,cy+s*.20f,s*.10f,0xFFFF6FAE);
            }
        }

        private void drawActions(Canvas c,float w,float h){
            float top=h*.755f,bottom=h*.865f; p.setColor(0xF7FFFFFF);round(c,w*.025f,top,w*.975f,bottom,28,p);
            String a,b; if(room==0){a=sleeping?"DESPERTAR":"DORMIR";b="ABRAZO";}else if(room==1){a="MANZANA";b="PASTELITO";}else if(room==2){a="BAÑAR";b="CEPILLAR";}else if(room==3){a="JUGAR";b="BUSCAR ★";}else{a="ESTILO";b="COLOR";}
            float gap=w*.025f,bw=(w*.90f-gap)/2f; leftAction.set(w*.05f,top+h*.02f,w*.05f+bw,bottom-h*.018f); rightAction.set(leftAction.right+gap,leftAction.top,leftAction.right+gap+bw,leftAction.bottom);
            action(c,leftAction,a,0xFFFF7AB8);action(c,rightAction,b,0xFF7C8CFF);
        }
        private void action(Canvas c,RectF r,String text,int color){p.setColor(0x22000000);round(c,r.left,r.top+5,r.right,r.bottom+5,24,p);p.setColor(color);round(c,r.left,r.top,r.right,r.bottom,24,p);p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);p.setFakeBoldText(true);p.setTextSize(getWidth()*.032f);c.drawText(text,r.centerX(),r.centerY()-(p.ascent()+p.descent())/2,p);p.setFakeBoldText(false);}

        private void drawNavigation(Canvas c,float w,float h){
            float top=h*.875f,item=w/5f;p.setColor(0xF45D3A77);round(c,0,top-12,w,h+20,32,p);
            for(int i=0;i<5;i++){
                RectF r=new RectF(i*item,top,(i+1)*item,h);navRects[i]=r;
                if(i==room){p.setColor(0x44FFFFFF);round(c,r.left+8,r.top+8,r.right-8,r.bottom-8,22,p);}
                p.setTextAlign(Paint.Align.CENTER);p.setFakeBoldText(i==room);p.setTextSize(w*.021f);p.setColor(i==room?0xFFFFE37A:Color.WHITE);c.drawText(NAV[i],r.centerX(),h*.953f,p);
                p.setTextSize(w*.018f);p.setFakeBoldText(false);p.setColor(Color.WHITE);c.drawText(ROOMS[i].toUpperCase(Locale.ROOT),r.centerX(),h*.982f,p);
            }
        }
        private void drawMessage(Canvas c,float w,float h){
            p.setTextSize(w*.034f);p.setFakeBoldText(true);float tw=p.measureText(message),bw=Math.min(w*.90f,tw+w*.10f),y=h*.70f;p.setColor(0xDD5D3A77);round(c,w/2-bw/2,y-h*.035f,w/2+bw/2,y+h*.035f,27,p);p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);c.drawText(message,w/2,y-(p.ascent()+p.descent())/2,p);p.setFakeBoldText(false);
        }

        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=e.getX(),y=e.getY();
            for(int i=0;i<5;i++)if(navRects[i]!=null&&navRects[i].contains(x,y)){room=i;sleeping=false;say("Vamos al "+ROOMS[i].toLowerCase(Locale.ROOT));burst(x,y,10);save();performClick();return true;}
            if(leftAction.contains(x,y)){primary();save();performClick();return true;}
            if(rightAction.contains(x,y)){secondary();save();performClick();return true;}
            burst(x,y,5);return true;
        }
        @Override public boolean performClick(){super.performClick();return true;}

        private void primary(){float x=getWidth()/2f,y=getHeight()*.48f;
            if(room==0){sleeping=!sleeping;if(sleeping){sleepUntil=SystemClock.elapsedRealtime()+8000;say("Shhh… A-tatis está descansando");}else say("¡A-tatis despertó!");}
            else if(room==1){food=cap(food+20);happy=cap(happy+4);say("¡Ñam! Manzana crujiente");burst(x,y,14);}
            else if(room==2){clean=cap(clean+35);happy=cap(happy+5);say("¡Burbujas mágicas!");burst(x,y,18);}
            else if(room==3){happy=cap(happy+25);energy=cap(energy-6);say("¡A-tatis jugó en el jardín!");burst(x,y,18);}
            else{style=(style+1)%5;happy=cap(happy+4);say("Estilo: "+STYLES[style]);burst(x,y,22);}
        }
        private void secondary(){float x=getWidth()/2f,y=getHeight()*.48f;
            if(room==0){happy=cap(happy+18);energy=cap(energy+4);say("¡Abrazo gigante para A-tatis!");burst(x,y,20);}
            else if(room==1){food=cap(food+29);happy=cap(happy+7);clean=cap(clean-2);say("¡Pastelito de estrellas!");burst(x,y,18);}
            else if(room==2){clean=cap(clean+18);happy=cap(happy+3);say("¡Pelito suave y brillante!");burst(x,y,14);}
            else if(room==3){int n=1+rnd.nextInt(3);stars+=n;happy=cap(happy+10);energy=cap(energy-3);say("¡Encontraste "+n+(n==1?" estrella!":" estrellas!"));burst(x,y,25);}
            else{fur=(fur+1)%5;happy=cap(happy+4);say("Color mágico: "+COLORS[fur]);burst(x,y,22);}
        }
        private void say(String s){message=s;messageUntil=SystemClock.elapsedRealtime()+2600;}

        private void burst(float x,float y,int n){for(int i=0;i<n;i++){float a=(float)(rnd.nextFloat()*Math.PI*2),v=60+rnd.nextFloat()*170;particles.add(new Particle(x,y,(float)Math.cos(a)*v,(float)Math.sin(a)*v-50,.7f+rnd.nextFloat()*.9f,MAGIC[rnd.nextInt(5)],5+rnd.nextFloat()*9));}}
        private void updateParticles(float dt){for(int i=particles.size()-1;i>=0;i--){Particle q=particles.get(i);q.life-=dt;q.x+=q.vx*dt;q.y+=q.vy*dt;q.vy+=180*dt;if(q.life<=0)particles.remove(i);}}
        private void drawParticles(Canvas c){for(Particle q:particles){p.setColor(Color.argb((int)(255*Math.min(1,q.life)),Color.red(q.color),Color.green(q.color),Color.blue(q.color)));star(c,q.x,q.y,q.size,p);}}

        private void cloud(Canvas c,float x,float y,float s){p.setColor(0xEEFFFFFF);c.drawCircle(x-s*.4f,y,s*.35f,p);c.drawCircle(x,y-s*.15f,s*.48f,p);c.drawCircle(x+s*.45f,y,s*.32f,p);round(c,x-s*.7f,y,x+s*.7f,y+s*.32f,s*.14f,p);}
        private void flower(Canvas c,float x,float y,float r,int color){line.setColor(0xFF3E8B57);line.setStrokeWidth(r*.25f);c.drawLine(x,y,x,y+r*2.8f,line);p.setColor(color);for(int i=0;i<6;i++){double a=i*Math.PI/3;c.drawCircle(x+(float)Math.cos(a)*r*.75f,y+(float)Math.sin(a)*r*.75f,r*.55f,p);}p.setColor(0xFFFFD166);c.drawCircle(x,y,r*.48f,p);}
        private void hanger(Canvas c,float x,float y,float s,int color){line.setColor(color);line.setStrokeWidth(s*.07f);c.drawArc(new RectF(x-s*.12f,y-s*.18f,x+s*.12f,y+s*.08f),180,210,false,line);c.drawLine(x,y+s*.05f,x-s*.75f,y+s*.55f,line);c.drawLine(x,y+s*.05f,x+s*.75f,y+s*.55f,line);c.drawLine(x-s*.75f,y+s*.55f,x+s*.75f,y+s*.55f,line);}
        private void bow(Canvas c,float x,float y,float s){p.setColor(0xFFFF5F96);c.drawOval(new RectF(x-s,y-s*.5f,x,y+s*.5f),p);c.drawOval(new RectF(x,y-s*.5f,x+s,y+s*.5f),p);p.setColor(0xFFFFD166);c.drawCircle(x,y,s*.25f,p);}
        private void heart(Canvas c,float x,float y,float s,int color){Path q=new Path();q.moveTo(x,y+s);q.cubicTo(x-s*1.3f,y,x-s*.5f,y-s,x,y-s*.25f);q.cubicTo(x+s*.5f,y-s,x+s*1.3f,y,x,y+s);p.setColor(color);c.drawPath(q,p);}
        private void star(Canvas c,float x,float y,float r,Paint paint){Path q=new Path();for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5;float rr=i%2==0?r:r*.42f,xx=x+(float)Math.cos(a)*rr,yy=y+(float)Math.sin(a)*rr;if(i==0)q.moveTo(xx,yy);else q.lineTo(xx,yy);}q.close();c.drawPath(q,paint);}
        private void round(Canvas c,float l,float t,float r,float b,float rad,Paint paint){c.drawRoundRect(new RectF(l,t,r,b),rad,rad,paint);}
        private static float cap(float v){return Math.max(0,Math.min(100,v));}
        private static int limit(int v,int a,int b){return Math.max(a,Math.min(b,v));}
        private static int blend(int a,int b,float k){return Color.rgb((int)(Color.red(a)+(Color.red(b)-Color.red(a))*k),(int)(Color.green(a)+(Color.green(b)-Color.green(a))*k),(int)(Color.blue(a)+(Color.blue(b)-Color.blue(a))*k));}

        static final class Particle {float x,y,vx,vy,life;final int color;final float size;Particle(float x,float y,float vx,float vy,float life,int color,float size){this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.life=life;this.color=color;this.size=size;}}
    }
}
